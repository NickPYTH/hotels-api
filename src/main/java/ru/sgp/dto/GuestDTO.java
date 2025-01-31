package ru.sgp.dto;

import lombok.Data;

@Data
public class GuestDTO {
    private Long id;
    private Integer tabnum;
    private String email;
    private String firstname;
    private String lastname;
    private String secondName;
    private String note;
    private String dateStart;
    private String dateFinish;
    private Long roomId;
    private String roomName;
    private Long flatId;
    private String flatName;
    private Long hotelId;
    private String hotelName;
    private Long filialId;
    private String filialEmployee;
    private String filialName;
    private String organization;
    private Boolean regPoMestu;
    private String memo;
    private String billing;
    private String reason;
    private String error;
    private Boolean male;
    private Float cost;
    private Float costByNight;
    private String daysCount;
    private String post;
    private Boolean checkouted;
    private String contractNumber;
    private Long contractId;
    private Long bedId;
    private String bedName;
}
