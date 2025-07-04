package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class HotelDTO {
    private Long id;
    private String name;
    private String location;
    private FilialDTO filial;
    private String mvz;

    // Вычисляемые поля
    private Integer bedsCount;
    private Integer emptyBedsCount;
    private Integer busyBedsCount;
    private Integer flatsCount;
}
