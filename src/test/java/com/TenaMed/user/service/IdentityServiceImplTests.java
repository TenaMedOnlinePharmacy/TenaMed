package com.TenaMed.user.service;

import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.dto.LoginResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.entity.Account;
import com.TenaMed.user.entity.Role;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.exception.EmailAlreadyRegisteredException;
import com.TenaMed.user.exception.InvalidCredentialsException;
import com.TenaMed.user.mapper.IdentityMapper;
import com.TenaMed.user.repository.AccountRepository;
import com.TenaMed.user.repository.RoleRepository;
import com.TenaMed.user.repository.UserRepository;
import com.TenaMed.user.repository.UserRoleRepository;
import com.TenaMed.user.service.impl.IdentityServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityServiceImplTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IdentityMapper identityMapper;

    @InjectMocks
    private IdentityServiceImpl identityService;

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        RegisterRequestDto request = buildRegisterRequest();

        when(accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@tenamed.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> identityService.register(request));
    }

    @Test
    void shouldRegisterWhenInputIsValid() {
        RegisterRequestDto request = buildRegisterRequest();

        Role role = new Role();
        role.setName("PATIENT");

        Account savedAccount = new Account();
        User savedUser = new User();
        RegisterResponseDto mappedResponse = RegisterResponseDto.builder()
                .accountId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .email("test@tenamed.com")
                .accountStatus("ACTIVE")
                .build();

        when(accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("test@tenamed.com")).thenReturn(false);
        when(userRepository.existsByPhone("+251900000000")).thenReturn(false);
        when(roleRepository.findAllActiveByNormalizedNameIn(Set.of("patient"))).thenReturn(List.of(role));
        when(passwordEncoder.encode("StrongPass123")).thenReturn("hashed");
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(identityMapper.toRegisterResponse(savedUser)).thenReturn(mappedResponse);

        RegisterResponseDto actual = identityService.register(request);

        assertNotNull(actual);
        assertEquals("test@tenamed.com", actual.getEmail());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals("hashed", accountCaptor.getValue().getPasswordHash());
    }

    @Test
    void shouldIncreaseFailedAttemptsWhenPasswordIsInvalid() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@tenamed.com");
        request.setPassword("wrong-password");

        Account account = new Account();
        account.setEmail("test@tenamed.com");
        account.setPasswordHash("stored-hash");
        account.setFailedLoginAttempts(1);
        account.setAccountStatus("ACTIVE");

        when(accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("test@tenamed.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> identityService.login(request));
        assertEquals(2, account.getFailedLoginAttempts());
        verify(accountRepository).save(account);
    }

    @Test
    void shouldLoginAndResetAttemptsWhenPasswordMatches() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@tenamed.com");
        request.setPassword("StrongPass123");

        Account account = new Account();
        account.setEmail("test@tenamed.com");
        account.setPasswordHash("stored-hash");
        account.setFailedLoginAttempts(4);
        account.setAccountStatus("ACTIVE");

        User user = new User();
        user.setAccount(account);

        LoginResponseDto mappedResponse = LoginResponseDto.builder()
                .email("test@tenamed.com")
                .accountStatus("ACTIVE")
                .roles(List.of("PATIENT"))
                .build();

        when(accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("test@tenamed.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("StrongPass123", "stored-hash")).thenReturn(true);
        when(userRepository.findByAccount_Id(isNull()))
                .thenReturn(Optional.of(user));
        when(identityMapper.toLoginResponse(user)).thenReturn(mappedResponse);

        LoginResponseDto actual = identityService.login(request);

        assertEquals("test@tenamed.com", actual.getEmail());
        assertEquals(0, account.getFailedLoginAttempts());
        verify(accountRepository).save(account);
    }

    private RegisterRequestDto buildRegisterRequest() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("test@tenamed.com");
        request.setPassword("StrongPass123");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setPhone("+251900000000");
        request.setAddress(Map.of("city", "Addis Ababa"));
        request.setRoleNames(Set.of("PATIENT"));
        return request;
    }
}
