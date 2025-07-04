package ru.sgp.dto;

import lombok.Data;
import ru.sgp.dto.chess.ChessDate;

import java.util.List;

@Data
public class BedDTO {
    private Long id;
    private String name;
    private RoomDTO room;
    private Boolean isExtra;

    private Boolean male = null;

    // Вычисляемые поля
    private List<ChessDate> dates;
    // -----
}
