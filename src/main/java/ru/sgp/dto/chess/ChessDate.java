package ru.sgp.dto.chess;

import lombok.Data;

import java.util.List;

@Data
public class ChessDate {
    private String date;
    private List<ChessGuest> guests;
}
