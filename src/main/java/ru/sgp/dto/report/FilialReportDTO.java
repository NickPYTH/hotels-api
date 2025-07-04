package ru.sgp.dto.report;

import lombok.Data;

import java.util.Date;

@Data
public class FilialReportDTO {
    private String filial;
    private String hotel;
    private String flat;
    private String room;
    private Integer floor;
    private String fio;
    private String periodStart;
    private String periodFinish;
    private String dateStart;
    private String dateFinish;
    private Integer tabnum;
    private String checkouted;
    private String guestFilial;
    private String contract;
    private Float contractPrice;
    private Float periodPrice;
    private String reason;
    private String billing;
    private Float nights;
    private String creditCard;
}
