package org.example.controller;

import jakarta.validation.Valid;
import org.example.config.JwtUtil;
import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.dto.RegisterRequest;
import org.example.model.Organization;
import org.example.model.User;
import org.example.repository.OrganizationRepository;
import org.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return toResponse(user);
    }

    @PostMapping("/register")
    @Transactional
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (organizationRepository.existsBySlug(request.getOrganizationSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Organization slug already exists");
        }

        Organization organization = new Organization();
        organization.setName(request.getOrganizationName());
        organization.setSlug(request.getOrganizationSlug());
        organization.setSubscriptionTier(Organization.SubscriptionTier.FREE);
        Organization savedOrganization = organizationRepository.save(organization);

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(User.Role.ADMIN); // the person who creates an organization administers it
        user.setOrganization(savedOrganization);

        return toResponse(userRepository.save(user));
    }

    private AuthResponse toResponse(User user) {
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
}
