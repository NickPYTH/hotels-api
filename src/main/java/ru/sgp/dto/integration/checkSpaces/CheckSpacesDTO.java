package ru.sgp.dto.integration.checkSpaces;

import lombok.Data;

import java.util.List;

@Data
public class CheckSpacesDTO {
    private List<TabWithItr> guests;
}
