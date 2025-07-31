package ru.sgp.dto.integration.checkEmployee;

import lombok.Data;
import ru.sgp.dto.BedDTO;

import java.util.List;

@Data
public class CheckEmployeeResponse {
    private Integer tabNumber;
    private String hotel;
    private String dateStart;
    private String dateFinish;
    private Long reservationId;

}
