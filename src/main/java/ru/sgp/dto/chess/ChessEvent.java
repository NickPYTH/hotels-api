package ru.sgp.dto.chess;

import lombok.Data;
import ru.sgp.dto.EventDTO;

import java.util.List;

@Data
public class ChessEvent {
    private String date;
    private List<EventDTO> events;
}
