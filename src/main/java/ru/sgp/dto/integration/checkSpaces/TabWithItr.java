package ru.sgp.dto.integration.checkSpaces;

import lombok.Data;

import java.util.List;

@Data
public class TabWithItr {
    private Integer tabNumber;
    private Boolean isItr;
    private Integer male;
    private String groupNumber;
    private String eventName;
    private List<DatePair> dates;
}
