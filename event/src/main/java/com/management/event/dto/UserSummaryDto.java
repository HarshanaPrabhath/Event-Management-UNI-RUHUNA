package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSummaryDto {
    private String regNumber;
    private String userName;
    private String email;
}
