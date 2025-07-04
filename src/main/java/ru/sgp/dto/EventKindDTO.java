package ru.sgp.dto;

import lombok.Data;

@Data
public class EventKindDTO {
    private Long id;
    private String name;
    private String description;
    private EventTypeDTO type;
    private String dateStart;
    private String dateFinish;
    private HotelDTO hotel;
    private Integer manCount;
    private Integer womenCount;
}
