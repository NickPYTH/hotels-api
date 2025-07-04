package ru.sgp.dto.integration.checkSpaces.response;

import lombok.Data;

import java.util.List;

@Data
public class CheckSpacesResponse {
    private List<CheckSpacesReservation> guests;
}
