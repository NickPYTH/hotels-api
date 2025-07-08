package ru.sgp.dto.chess;

import lombok.Data;

@Data
public class ChessGuest {
    private Long id;
    private String post;
    private Boolean isReservation;
    private String name;
    private String lastname;
    private String secondName;
    private Boolean male;
    private Long dateStart;
    private Long dateFinish;
    private String note;
    private Boolean isCheckouted;
}
