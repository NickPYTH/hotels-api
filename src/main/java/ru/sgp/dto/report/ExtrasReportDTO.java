package ru.sgp.dto.report;

import lombok.Data;

@Data
public class ExtrasReportDTO {
    private Integer number;
    private String name;
    private String guest;
    private String dateStart;
    private String dateFinish;
    private String billing;
    private Integer count;
    private Float cost;
}
