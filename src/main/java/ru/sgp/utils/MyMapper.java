package ru.sgp.utils;

import ru.sgp.dto.*;
import ru.sgp.model.Bed;
import ru.sgp.model.Employee;
import ru.sgp.model.Guest;

import java.text.SimpleDateFormat;

public class MyMapper {
    public static BedDTO BedToBedDTO(Bed bed) {
        FilialDTO filialDTO = new FilialDTO();
        filialDTO.setId(bed.getRoom().getFlat().getHotel().getFilial().getId());
        filialDTO.setName(bed.getRoom().getFlat().getHotel().getFilial().getName());

        HotelDTO hotelDTO = new HotelDTO();
        hotelDTO.setId(bed.getRoom().getFlat().getHotel().getId());
        hotelDTO.setName(bed.getRoom().getFlat().getHotel().getName());
        hotelDTO.setFilial(filialDTO);

        FlatDTO flatDTO = new FlatDTO();
        flatDTO.setId(bed.getRoom().getFlat().getId());
        flatDTO.setName(bed.getRoom().getFlat().getName());
        flatDTO.setHotel(hotelDTO);

        RoomDTO roomDTO = new RoomDTO();
        roomDTO.setId(bed.getRoom().getId());
        roomDTO.setName(bed.getRoom().getName());
        roomDTO.setFlat(flatDTO);

        BedDTO bedDTO = new BedDTO();
        bedDTO.setId(bed.getId());
        bedDTO.setName(bed.getName());
        bedDTO.setRoom(roomDTO);

        return bedDTO;
    }
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
        guestDTO.setBed(BedToBedDTO(guest.getBed()));
        guestDTO.setMemo(guest.getMemo());
        guestDTO.setMale(guest.getMale());
        guestDTO.setCheckouted(guest.getCheckouted());
        if (guest.getContract() != null) {
            guestDTO.setContractId(guest.getContract().getId());
            guestDTO.setBilling(guest.getContract().getBilling());
            guestDTO.setReason(guest.getContract().getReason().getName());
        }
        if (guest.getOrganization() != null) {
            guestDTO.setFilialEmployee(guest.getOrganization().getName());
            guestDTO.setOrganizationId(guest.getOrganization().getId());
            guestDTO.setOrganizationName(guest.getOrganization().getName());
        }
        if (guest.getFamilyMemberOfEmployee() != null)
            guestDTO.setFamilyMemberOfEmployee(guest.getFamilyMemberOfEmployee().getTabnum());
        guestDTO.setRegPoMestu(guest.getRegPoMestu());
        return guestDTO;
    }
}
