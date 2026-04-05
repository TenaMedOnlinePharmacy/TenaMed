package com.TenaMed.user.service.impl;

import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.dto.LoginResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.entity.Account;
import com.TenaMed.user.entity.Role;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.entity.UserRole;
import com.TenaMed.user.exception.AccountNotActiveException;
import com.TenaMed.user.exception.EmailAlreadyRegisteredException;
import com.TenaMed.user.exception.InvalidCredentialsException;
import com.TenaMed.user.exception.PhoneAlreadyUsedException;
import com.TenaMed.user.exception.RoleNotFoundException;
import com.TenaMed.user.mapper.IdentityMapper;
import com.TenaMed.user.repository.AccountRepository;
import com.TenaMed.user.repository.RoleRepository;
import com.TenaMed.user.repository.UserRepository;
import com.TenaMed.user.service.IdentityService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class IdentityServiceImpl implements IdentityService {

    private static final String ACCOUNT_STATUS_ACTIVE = "ACTIVE";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper identityMapper;

    public IdentityServiceImpl(AccountRepository accountRepository,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               IdentityMapper identityMapper) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.identityMapper = identityMapper;
    }

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
            throw new InvalidCredentialsException();
        }

        account.setFailedLoginAttempts(0);
        account.setLastLogin(LocalDateTime.now());
        accountRepository.save(account);

        User user = userRepository.findByAccount_Id(account.getId())
                .orElseThrow(() -> new InvalidCredentialsException());

        return identityMapper.toLoginResponse(user);
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
}
