package ru.sgp.dto.integration.cancelReservation;

import lombok.Data;

import java.util.List;

@Data
public class CancelReservationRequest {
    private String author;
    private List<TabAndReservation> reservations;
}