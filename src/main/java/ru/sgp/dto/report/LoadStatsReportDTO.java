package ru.sgp.dto.report;

import lombok.Data;

@Data
public class LoadStatsReportDTO {
    private String filial;
    private String hotel;
    private String dates;
    private String percent;
    private Integer allBeds;
    private Integer busyBeds;
}
