package com.TenaMed.user.service.impl;

import com.TenaMed.antidoping.entity.AthleteProfile;
import com.TenaMed.antidoping.repository.AthleteProfileRepository;
import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.user.dto.AdminPharmacistRequestDto;
import com.TenaMed.user.dto.AdminPharmacistResponseDto;
import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.dto.LoginResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.entity.Account;
import com.TenaMed.user.entity.Role;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.entity.UserRole;
import com.TenaMed.user.dto.UserDetailsResponseDto;
import com.TenaMed.user.dto.UserRolesResponseDto;
import com.TenaMed.user.dto.AccountInfoResponseDto;
import com.TenaMed.user.dto.UpdateAccountRequestDto;
import com.TenaMed.user.exception.AccountNotActiveException;
import com.TenaMed.user.exception.EmailAlreadyRegisteredException;
import com.TenaMed.user.exception.InvalidCredentialsException;
import com.TenaMed.user.exception.PhoneAlreadyUsedException;
import com.TenaMed.user.exception.RoleNotFoundException;
import com.TenaMed.user.exception.RoleAlreadyAssignedException;
import com.TenaMed.user.exception.RoleNotAssignedException;
import com.TenaMed.user.exception.UserNotFoundException;
import com.TenaMed.user.mapper.IdentityMapper;
import com.TenaMed.user.repository.AccountRepository;
import com.TenaMed.user.repository.RoleRepository;
import com.TenaMed.user.repository.UserRepository;
import com.TenaMed.user.repository.UserRoleRepository;
import com.TenaMed.user.service.IdentityService;
import com.TenaMed.events.DomainEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IdentityServiceImpl implements IdentityService {

    private static final String ACCOUNT_STATUS_ACTIVE = "ACTIVE";
    private static final String ADMIN_PHARMACIST_ROLE = "ADMIN_PHARMACIST";
    private static final String DEFAULT_ROLES_FALLBACK = "ADMIN,PATIENT,DOCTOR,PHARMACIST,PHARMACYOWNER,HOSPITAL_OWNER,ADMIN_PHARMACIST";
    private static final String TEMP_PASSWORD_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper identityMapper;
    private final DomainEventService domainEventService;
    private final AthleteProfileRepository athleteProfileRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${user.default-roles:" + DEFAULT_ROLES_FALLBACK + "}")
    private String defaultRolesCsv;


    @Override
    public RegisterResponseDto register(RegisterRequestDto requestDto) {
        String normalizedEmail = normalizeEmail(requestDto.getEmail());
        if (accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        String normalizedPhone = normalizeNullable(requestDto.getPhone());
        if (normalizedPhone != null && userRepository.existsByPhone(normalizedPhone)) {
            throw new PhoneAlreadyUsedException(normalizedPhone);
        }

        List<Role> roles = resolveRoles(requestDto.getRoleNames());

        Account account = new Account();
        account.setEmail(normalizedEmail);
        account.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));
        //account status will be inactive and must be activiated after email verification
        account.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
        account.setFailedLoginAttempts(0);
        Account savedAccount = accountRepository.save(account);

        User user = new User();
        user.setAccount(savedAccount);
        user.setFirstName(requestDto.getFirstName().trim());
        user.setLastName(requestDto.getLastName().trim());
        user.setPhone(normalizedPhone);
        user.setAddress(copyAddress(requestDto.getAddress()));

        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            user.getUserRoles().add(userRole);
        }

        User savedUser = userRepository.save(user);


        Map<String, Object> registerMetadata = new HashMap<>();
        registerMetadata.put("email", normalizedEmail);
        if (savedAccount.getId() != null) {
            registerMetadata.put("accountId", savedAccount.getId());
        }
        domainEventService.publish(
            "USER_REGISTERED",
            "USER",
            savedUser.getId(),
            "PLATFORM",
            null,
            registerMetadata
        );
        return identityMapper.toRegisterResponse(savedUser);
    }

    @Override
    public LoginResponseDto login(LoginRequestDto requestDto) {
        String normalizedEmail = normalizeEmail(requestDto.getEmail());
        Account account = accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!ACCOUNT_STATUS_ACTIVE.equalsIgnoreCase(normalizeNullable(account.getAccountStatus()))) {
            throw new AccountNotActiveException(account.getAccountStatus());
        }

        if (!passwordEncoder.matches(requestDto.getPassword(), account.getPasswordHash())) {
            int attempts = account.getFailedLoginAttempts() == null ? 0 : account.getFailedLoginAttempts();
            account.setFailedLoginAttempts(attempts + 1);
            accountRepository.save(account);
            domainEventService.publish(
                    "USER_LOGIN_FAILED",
                    "ACCOUNT",
                    account.getId(),
                    "USER",
                    null,
                    "PLATFORM",
                    null,
                    Map.of("failedLoginAttempts", account.getFailedLoginAttempts())
            );
            throw new InvalidCredentialsException();
        }

        account.setFailedLoginAttempts(0);
        account.setLastLogin(LocalDateTime.now());
        accountRepository.save(account);

        User user = userRepository.findByAccount_Id(account.getId())
                .orElseThrow(() -> new InvalidCredentialsException());

        domainEventService.publish(
            "USER_LOGIN_SUCCEEDED",
            "ACCOUNT",
            account.getId(),
            "USER",
            user.getId(),
            "PLATFORM",
            null,
            account.getLastLogin() == null
                    ? Map.of()
                    : Map.of("lastLogin", account.getLastLogin().toString())
        );

        return identityMapper.toLoginResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsResponseDto getUserDetails(UUID userId) {
        User user = fetchUser(userId);
        return identityMapper.toUserDetailsResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountInfoResponseDto getAccountInfo(UUID userId) {
        User user = fetchUser(userId);
        Account account = user.getAccount();
        return AccountInfoResponseDto.builder()
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(account.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .build();
    }

    @Override
    public AccountInfoResponseDto updateAccountInfo(UUID userId, UpdateAccountRequestDto requestDto) {
        User user = fetchUser(userId);
        user.setPhone(normalizeNullable(requestDto.getPhone()));
        user.setAddress(copyAddress(requestDto.getAddress()));
        User saved = userRepository.save(user);

        domainEventService.publish(
            "USER_ACCOUNT_UPDATED",
            "USER",
            userId,
            "PLATFORM",
            null,
            Map.of("phoneUpdated", user.getPhone() != null)
        );

        return getAccountInfo(userId);
    }

    @Override
    public UserRolesResponseDto assignRoleToUser(UUID userId, String roleName) {
        User user = fetchUser(userId);
        String normalizedRoleName = normalizeRoleName(roleName);

        Role role = findRoleIgnoreCase(normalizedRoleName)
                .orElseThrow(() -> new RoleNotFoundException(Set.of(normalizedRoleName)));

        if (userRoleRepository.existsByUser_IdAndRole_Id(userId, role.getId())) {
            throw new RoleAlreadyAssignedException(userId, role.getName());
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);

        domainEventService.publish(
            "USER_ROLE_ASSIGNED",
            "USER",
            userId,
            "PLATFORM",
            null,
            Map.of("roleName", role.getName())
        );

        return getUserRoles(userId);
    }

    @Override
    public UserRolesResponseDto removeRoleFromUser(UUID userId, String roleName) {
        fetchUser(userId);
        String normalizedRoleName = normalizeRoleName(roleName);

        Role role = findRoleIgnoreCase(normalizedRoleName)
                .orElseThrow(() -> new RoleNotFoundException(Set.of(normalizedRoleName)));

        if (!userRoleRepository.existsByUser_IdAndRole_Id(userId, role.getId())) {
            throw new RoleNotAssignedException(userId, role.getName());
        }

        userRoleRepository.deleteByUser_IdAndRole_Id(userId, role.getId());
        domainEventService.publish(
            "USER_ROLE_REMOVED",
            "USER",
            userId,
            "PLATFORM",
            null,
            Map.of("roleName", role.getName())
        );
        return getUserRoles(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserRolesResponseDto getUserRoles(UUID userId) {
        User user = fetchUser(userId);
        List<UserRole> roles = userRoleRepository.findByUserId(userId);
        user.setUserRoles(roles.stream().collect(Collectors.toSet()));
        return identityMapper.toUserRolesResponse(user);
    }

    @Override
    public List<String> populateRoles() {
        Set<String> existingRoleNames = roleRepository.findAll().stream()
                .map(role -> normalizeRoleName(role.getName()))
                .collect(Collectors.toSet());

        List<Role> rolesToCreate = configuredDefaultRoles().stream()
                .filter(roleName -> !existingRoleNames.contains(normalizeRoleName(roleName)))
                .map(roleName -> {
                    Role role = new Role();
                    role.setName(roleName);
                    return role;
                })
                .collect(Collectors.toList());

        if (!rolesToCreate.isEmpty()) {
            roleRepository.saveAll(rolesToCreate);
            domainEventService.publish(
                    "DEFAULT_ROLES_POPULATED",
                    "ROLE",
                    null,
                    "PLATFORM",
                    null,
                    Map.of("createdRoles", rolesToCreate.stream().map(Role::getName).toList())
            );
        }

        return rolesToCreate.stream()
                .map(Role::getName)
                .toList();
    }

    @Override
    public AdminPharmacistResponseDto createAdminPharmacist(AdminPharmacistRequestDto requestDto) {
        if (requestDto == null) {
            throw new BadRequestException("Admin pharmacist payload is required");
        }

        String normalizedEmail = normalizeEmail(requestDto.getEmail());
        if (accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        String normalizedPhone = normalizeNullable(requestDto.getPhone());
        if (normalizedPhone != null && userRepository.existsByPhone(normalizedPhone)) {
            throw new PhoneAlreadyUsedException(normalizedPhone);
        }

        populateRoles();

        Role role = findRoleIgnoreCase(normalizeRoleName(ADMIN_PHARMACIST_ROLE))
                .orElseThrow(() -> new RoleNotFoundException(Set.of(normalizeRoleName(ADMIN_PHARMACIST_ROLE))));

        String temporaryPassword = generateTemporaryPassword();

        Account account = new Account();
        account.setEmail(normalizedEmail);
        account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        account.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
        account.setFailedLoginAttempts(0);
        Account savedAccount = accountRepository.save(account);

        User user = new User();
        user.setAccount(savedAccount);
        user.setFirstName(requestDto.getFirstName().trim());
        user.setLastName(requestDto.getLastName().trim());
        user.setPhone(normalizedPhone);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);

        User savedUser = userRepository.save(user);

        domainEventService.publish(
                "ADMIN_PHARMACIST_CREATED",
                "USER",
                savedUser.getId(),
                "PLATFORM",
                null,
                Map.of("roleName", role.getName())
        );

        return AdminPharmacistResponseDto.builder()
                .id(savedUser.getId())
                .email(normalizedEmail)
                .password(temporaryPassword)
                .role(role.getName())
                .build();
    }

    private List<String> configuredDefaultRoles() {
        String source = normalizeNullable(defaultRolesCsv);
        if (source == null) {
            source = DEFAULT_ROLES_FALLBACK;
        }

        return Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private List<Role> resolveRoles(Set<String> roleNames) {
        Set<String> normalizedNames = roleNames.stream()
                .map(this::normalizeRoleName)
                .collect(Collectors.toCollection(HashSet::new));

        List<Role> roles = roleRepository.findAllActiveByNormalizedNameIn(normalizedNames);
        Map<String, Role> byName = roles.stream()
                .collect(Collectors.toMap(role -> normalizeRoleName(role.getName()), Function.identity()));

        Set<String> missingRoles = normalizedNames.stream()
                .filter(name -> !byName.containsKey(name))
                .collect(Collectors.toCollection(HashSet::new));

        if (!missingRoles.isEmpty()) {
            throw new RoleNotFoundException(missingRoles);
        }

        return roles;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRoleName(String roleName) {
        return roleName.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Map<String, Object> copyAddress(Map<String, Object> address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        return new HashMap<>(address);
    }

    private User fetchUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Optional<Role> findRoleIgnoreCase(String normalizedRoleName) {
        return roleRepository.findByName(normalizedRoleName)
            .or(() -> roleRepository.findAll().stream()
                .filter(role -> normalizeRoleName(role.getName()).equals(normalizedRoleName))
                .findFirst());
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int index = secureRandom.nextInt(TEMP_PASSWORD_CHARSET.length());
            builder.append(TEMP_PASSWORD_CHARSET.charAt(index));
        }
        return builder.toString();
    }
}
