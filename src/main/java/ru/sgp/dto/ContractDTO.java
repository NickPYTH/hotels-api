package ru.sgp.dto;

import lombok.Data;

@Data
public class ContractDTO {
    private Long id;
    private String docnum;
    private ReasonDTO reason;
    private FilialDTO filial;
    private HotelDTO hotel;
    private OrganizationDTO organization;
    private Float cost;
    private String billing;
    private Integer year;
    private String note;
}
