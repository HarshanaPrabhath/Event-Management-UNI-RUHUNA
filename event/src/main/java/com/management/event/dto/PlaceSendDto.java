package com.management.event.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaceSendDto {

    private Long placeId;

    private String placeName;

    private String department;

    private Integer capacity;
}
