package ru.sgp.dto;

import lombok.Data;

@Data
public class BedDTO {
    private Long id;
    private String name;
    private RoomDTO room;
    private Boolean isExtra;
}
