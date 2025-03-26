package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoomDTO {
    private Long id;
    private String name;
    private Integer bedsCount;
    private FlatDTO flat;

    // Вычисляемые поля
    private StatusDTO status;
    private Long roomLockId;
    private List<GuestDTO> guests;
    private List<BedDTO> beds;
}
