package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class FlatDTO {
    private Long id;
    private String name;
    private Integer floor;
    private Long statusId;
    private String status;
    private Long hotelId;
    private String hotelName;
    private Long filialId;
    private Integer roomsCount;
    private Integer bedsCount;
    private Integer emptyBedsCount;
    private Long categoryId;
    private String category;
    private String note;
    private Boolean tech;
    private List<RoomDTO> rooms;
}
