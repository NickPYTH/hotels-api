package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.HotelDTO;
import ru.sgp.dto.report.HotelsStatsReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class HotelService {
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    FlatRepository flatRepository;
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    CommendantsRepository commendantsRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoomLocksRepository roomLocksRepository;
    @Autowired
    FlatLocksRepository flatLocksRepository;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Autowired
    private BedRepository bedRepository;
    @Transactional
    public List<HotelDTO> getAllByFilialId(Long filialId) throws ParseException {
        ModelMapper modelMapper = new ModelMapper();
        return hotelRepository.findAllByFilial(filialRepository.getById(filialId)).stream().map(hotel -> modelMapper.map(hotel, HotelDTO.class)).collect(Collectors.toList());
    }
    @Transactional
    public List<HotelDTO> getAllByFilialIdWithStats(Long filialId, String dateStr) throws ParseException {
        Date date = dateTimeFormatter.parse(dateStr);
        Filial filial = filialRepository.getById(filialId);
        List<HotelDTO> response = new ArrayList<>();
        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
            HotelDTO hotelDTO = new HotelDTO();
            AtomicReference<Integer> bedsCount = new AtomicReference<>(0);
            flatRepository.findAllByHotelOrderById(hotel).forEach(flat -> {
                if (!flat.getTech())
                    roomRepository.findAllByFlatOrderById(flat).forEach(room -> {
                        bedsCount.updateAndGet(v -> v + room.getBedsCount());
                    });
            });
            hotelDTO.setId(hotel.getId());
            hotelDTO.setName(hotel.getName());
            hotelDTO.setBedsCount(bedsCount.get());
            hotelDTO.setFlatsCount(flatRepository.countAllByHotel(hotel));
            hotelDTO.setLocation(hotel.getLocation());
            Integer countBusyBeds = 0;
            List<Long> flatsExcludeList = new ArrayList<>();
            List<Long> roomExcludeList = new ArrayList<>();
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(date, date, hotel)) {
                if (roomExcludeList.contains(guest.getBed().getRoom().getId())) continue;
                if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) continue;
                Room guestRoom = guest.getBed().getRoom();
                Flat guestFlat = guestRoom.getFlat();
                List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, guestRoom);
                List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, guestFlat);
                if (!flatLocksList.isEmpty()) { // Посчитать кол-во мест во всей секции и указать что они заняты
                    if (flatLocksList.get(0).getStatus().getId() == 4L) { // указывать что секции заняты только если они выкупалены организацией (ИД 4)
                        countBusyBeds += bedRepository.countByRoomFlatAndIsExtra(guestFlat, false);
                        flatsExcludeList.add(guestFlat.getId());
                    } else countBusyBeds += 1;
                } else if (!roomLocksList.isEmpty()) { // Посчитать кол-во мест в комнате и указать что они заняты
                    if (roomLocksList.get(0).getStatus().getId() == 3L) { // указывать что комнаты заняты только если они выкупалены организацией (ИД 3)
                        countBusyBeds += bedRepository.countByRoomAndIsExtra(guestRoom, false);
                        roomExcludeList.add(guestRoom.getId());
                    } else countBusyBeds += 1;
                } else countBusyBeds += 1;  // Просто указываем что гость занимает одно место
            }
            if (hotelDTO.getBedsCount() > 0) {
                hotelDTO.setBusyBedsCount(countBusyBeds);
                hotelDTO.setEmptyBedsCount(hotelDTO.getBedsCount() - hotelDTO.getBusyBedsCount());
            } else {
                hotelDTO.setBusyBedsCount(0);
                hotelDTO.setEmptyBedsCount(0);
            }
            response.add(hotelDTO);
        }
        return response;
    }
    @Transactional
    public List<HotelDTO> getAllByCommendant() {
        ModelMapper modelMapper = new ModelMapper();
        List<HotelDTO> response = new ArrayList<>();
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        for (Commendant commendant : commendantsRepository.findAllByUser(user))
            response.add(modelMapper.map(commendant.getHotel(), HotelDTO.class));
        return response;
    }
    public List<HotelDTO> getAllByCommendantWithStats() {
        Date date = new Date();
        List<HotelDTO> response = new ArrayList<>();
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        for (Commendant commendant : commendantsRepository.findAllByUser(user)) {
            Hotel hotel = commendant.getHotel();
            HotelDTO hotelDTO = new HotelDTO();
            AtomicReference<Integer> bedsCount = new AtomicReference<>(0);
            flatRepository.findAllByHotelOrderById(hotel).forEach(flat -> {
                roomRepository.findAllByFlatOrderById(flat).forEach(room -> {
                    bedsCount.updateAndGet(v -> v + room.getBedsCount());
                });
            });
            hotelDTO.setId(hotel.getId());
            hotelDTO.setName(hotel.getName());
            hotelDTO.setBedsCount(bedsCount.get());
            hotelDTO.setFlatsCount(flatRepository.countAllByHotel(hotel));
            hotelDTO.setLocation(hotel.getLocation());
            Integer countBusyBeds = 0;
            List<Long> flatsExcludeList = new ArrayList<>();
            List<Long> roomExcludeList = new ArrayList<>();
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(date, date, hotel)) {
                if (roomExcludeList.contains(guest.getBed().getRoom().getId())) continue;
                if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) continue;
                Room guestRoom = guest.getBed().getRoom();
                Flat guestFlat = guestRoom.getFlat();
                List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, guestRoom);
                List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, guestFlat);
                if (!flatLocksList.isEmpty()) { // Посчитать кол-во мест во всей секции и указать что они заняты
                    if (flatLocksList.get(0).getStatus().getId() == 4L) { // указывать что секции заняты только если они выкупалены организацией (ИД 4)
                        countBusyBeds += bedRepository.countByRoomFlatAndIsExtra(guestFlat, false);
                        flatsExcludeList.add(guestFlat.getId());
                    } else countBusyBeds += 1;
                } else if (!roomLocksList.isEmpty()) { // Посчитать кол-во мест в комнате и указать что они заняты
                    if (roomLocksList.get(0).getStatus().getId() == 3L) { // указывать что комнаты заняты только если они выкупалены организацией (ИД 3)
                        countBusyBeds += bedRepository.countByRoomAndIsExtra(guestRoom, false);
                        roomExcludeList.add(guestRoom.getId());
                    } else countBusyBeds += 1;
                } else countBusyBeds += 1;  // Просто указываем что гость занимает одно место
            }
            if (hotelDTO.getBedsCount() > 0) {
                hotelDTO.setBusyBedsCount(countBusyBeds);
                hotelDTO.setEmptyBedsCount(hotelDTO.getBedsCount() - hotelDTO.getBusyBedsCount());
            } else {
                hotelDTO.setBusyBedsCount(0);
                hotelDTO.setEmptyBedsCount(0);
            }
            response.add(hotelDTO);
        }
        return response;
    }
    public List<HotelsStatsReportDTO> getHotelsStats(Long idFilial, String dateStartStr, String dateFinishStr) throws ParseException {
        List<HotelsStatsReportDTO> response = new ArrayList<>();
        Filial filial = filialRepository.findById(idFilial).get();
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
            AtomicReference<Integer> bedsCount = new AtomicReference<>(0);
            flatRepository.findAllByHotelOrderById(hotel).forEach(flat -> {
                roomRepository.findAllByFlatOrderById(flat).forEach(room -> {
                    bedsCount.updateAndGet(v -> v + room.getBedsCount());
                });
            });
            Integer bedsByDays = bedsCount.get();
            LocalDate start = dateStart.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            int daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS)));
            int count = 0;
            while (count <= daysCount) {
                Date tmp = dateTimeFormatter.parse(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + " 23:59");
                Integer countBusyBeds = 0; // расчет занятых мест в отеле за конкретный день
                List<Long> flatsExcludeList = new ArrayList<>();
                List<Long> roomExcludeList = new ArrayList<>();
                for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(tmp, tmp, hotel)) {
                    if (roomExcludeList.contains(guest.getBed().getRoom().getId())) {
                        continue;
                    }
                    if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) {
                        continue;
                    }
                    Hotel guestHotel = guest.getBed().getRoom().getFlat().getHotel();
                    if (Objects.equals(guestHotel.getId(), hotel.getId())) {
                        Room guestRoom = guest.getBed().getRoom();
                        Flat guestFlat = guestRoom.getFlat();
                        List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(tmp, tmp, guestRoom);
                        List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(tmp, tmp, guestFlat);
                        if (!flatLocksList.isEmpty()) { // Посчитать кол-во мест во всей секции и указать что они заняты
                            if (flatLocksList.get(0).getStatus().getId() == 4L) { // указывать что секции заняты только если они выкупалены организацией (ИД 4)
                                countBusyBeds += bedRepository.countByRoomFlatAndIsExtra(guestFlat, false);
                                flatsExcludeList.add(guestFlat.getId());
                            }
                        } else if (!roomLocksList.isEmpty()) { // Посчитать кол-во мест в комнате и указать что они заняты
                            if (roomLocksList.get(0).getStatus().getId() == 3L) { // указывать что комнаты заняты только если они выкупалены организацией (ИД 3)
                                countBusyBeds += bedRepository.countByRoomAndIsExtra(guestRoom, false);
                                roomExcludeList.add(guestRoom.getId());
                            }
                        } else countBusyBeds += 1;  // Просто указываем что гость занимает одно место
                    }
                }
                HotelsStatsReportDTO record1 = new HotelsStatsReportDTO();
                record1.setDate(dateTimeFormatter.format(tmp));
                record1.setHotelId(hotel.getId());
                record1.setType("emptyBeds");
                record1.setValue(bedsByDays - countBusyBeds);
                response.add(record1);

                HotelsStatsReportDTO record2 = new HotelsStatsReportDTO();
                record2.setDate(dateTimeFormatter.format(tmp));
                record2.setHotelId(hotel.getId());
                record2.setType("busyBeds");
                record2.setValue(countBusyBeds);
                response.add(record2);

                start = start.plusDays(1);
                count++;
            }
        }
        return response;
    }
}