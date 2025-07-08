package ru.sgp.dto;

import lombok.Data;

@Data
public class ReasonDTO {
    private Long id;
    private String name;
    private Boolean isDefault;
}
