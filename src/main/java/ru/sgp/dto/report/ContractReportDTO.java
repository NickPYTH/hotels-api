package ru.sgp.dto.report;

import lombok.Data;

@Data
public class ContractReportDTO {
    private Long id;
    private String filial;
    private String hotel;
    private String company;
    private String contractNumber;
    private Float cost;
    private String billing;
    private String note;
    private Integer year;
    private String roomName;
    private String reason;
}
