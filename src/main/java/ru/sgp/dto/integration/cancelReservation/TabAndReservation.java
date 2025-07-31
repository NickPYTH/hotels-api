package ru.sgp.dto.integration.cancelReservation;

import lombok.Data;

@Data
public class TabAndReservation {
    private Integer tabNumber;
    private Long reservationId;
}
