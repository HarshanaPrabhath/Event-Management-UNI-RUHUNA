package com.management.event.security.config;

import com.management.event.entity.AppRole;
import com.management.event.entity.Place;
import com.management.event.entity.Role;
import com.management.event.entity.User;
import com.management.event.repository.PlaceRepository;
import com.management.event.repository.RoleRepository;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.HashSet;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/signin",
                                "/api/auth/register",
                                "/api/calendar/**",
                                "/api/clubs/**",
                                "/api/public/**",
                                "/images/**",
                                "/uploads/**"
                        )
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
//        .authorizeHttpRequests(auth -> auth
//                .anyRequest().permitAll()
//        )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository,
                                       RoleRepository roleRepository,
                                       PasswordEncoder passwordEncoder,
                                       PlaceRepository placeRepository) {
        return args -> {
            for (AppRole appRole : AppRole.values()) {
                if (roleRepository.findByRoleName(appRole).isEmpty()) {
                    roleRepository.save(new Role(appRole));
                }
            }

            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();
            Role lecturerRole = roleRepository.findByRoleName(AppRole.ROLE_LECTURER).orElseThrow();
            Role deanRole = roleRepository.findByRoleName(AppRole.ROLE_DEAN).orElseThrow();
            Role toRole = roleRepository.findByRoleName(AppRole.ROLE_TO).orElseThrow();

            if (userRepository.findByRegNumber("ADMIN/001").isEmpty()) {
                User admin = new User();
                admin.setUserName("admin");
                admin.setEmail("admin@example.com");
                admin.setRegNumber("ADMIN/001");
                admin.setPassword(passwordEncoder.encode("1234"));
                admin.setRoles(new HashSet<>(List.of(adminRole)));
                userRepository.save(admin);
            }

            // --- Real department roster (source: PANEL.txt). Shared password "1234" for everyone,
            // matching PANEL.txt - change on first login in a real deployment.
            //
            // The HOD is also a lecturer, so ROLE_LECTURER covers them too (there is no separate
            // ROLE_HOD in the system). Any lecturer here - HOD included - can additionally be
            // picked as a club's senior treasurer from the admin's "Create Club" screen;
            // ClubAdminService.assignSeniorTreasurer() adds ROLE_SENIOR_TRESURER to them
            // automatically at that point, so nothing extra needs to be seeded for that here.
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Dr. H.M. Chandana Pushpakumara", "chandanap@ictec.ruh.ac.lk", "ICT-HOD-Chanadana");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Mr. P.H.P.N Laksiri", "phpnlaksiri@ictec.ruh.ac.lk", "LC-Laksiri");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Rumeshika W. arachi", "rumeshika@ictec.ruh.ac.lk", "LC-Rumeshika");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Buddika Gayashani", "buddika@ictec.ruh.ac.lk", "LC-Buddika");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Malsha Prabuddhi", "malsha@ictec.ruh.ac.lk", "LC-Malsha");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Chanduni Gamage", "chanduni@ictec.ruh.ac.lk", "LC-Chanduni");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. E.H.M.P.M. Wijerathna", "piyumi@ictec.ruh.ac.lk", "LC-Wijerathna");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. R.D.N. Shakya", "shakya@ictec.ruh.ac.lk", "LC-Shakya");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Akila Brahmana", "akila@ictec.ruh.ac.lk", "LC-Akila");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Mr. A.W.A.T. Dilhan", "dilhan@ictec.ruh.ac.lk", "LC-Dilhan");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Mr. Shashitha Lakal", "shashithal@ictec.ruh.ac.lk", "LC-Shashitha");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Dinoo Gunasekera", "dinoog@ictec.ruh.ac.lk", "LC-Dinoo");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Sandaruwani Pathirage", "sandaruwani@fot.ruh.ac.lk", "LC-Sandaruwani");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Dharani Gunasekara", "dharani@fot.ruh.ac.lk", "LC-Dharani");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Ms. Thirumugam Priyanka", "priyanka@fot.ruh.ac.lk", "LC-Priyanka");
            seedLecturer(userRepository, lecturerRole, passwordEncoder,
                    "Mr. R.M. Nayanajith Rathnayake", "nayanajith@fot.ruh.ac.lk", "LC-Nayanajith");

            if (userRepository.findByRegNumber("DEAN-Jayasinghe").isEmpty()) {
                User dean = new User();
                dean.setUserName("Prof. P.K.S.C Jayasinghe");
                dean.setEmail("subash@ictec.ruh.ac.lk");
                dean.setRegNumber("DEAN-Jayasinghe");
                dean.setPassword(passwordEncoder.encode("1234"));
                dean.setRoles(new HashSet<>(List.of(deanRole)));
                userRepository.save(dean);
            }

            // Real ICT-department technical officers - responsible persons for the ICT places
            // (Lab11 / Lab12) below. PANEL.txt doesn't give these two a reg number (only
            // name/email/title), so "TO-<surname>" was made up to match the existing TO-* style.
            if (userRepository.findByRegNumber("TO-Kumara").isEmpty()) {
                User kumara = new User();
                kumara.setUserName("Mr. W.P.C. D. Kumara");
                kumara.setEmail("kumara@gmail.com");
                kumara.setRegNumber("TO-Kumara");
                kumara.setPassword(passwordEncoder.encode("1234"));
                kumara.setRoles(new HashSet<>(List.of(toRole)));
                userRepository.save(kumara);
            }
            if (userRepository.findByRegNumber("TO-Ranasinghe").isEmpty()) {
                User ranasinghe = new User();
                ranasinghe.setUserName("Ms. T.P.Ranasinghe");
                ranasinghe.setEmail("ranasinghe@gmail.com");
                ranasinghe.setRegNumber("TO-Ranasinghe");
                ranasinghe.setPassword(passwordEncoder.encode("1234"));
                ranasinghe.setRoles(new HashSet<>(List.of(toRole)));
                userRepository.save(ranasinghe);
            }

            // No one is named in PANEL.txt for ET / BST / general-office, so those places are seeded
            // with no responsible person (nullable) instead of a placeholder account. Assign a real
            // person via /api/admin/places once known - AdminPlaceService grants ROLE_TO at that point.
            if (placeRepository.count() == 0) {
                User ictToLab11 = userRepository.findByRegNumber("TO-Kumara").orElseThrow();
                User ictToLab12 = userRepository.findByRegNumber("TO-Ranasinghe").orElseThrow();

                placeRepository.saveAll(List.of(
                        new Place(null, "Auditorium", "All",  450,  null, null),
                        new Place(null, "Lab11",      "ICT",  80,   null, ictToLab11),
                        new Place(null, "Lab12",      "ICT",  110,  null, ictToLab12),
                        new Place(null, "NBLLT",      "ET",   200,  null, null),
                        new Place(null, "LH210",      "ET",   500,  null, null),
                        new Place(null, "BST12",      "BST",  120,  null, null),
                        new Place(null, "Ground",     "All",  null, null, null),
                        new Place(null, "King Road",  "All",  null, null, null)
                ));
            }

            System.out.println("Admin, department roster (PANEL.txt), and places initialized (if missing).");
        };
    }

    private static void seedLecturer(UserRepository userRepository, Role lecturerRole, PasswordEncoder passwordEncoder,
                                      String fullName, String email, String regNumber) {
        if (userRepository.findByRegNumber(regNumber).isEmpty()) {
            User lecturer = new User();
            lecturer.setUserName(fullName);
            lecturer.setEmail(email);
            lecturer.setRegNumber(regNumber);
            lecturer.setPassword(passwordEncoder.encode("1234"));
            lecturer.setRoles(new HashSet<>(List.of(lecturerRole)));
            userRepository.save(lecturer);
        }
    }
}
