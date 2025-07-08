package ru.sgp.dto;

import lombok.Data;

@Data
public class RoomLocksDTO {
    private Long id;
    private Long roomId;
    private Long statusId;
    private String dateStart;
    private String dateFinish;
    private String error;
}