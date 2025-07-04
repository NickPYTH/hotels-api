package ru.sgp.dto.report;

import lombok.Data;

@Data
public class ReservationConfirmReportDTO {
    private Integer n;
    private Integer number;
    private String period;
    private String fio;
    private String type;
    private String flat;
    private Float cost;
    private Float days;
    private Float summary;
}
