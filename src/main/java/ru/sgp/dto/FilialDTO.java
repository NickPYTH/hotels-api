package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class FilialDTO {
    private Long id;
    private String name;
    private Integer code;
    private String boss;
    private Boolean excluded;

    // Вычисляемые поля
    private List<HotelDTO> hotels;
    private Integer bedsCount;
    private Integer emptyBedsCount;
    private Integer emptyBedsCountWithBusy;
}
