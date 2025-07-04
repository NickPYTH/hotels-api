package ru.sgp.dto.integration.checkSpaces.response;

import lombok.Data;

import java.util.List;

@Data
public class CheckSpacesReservation {
    private Integer tabNumber;
    private String range;
    private String dateStart;
    private String dateFinish;
    private Integer daysCount;
    private String status;
    private Integer errorCode;
    private String hotel;
}
