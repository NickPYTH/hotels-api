package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class HotelDTO {
    private Long id;
    private String name;
    private Long filialId;
    private String filialName;
    private Integer bedsCount;
    private Integer emptyBedsCount;
    private Integer busyBedsCount;
    private Integer flatsCount;
    private String location;
    private FilialDTO filial;
}
