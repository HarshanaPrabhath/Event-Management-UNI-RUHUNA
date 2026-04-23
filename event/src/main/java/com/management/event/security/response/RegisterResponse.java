package com.management.event.security.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseCookie;

@Getter
@AllArgsConstructor
public class RegisterResponse{
    private final UserInfoResponse userInfo;
    private final ResponseCookie jwtCookie;

}


