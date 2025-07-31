package ru.sgp.dto.integration.checkSpaces;

import lombok.Data;

import java.util.List;

@Data
public class CheckSpacesDTO {
    private String author;
    private List<TabWithItr> guests;
}
