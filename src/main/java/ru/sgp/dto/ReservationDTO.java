package ru.sgp.dto;

import lombok.Data;

@Data
public class ReservationDTO {
    private Long id;
    private Integer tabnum;
    private String firstname;
    private String lastname;
    private String secondname;
    private String fio;
    private String dateStart;
    private String dateFinish;
    private BedDTO bed;
    private EventDTO event;
    private FilialDTO fromFilial;
    private String note;
    private GuestDTO guest;
    private StatusDTO status;
    private String error;
}
