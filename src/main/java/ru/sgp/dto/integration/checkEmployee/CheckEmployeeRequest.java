package ru.sgp.dto.integration.checkEmployee;

import lombok.Data;

@Data
public class CheckEmployeeRequest {
    private Integer tabNumber;
    private String dateStart;
    private String dateFinish;
}
