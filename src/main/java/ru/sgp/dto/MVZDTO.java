package ru.sgp.dto;

import lombok.Data;

@Data
public class MVZDTO {
    private Long id;
    private String employeeTab;
    private String employeeFio;
    private String mvz;
    private String mvzName;
    private FilialDTO filial;
    private String organization;
}
