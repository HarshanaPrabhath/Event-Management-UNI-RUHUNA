package com.management.event.security.services;

import com.management.event.entity.User;
import com.management.event.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String regNumber) throws UsernameNotFoundException {
        User user = userRepository.findByRegNumber(regNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with reg number: " + regNumber));

        return UserDetailsImpl.build(user);
    }
}