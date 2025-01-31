package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class BedDTO {
    private Long id;
    private String name;
    private GuestDTO guest;
}
