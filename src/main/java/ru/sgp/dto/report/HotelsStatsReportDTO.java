package ru.sgp.dto.report;

import lombok.Data;

@Data
public class HotelsStatsReportDTO {
    private Long hotelId;
    private String type;
    private Integer value;
    private String date;
}
