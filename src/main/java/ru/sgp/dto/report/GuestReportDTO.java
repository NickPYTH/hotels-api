package ru.sgp.dto.report;

import lombok.Data;

@Data
public class GuestReportDTO {
    private String id;
    private String surname;
    private String name;
    private String secondName;
    private String dateStart;
    private String dateFinish;
    private String room;
    private String hotel;
    private String filial;
    private String org;
    private String repPoMestu;
    private String cz;
    private String billing;
    private String reason;
    private String male;
    private String checkouted;
    private String contract;
    private String bed;
}
