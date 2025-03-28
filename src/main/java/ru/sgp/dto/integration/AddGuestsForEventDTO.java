package ru.sgp.dto.integration;

import lombok.Data;

import java.util.List;

@Data
public class AddGuestsForEventDTO {
    private Long hotelId;
    private String dateStart;
    private String dateEnd;
    private List<Integer> tabNumbers;
    private String eventName;
}
