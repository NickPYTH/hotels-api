package ru.sgp.dto.report;

import lombok.Data;

import java.util.Date;

@Data
public class FilialReportDTO {
    private String filial;
    private String hotel;
    private String flat;
    private String room;
    private String floor;
    private String fio;
    private String dateStart;
    private String dateFinish;
    private String tabnum;
    private String checkouted;
    private String guestFilial;
}
