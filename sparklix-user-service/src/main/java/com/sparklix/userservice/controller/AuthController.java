package com.sparklix.userservice.controller;

import com.sparklix.userservice.dto.AuthRequest;
import com.sparklix.userservice.dto.AuthResponse;
import com.sparklix.userservice.dto.MessageResponse;
import com.sparklix.userservice.dto.UserRegistrationRequest;
import com.sparklix.userservice.entity.Role;
import com.sparklix.userservice.entity.User;
import com.sparklix.userservice.repository.RoleRepository;
import com.sparklix.userservice.repository.UserRepository;
import com.sparklix.userservice.util.JwtUtil; // Ensure this import is present
// import org.springframework.security.core.userdetails.UserDetails; // Not directly needed from here anymore for response
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // Import Spring's UserDetails
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil; // Uncommented

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) { // jwtUtil parameter uncommented and type added
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil; // Uncommented
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        if (userRepository.existsByUsername(registrationRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        User user = new User();
        user.setName(registrationRequest.getName());
        user.setUsername(registrationRequest.getUsername());
        user.setEmail(registrationRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token using the Authentication object
        String jwt = jwtUtil.generateToken(authentication);

        // Get UserDetails from the authentication principal to extract username and roles for the response
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AuthResponse(jwt, userDetails.getUsername(), roles));
    }
    @GetMapping("/test/user")
    @PreAuthorize("hasRole('USER')") // We can add this later for more fine-grained control
    public ResponseEntity<String> testUserAccess() {
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	String currentPrincipalName = authentication.getName();
    	
    	return ResponseEntity.ok("Hello " + currentPrincipalName + "! You have USER access. Your roles are: " + authentication.getAuthorities());
    }
    
    @GetMapping("/test/admin")
    @PreAuthorize("hasRole('ADMIN')") // We can add this later
    public ResponseEntity<String> testAdminAccess() {
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	String currentPrincipalName = authentication.getName();
    	
    	return ResponseEntity.ok("Hello " + currentPrincipalName + "! You have ADMIN access (mock for now). Your roles are: " + authentication.getAuthorities());
    }
}