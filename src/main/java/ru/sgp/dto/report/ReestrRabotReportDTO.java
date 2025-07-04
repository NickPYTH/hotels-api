package ru.sgp.dto.report;

import lombok.Data;

@Data
public class ReestrRabotReportDTO {
    private String bs;
    private String filial;
    private Float workSize;

    // Для режима сторонних организаций
    private String organization;
    private String billing;
    private Float cost;
}
