package ru.sgp.dto.report;

import lombok.Data;

@Data
public class PlanZaezdReportDTO {
    private Integer id;
    private String status;
    private String guest;
    private String dateStart;
    private String dateFinish;
    private String flatName;
    private String billing;
}
