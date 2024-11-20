package ru.sgp.dto;

import lombok.Data;

@Data
public class LogDTO {
    private Long id;
    private String status;
    private String user;
    private String path;
    private Double duration;
    private String message;
    private String date;
}
