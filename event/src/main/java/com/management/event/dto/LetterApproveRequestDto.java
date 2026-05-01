package com.management.event.dto;

import lombok.Data;

@Data
public class LetterApproveRequestDto {
    // Optional note/reason from the approver. Saved into workflow step remarks.
    // If this is the final approval, it is also saved into Letter.approvalNote.
    private String remarks;
}

