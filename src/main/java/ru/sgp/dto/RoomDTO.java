package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoomDTO {
    private Long id;
    private String name;
    private Integer bedsCount;
    private Long flatId;
    private Long filialId;
    private Long hotelId;
    private Long statusId;
    private String statusName;
    private Long roomLockId;
    private List<GuestDTO> guests;
    private List<BedDTO> beds;
    private FlatDTO flat;
}
