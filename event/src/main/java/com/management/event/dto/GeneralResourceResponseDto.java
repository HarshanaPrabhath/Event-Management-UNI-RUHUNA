package com.management.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralResourceResponseDto {
    private Long id;
    private String name;
    private Integer quantity;
    // Quantity minus whatever's already committed to other overlapping events, when a date/time
    // was given to the listing endpoint; otherwise equal to quantity.
    private Integer remainingQuantity;
    // How much of this resource other still-pending letters (not yet approved for it) have also
    // asked for on an overlapping slot - a heads-up, not a block. 0/null when no date/time was given.
    private Integer pendingElsewhere;
    private String responsiblePersonRegNumber;
    private String responsiblePersonName;
}
