package ru.sgp.dto;

import lombok.Data;

@Data
public class FlatLocksDTO {
    private Long id;
    private Long flatId;
    private Long statusId;
    private String dateStart;
    private String dateFinish;
    private String error;
}