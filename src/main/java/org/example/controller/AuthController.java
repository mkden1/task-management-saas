package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.dto.RegisterRequest;
import org.example.config.JwtUtil;
import org.example.model.User;
import org.example.model.Organization;
import org.example.repository.UserRepository;
import org.example.repository.OrganizationRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, 
                         UserRepository userRepository, OrganizationRepository organizationRepository,
                         PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getOrganization().getId());

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getId(),
                user.getOrganization().getId(),
                user.getOrganization().getName(),
                user.getRole().name()
        );
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (organizationRepository.existsBySlug(registerRequest.getOrganizationSlug())) {
            throw new RuntimeException("Organization slug already exists");
        }

        // Create organization first
        Organization organization = new Organization();
        organization.setName(registerRequest.getOrganizationName());
        organization.setSlug(registerRequest.getOrganizationSlug());
        organization.setSubscriptionTier(Organization.SubscriptionTier.FREE);
        Organization savedOrganization = organizationRepository.save(organization);

        // Create user with organization
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setRole(User.Role.ADMIN); // First user is admin
        user.setOrganization(savedOrganization);

        User savedUser = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPasswordHash())
                .authorities("ROLE_" + savedUser.getRole().name())
                .build();

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getOrganization().getId());

        return new AuthResponse(
                token,
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getOrganization().getId(),
                savedUser.getOrganization().getName(),
                savedUser.getRole().name()
        );
    }
}
