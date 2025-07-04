package ru.sgp.dto;

import lombok.Data;

@Data
public class ReservationDTO {
    private Long id;
    private Integer tabnum;
    private String firstname;
    private String lastname;
    private String secondName;
    private String fio;
    private String dateStart;
    private String dateFinish;
    private BedDTO bed;
    private EventKindDTO event;
    private FilialDTO fromFilial;
    private ContractDTO contract;
    private String note;
    private GuestDTO guest;
    private StatusDTO status;
    private String error;
    private Boolean male;
    private Integer familyMemberOfEmployee;
    private Long roomId; // Нужно для бронирования на доп. место
}
