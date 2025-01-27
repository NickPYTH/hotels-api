package ru.sgp.dto.report;

import lombok.Data;

@Data
public class MonthReportDTO {
    private String id;
    private String fio;
    private String dateStart;
    private String dateFinish;
    private Integer daysCount;
    private String costFromContract;
    private Float cost;
    private String tabnum;
    private String memo;
}
