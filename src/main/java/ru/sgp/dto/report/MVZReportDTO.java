package ru.sgp.dto.report;

import lombok.Data;

import java.util.Date;

@Data
public class MVZReportDTO {
    private String id;
    private String fio;
    private String hotel;
    private Integer daysCount;
    private String tabnum;
    private String filial;
    private String mvz;
    private String mvzName;
    private String guestFilial;
    private String orgUnit;
    private String billing;
    private String reason;
    private Date dateStart;
    private Date dateFinish;
}
