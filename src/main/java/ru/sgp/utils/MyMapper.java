package ru.sgp.utils;

import org.modelmapper.ModelMapper;
import ru.sgp.dto.*;
import ru.sgp.model.*;

import java.text.SimpleDateFormat;

public class MyMapper {
    public static OrganizationDTO OrganizationToOrganizationDTO(Organization organization) {
        if (organization == null) return null;
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(organization, OrganizationDTO.class);
    }
    public static ContractDTO ContractToContractDTO(Contract contract) {
        if (contract == null) return null;
        ModelMapper mapper = new ModelMapper();
        return mapper.map(contract, ContractDTO.class);
    }
    public static BedDTO BedToBedDTO(Bed bed) {
        if (bed == null) return null;
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
        if (guest == null) return null;
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        GuestDTO guestDTO = new GuestDTO();
        if (guest.getEmployee() != null) {
            Employee employee = guest.getEmployee();
            guestDTO.setTabnum(employee.getTabnum());
            guestDTO.setFilialEmployee(employee.getIdFilial().toString());
        }
        if (guest.getContract() != null) guestDTO.setContract(ContractToContractDTO(guest.getContract()));
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
        guestDTO.setOrganization(MyMapper.OrganizationToOrganizationDTO(guest.getOrganization()));
        if (guest.getFamilyMemberOfEmployee() != null)
            guestDTO.setFamilyMemberOfEmployee(guest.getFamilyMemberOfEmployee().getTabnum());
        guestDTO.setRegPoMestu(guest.getRegPoMestu());
        return guestDTO;
    }
}
