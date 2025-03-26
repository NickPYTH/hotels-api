package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class FlatDTO {
    private Long id;
    private String name;
    private Integer floor;
    private String note;
    private Boolean tech;
    private HotelDTO hotel;
    private CategoryDTO category;

    // Вычисляемые поля
    private StatusDTO status;
    private Integer roomsCount;
    private Integer bedsCount;
    private Integer emptyBedsCount;
    private Long flatLockId;
    private List<RoomDTO> rooms;
}
