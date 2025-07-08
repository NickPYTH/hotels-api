package ru.sgp.dto.report;

import lombok.Data;

@Data
public class AvdReportDTO {
    private Integer number;
    private String period;
    private String fio;
    private Integer familyMembersCount;
    private String contragent;
    private Float countDays;
}
