package com.management.event.dto;

import lombok.Data;

@Data
public class LetterRejectRequestDto {

    // Optional. If provided, stored as Letter.rejectionReason and also in workflow step remarks.
    private String rejectionReason;

    // Optional. Alternative note; if rejectionReason is empty, this will be used as the rejectionReason.
    private String remarks;
}
