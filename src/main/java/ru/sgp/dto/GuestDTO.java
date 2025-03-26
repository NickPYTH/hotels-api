package ru.sgp.dto;

import lombok.Data;

@Data
public class GuestDTO {
    // Поля совпадающие с моделью Guest
    private Long id;
    private String firstname;
    private String lastname;
    private String secondName;
    private String note;
    private String dateStart;
    private String dateFinish;
    private OrganizationDTO organization;
    private Boolean regPoMestu;
    private String email;
    private Boolean male;
    private String memo;
    private Boolean checkouted;
    private ContractDTO contract;
    private Integer tabnum;
    private BedDTO bed;
    private Integer familyMemberOfEmployee;
    // -----

    // Вычисляемые поля
    private String filialEmployee;
    private String error;
    private Float cost;
    private Float costByNight;
    private String daysCount;
    private String post;
    // -----

    // Дополнительные поля для бронирования
    private EventDTO event;
    private FilialDTO fromFilial;
    private Boolean isReservation;
    // -----
}
