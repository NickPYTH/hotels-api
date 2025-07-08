package ru.sgp.dto.report;

import lombok.Data;

@Data
public class YearReservationReportDTO {
    private Integer number;
    private Integer filialNumber;
    private String filialName;
    private Integer count;
    private Float percent;
    private Integer january;
    private Integer february;
    private Integer march;
    private Integer april;
    private Integer may;
    private Integer june;
    private Integer july;
    private Integer august;
    private Integer september;
    private Integer october;
    private Integer november;
    private Integer december;
}
