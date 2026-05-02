package com.management.event.security.config;

import com.management.event.entity.AppRole;
import com.management.event.entity.CalendarEvent;
import com.management.event.entity.CalendarEventStatus;
import com.management.event.entity.Letter;
import com.management.event.entity.LetterStatus;
import com.management.event.entity.Place;
import com.management.event.entity.Role;
import com.management.event.entity.User;
import com.management.event.repository.CalendarEventRepository;
import com.management.event.repository.LetterRepository;
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
import java.util.Optional;

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
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(
//                                "/api/auth/signin",
//                                "/api/auth/register",
//                                "/api/calendar/**",
//                                "/api/public/**",
//                                "/images/**",
//                                "/uploads/**"
//                        )
//                        .permitAll()
//                        .anyRequest()
//                        .authenticated()
//                )
        .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
        )
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
                                       PlaceRepository placeRepository,
                                       LetterRepository letterRepository,
                                       CalendarEventRepository calendarEventRepository) {
        return args -> {
            for (AppRole appRole : AppRole.values()) {
                if (roleRepository.findByRoleName(appRole).isEmpty()) {
                    roleRepository.save(new Role(appRole));
                }
            }

            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();
            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();
            Role lecturerRole = roleRepository.findByRoleName(AppRole.ROLE_LECTURER).orElseThrow();
            Role deanRole = roleRepository.findByRoleName(AppRole.ROLE_DEAN).orElseThrow();

            if (userRepository.findByEmail("john@example.com").isEmpty()) {
                User user1 = new User();
                user1.setUserName("john");
                user1.setEmail("john@example.com");
                user1.setRegNumber("TG/2023/001");
                user1.setPassword(passwordEncoder.encode("1234"));
                user1.setRoles(new HashSet<>(List.of(userRole)));
                userRepository.save(user1);
            }

            if (userRepository.findByEmail("admin@example.com").isEmpty()) {
                User user2 = new User();
                user2.setUserName("admin");
                user2.setEmail("admin@example.com");
                user2.setRegNumber("ADMIN/001");
                user2.setPassword(passwordEncoder.encode("1234"));
                user2.setRoles(new HashSet<>(List.of(adminRole)));
                userRepository.save(user2);
            }

            if (userRepository.findByRegNumber("LC2001").isEmpty()) {
                User lecturer = new User();
                lecturer.setUserName("lecturer");
                lecturer.setEmail("lecturer@example.com");
                lecturer.setRegNumber("LC2001");
                lecturer.setPassword(passwordEncoder.encode("1234"));
                lecturer.setRoles(new HashSet<>(List.of(lecturerRole)));
                userRepository.save(lecturer);
            }

            if (userRepository.findByRegNumber("DID100").isEmpty()) {
                User dean = new User();
                dean.setUserName("dean");
                dean.setEmail("dean@example.com");
                dean.setRegNumber("DID100");
                dean.setPassword(passwordEncoder.encode("deanpass"));
                dean.setRoles(new HashSet<>(List.of(deanRole)));
                userRepository.save(dean);
            }

            if (userRepository.findByRegNumber("HODICT").isEmpty()) {
                User hodict = new User();
                hodict.setUserName("hodict");
                hodict.setEmail("hodict@example.com");
                hodict.setRegNumber("HODICT");
                hodict.setPassword(passwordEncoder.encode("1234"));
                hodict.setRoles(new HashSet<>(List.of(userRole)));
                userRepository.save(hodict);
            }

            if (userRepository.findByRegNumber("ET-TO1").isEmpty()) {
                User etTo = new User();
                etTo.setUserName("et-to");
                etTo.setEmail("et.to@example.com");
                etTo.setRegNumber("ET-TO1");
                etTo.setPassword(passwordEncoder.encode("1234"));
                etTo.setRoles(new HashSet<>(List.of(userRole)));
                userRepository.save(etTo);
            }

            if (userRepository.findByRegNumber("ICT-TO2").isEmpty()) {
                User ictTo = new User();
                ictTo.setUserName("ict-to");
                ictTo.setEmail("ict.to@example.com");
                ictTo.setRegNumber("ICT-TO2");
                ictTo.setPassword(passwordEncoder.encode("1234"));
                ictTo.setRoles(new HashSet<>(List.of(userRole)));
                userRepository.save(ictTo);
            }

            if (userRepository.findByRegNumber("BST-TO2").isEmpty()) {
                User bstTo = new User();
                bstTo.setUserName("bst-to");
                bstTo.setEmail("bst.to@example.com");
                bstTo.setRegNumber("BST-TO2");
                bstTo.setPassword(passwordEncoder.encode("1234"));
                bstTo.setRoles(new HashSet<>(List.of(userRole)));
                userRepository.save(bstTo);
            }

            if (userRepository.findByRegNumber("D-OFFICE-01").isEmpty()) {
                User dOffice = new User();
                dOffice.setUserName("d-office");
                dOffice.setEmail("d.office@example.com");
                dOffice.setRegNumber("D-OFFICE-01");
                dOffice.setPassword(passwordEncoder.encode("1234"));
                dOffice.setRoles(new HashSet<>(List.of(userRole)));
                userRepository.save(dOffice);
            }

            if (placeRepository.count() == 0) {
                User etTo    = userRepository.findByRegNumber("ET-TO1").orElseThrow();
                User ictTo   = userRepository.findByRegNumber("ICT-TO2").orElseThrow();
                User bstTo   = userRepository.findByRegNumber("BST-TO2").orElseThrow();
                User dOffice = userRepository.findByRegNumber("D-OFFICE-01").orElseThrow();

                placeRepository.saveAll(List.of(
                        new Place(null, "Auditorium", "All",  450,  dOffice),
                        new Place(null, "Lab11",      "ICT",  80,   ictTo),
                        new Place(null, "Lab12",      "ICT",  110,  ictTo),
                        new Place(null, "NBLLT",      "ET",   200,  etTo),
                        new Place(null, "LH210",      "ET",   500,  etTo),
                        new Place(null, "BST12",      "BST",  120,  bstTo),
                        new Place(null, "Ground",     "All",  null, dOffice),
                        new Place(null, "King Road",  "All",  null, dOffice)
                ));
            }

            // Seed 3 calendar events for May 2026 (idempotent).
            User admin = userRepository.findByRegNumber("ADMIN/001").orElseThrow();

            seedApprovedCalendarEvent(
                    "May 2026 Career Guidance",
                    "Career guidance session for undergraduates.",
                    java.time.LocalDate.of(2026, 5, 5),
                    java.time.LocalTime.of(9, 0),
                    java.time.LocalTime.of(11, 0),
                    "Auditorium",
                    admin,
                    placeRepository,
                    letterRepository,
                    calendarEventRepository
            );

            seedApprovedCalendarEvent(
                    "May 2026 Coding Workshop",
                    "Hands-on coding workshop.",
                    java.time.LocalDate.of(2026, 5, 12),
                    java.time.LocalTime.of(13, 30),
                    java.time.LocalTime.of(16, 30),
                    "Lab12",
                    admin,
                    placeRepository,
                    letterRepository,
                    calendarEventRepository
            );

            seedApprovedCalendarEvent(
                    "May 2026 Department Seminar",
                    "Monthly department seminar.",
                    java.time.LocalDate.of(2026, 5, 28),
                    java.time.LocalTime.of(16, 0),
                    java.time.LocalTime.of(18, 0),
                    "NBLLT",
                    admin,
                    placeRepository,
                    letterRepository,
                    calendarEventRepository
            );

            System.out.println("Default roles, users, and places initialized (if missing).");
        };
    }

    private static void seedApprovedCalendarEvent(
            String title,
            String description,
            java.time.LocalDate date,
            java.time.LocalTime time,
            java.time.LocalTime endTime,
            String placeName,
            User owner,
            PlaceRepository placeRepository,
            LetterRepository letterRepository,
            CalendarEventRepository calendarEventRepository
    ) {
        Optional<Letter> existingLetter = letterRepository.findByTitleAndEventDateAndEventTimeAndEventPlace(
                title, date, time, placeName
        );

        Letter letter = existingLetter.orElseGet(() -> {
            Letter l = new Letter();
            l.setUser(owner);
            l.setTitle(title);
            l.setDescription(description);
            l.setEventDate(date);
            l.setEventTime(time);
            l.setEventEndTime(endTime);
            l.setEventPlace(placeName);
            l.setPdfPath("uploads/letters/seed-" + title.toLowerCase().replace(' ', '-') + ".pdf");
            l.setGlobalStatus(LetterStatus.APPROVED);
            l.setRejectionReason(null);
            return letterRepository.save(l);
        });

        // Ensure legacy letter rows get end time for correct conflict detection.
        if (letter.getEventEndTime() == null) {
            letter.setEventEndTime(endTime);
            letterRepository.save(letter);
        }

        CalendarEvent existingEvent = calendarEventRepository.findByLetterId(letter.getId()).orElse(null);
        if (existingEvent == null) {
            CalendarEvent event = new CalendarEvent();
            event.setLetter(letter);
            event.setTitle(title);
            event.setDescription(description);
            event.setEventDate(date);
            event.setEventTime(time);
            event.setEndTime(endTime);
            event.setPlaceName(placeName);
            event.setPlace(placeRepository.findByPlaceName(placeName).orElse(null));
            event.setStatus(CalendarEventStatus.APPROVED);
            calendarEventRepository.save(event);
        } else {
            // Keep seed data consistent if the letter exists but event fields changed.
            existingEvent.setTitle(title);
            existingEvent.setDescription(description);
            existingEvent.setEventDate(date);
            existingEvent.setEventTime(time);
            existingEvent.setEndTime(endTime);
            existingEvent.setPlaceName(placeName);
            existingEvent.setPlace(placeRepository.findByPlaceName(placeName).orElse(null));
            existingEvent.setStatus(CalendarEventStatus.APPROVED);
            calendarEventRepository.save(existingEvent);
        }
    }
}
