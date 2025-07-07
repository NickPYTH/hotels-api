package ru.sgp.dto.integration.checkEmployee;

import lombok.Data;
import ru.sgp.dto.BedDTO;

import java.util.List;

@Data
public class CheckEmployeeResponse {
    private BedDTO bed;
    private String dateStart;
    private String dateFinish;
}
