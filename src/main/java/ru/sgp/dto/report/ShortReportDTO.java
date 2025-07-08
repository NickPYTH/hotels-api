package ru.sgp.dto.report;

import lombok.Data;

@Data
public class ShortReportDTO {
    private String filial;
    private String hotel;
    private Integer percent;
    private Integer all;
    private Integer vacant;
    private Integer busy;
}
