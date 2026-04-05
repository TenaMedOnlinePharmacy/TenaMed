package com.TenaMed.user.security;

import com.TenaMed.user.entity.Account;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.repository.AccountRepository;
import com.TenaMed.user.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public CustomUserDetailsService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedEmail = username.trim().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found for email"));
        return toPrincipal(account);
    }

    public AuthenticatedUserPrincipal loadPrincipalByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user.getAccount(), user);
    }

    private AuthenticatedUserPrincipal toPrincipal(Account account) {
        User user = userRepository.findByAccount_Id(account.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User profile not found for account"));
        return toPrincipal(account, user);
    }

    private AuthenticatedUserPrincipal toPrincipal(Account account, User user) {
        boolean enabled = ACTIVE_STATUS.equalsIgnoreCase(account.getAccountStatus());
        if (!enabled) {
            throw new DisabledException("Account is not active");
        }

        Collection<? extends GrantedAuthority> authorities = user.getUserRoles().stream()
                .map(link -> new SimpleGrantedAuthority("ROLE_" + link.getRole().getName().toUpperCase(Locale.ROOT)))
                .toList();

        return new AuthenticatedUserPrincipal(
                user.getId(),
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                authorities,
                true
        );
    }
}
