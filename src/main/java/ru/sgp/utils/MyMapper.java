package ru.sgp.utils;

import ru.sgp.dto.GuestDTO;
import ru.sgp.model.Employee;
import ru.sgp.model.Guest;

import java.text.SimpleDateFormat;

public class MyMapper {
    public static GuestDTO GuestToGuestDTO(Guest guest) {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        GuestDTO guestDTO = new GuestDTO();
        if (guest.getEmployee() != null) {
            Employee employee = guest.getEmployee();
            guestDTO.setTabnum(employee.getTabnum());
            guestDTO.setFilialEmployee(employee.getIdFilial().toString());
        }
        if (guest.getContract() != null) guestDTO.setContractId(guest.getContract().getId());
        guestDTO.setId(guest.getId());
        guestDTO.setFirstname(guest.getFirstname());
        guestDTO.setLastname(guest.getLastname());
        guestDTO.setSecondName(guest.getSecondName());
        guestDTO.setNote(guest.getNote());
        guestDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
        guestDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
        guestDTO.setBedId(guest.getBed().getId());
        guestDTO.setBedName(guest.getBed().getName());
        guestDTO.setRoomId(guest.getBed().getRoom().getId());
        guestDTO.setRoomName(guest.getBed().getRoom().getName());
        guestDTO.setFlatName(guest.getBed().getRoom().getFlat().getName());
        guestDTO.setFlatId(guest.getBed().getRoom().getFlat().getId());
        guestDTO.setHotelName(guest.getBed().getRoom().getFlat().getHotel().getName());
        guestDTO.setHotelId(guest.getBed().getRoom().getFlat().getHotel().getId());
        guestDTO.setFilialName(guest.getBed().getRoom().getFlat().getHotel().getFilial().getName());
        guestDTO.setFilialId(guest.getBed().getRoom().getFlat().getHotel().getFilial().getId());
        guestDTO.setMemo(guest.getMemo());
        guestDTO.setMale(guest.getMale());
        guestDTO.setCheckouted(guest.getCheckouted());
        if (guest.getContract() != null) {
            guestDTO.setContractId(guest.getContract().getId());
            guestDTO.setBilling(guest.getContract().getBilling());
            guestDTO.setReason(guest.getContract().getReason().getName());
        }
        if (guest.getOrganization() != null) {
            guestDTO.setOrganizationId(guest.getOrganization().getId());
            guestDTO.setOrganizationName(guest.getOrganization().getName());
        }
        guestDTO.setRegPoMestu(guest.getRegPoMestu());
        return guestDTO;
    }
}
