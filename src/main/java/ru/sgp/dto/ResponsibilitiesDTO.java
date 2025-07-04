package ru.sgp.dto;

import lombok.Data;

@Data
public class ResponsibilitiesDTO {
    private Long id;
    private Long employeeId;
    private Integer tabnum;
    private String fio;
    private String filial;
    private Long filialId;
    private Long hotelId;
    private String hotel;
    private String customPost;
}
