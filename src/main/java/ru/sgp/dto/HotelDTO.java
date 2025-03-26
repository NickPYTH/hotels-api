package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class HotelDTO {
    private Long id;
    private String name;
    private String location;
    private FilialDTO filial;

    // Вычисляемые поля
    private Integer bedsCount;
    private Integer emptyBedsCount;
    private Integer busyBedsCount;
    private Integer flatsCount;
}
