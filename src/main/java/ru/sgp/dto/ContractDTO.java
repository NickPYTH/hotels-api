package ru.sgp.dto;

import lombok.Data;

@Data
public class ContractDTO {
    private Long id;
    private String filial;
    private Long filialId;
    private String hotel;
    private Long hotelId;
    private String organization;
    private Long organizationId;
    private String docnum;
    private Float cost;
    private String note;
    private String billing;
    private String osnovanie;
    private Long reasonId;
    private Integer year;
    private Integer roomNumber;
}
