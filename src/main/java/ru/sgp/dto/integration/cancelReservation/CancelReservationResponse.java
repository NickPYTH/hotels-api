package ru.sgp.dto.integration.cancelReservation;

import lombok.Data;

@Data
public class CancelReservationResponse {
    private Integer tabNumber;
    private Long reservationId;
    private String status;
}
