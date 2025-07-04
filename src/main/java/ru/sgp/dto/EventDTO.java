package ru.sgp.dto;

import lombok.Data;

@Data
public class EventDTO {
    private Long id;
    private EventKindDTO kind;
    private HotelDTO hotel;
    private Long dateStart;
    private Long dateFinish;
}
