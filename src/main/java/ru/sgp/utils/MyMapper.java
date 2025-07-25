package ru.sgp.utils;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import ru.sgp.dto.*;
import ru.sgp.model.*;

import java.text.SimpleDateFormat;

public class MyMapper {
    public static StatusDTO StatusToStatusDTO(Status status) {
        if (status == null) return null;
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(status, StatusDTO.class);
    }
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
    public static RoomDTO RoomToRoomDTO(Room room) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        if (room == null) return null;
        return modelMapper.map(room, RoomDTO.class);
    }
    public static FlatDTO FlatToFlatDTO(Flat flat) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        if (flat == null) return null;
        return modelMapper.map(flat, FlatDTO.class);
    }
    public static HotelDTO HotelToHotelDTO(Hotel hotel) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        if (hotel == null) return null;
        return modelMapper.map(hotel, HotelDTO.class);
    }
    public static FilialDTO FilialToFilialDTO(Filial filial) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        if (filial == null) return null;
        return modelMapper.map(filial, FilialDTO.class);
    }
    public static ReservationDTO ReservationToReservationDTO(Reservation reservation) {
        ModelMapper modelMapper = new ModelMapper();
        if (reservation == null) return null;
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        ReservationDTO reservationDTO = new ReservationDTO();
        reservationDTO.setId(reservation.getId());
        reservationDTO.setFirstname(reservation.getFirstname());
        reservationDTO.setLastname(reservation.getLastname());
        reservationDTO.setSecondName(reservation.getSecondName());
        reservationDTO.setFio(reservation.getLastname() + " " + reservation.getFirstname() + " " + reservation.getSecondName());
        reservationDTO.setTabnum(reservation.getTabnum());
        reservationDTO.setNote(reservation.getNote());
        reservationDTO.setBed(BedToBedDTO(reservation.getBed()));
        if (reservation.getDateStart() != null) reservationDTO.setDateStart(dateTimeFormatter.format(reservation.getDateStart()));
        if (reservation.getDateFinish() != null) reservationDTO.setDateFinish(dateTimeFormatter.format(reservation.getDateFinish()));
        if (reservation.getContract() != null) reservationDTO.setContract(ContractToContractDTO(reservation.getContract()));
        if (reservation.getEventKind() != null) reservationDTO.setEvent(modelMapper.map(reservation.getEventKind(), EventKindDTO.class));
        if (reservation.getFromFilial() != null) reservationDTO.setFromFilial(FilialToFilialDTO(reservation.getFromFilial()));
        reservationDTO.setFamilyMemberOfEmployee(reservation.getFamilyMemberOfEmployee());
        reservationDTO.setMale(reservation.getMale());
        return reservationDTO;
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
        guestDTO.setCreditCard(guest.getCreditCard());
        if (guest.getDateStart() != null) guestDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
        if (guest.getDateFinish() != null) guestDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
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
