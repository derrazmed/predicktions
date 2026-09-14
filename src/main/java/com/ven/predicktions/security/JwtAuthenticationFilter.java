package com.ven.predicktions.security;

import com.ven.predicktions.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectProvider<UserRepository> userRepositoryProvider;

    @Autowired
    public JwtAuthenticationFilter(
            JwtService jwtService,
            ObjectProvider<UserRepository> userRepositoryProvider
    ) {
        this.jwtService = jwtService;
        this.userRepositoryProvider = userRepositoryProvider;
    }

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this(jwtService, new SingleUserRepositoryProvider(userRepository));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        if (jwtService.isValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UUID userId = jwtService.extractUserId(token);

            UserRepository userRepository = userRepositoryProvider.getIfAvailable();
            if (userRepository == null || userRepository.findById(userId)
                    .map(user -> user.isEnabled())
                    .orElse(true)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        AuthorityUtils.createAuthorityList(
                                                "ROLE_" + jwtService.extractRole(token).name()
                                        )
                                );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private record SingleUserRepositoryProvider(UserRepository repository)
            implements ObjectProvider<UserRepository> {
        @Override
        public UserRepository getObject(Object... args) {
            return repository;
        }

        @Override
        public UserRepository getIfAvailable() {
            return repository;
        }

        @Override
        public UserRepository getIfUnique() {
            return repository;
        }
    }
}