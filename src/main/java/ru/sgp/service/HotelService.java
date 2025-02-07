package ru.sgp.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.HotelDTO;
import ru.sgp.dto.report.FilialReportDTO;
import ru.sgp.dto.report.HotelsStatsReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    ModelMapper modelMapper = new ModelMapper();
    @Autowired
    private BedRepository bedRepository;

    @Transactional
    public List<HotelDTO> getAllByFilialIdWithStats(Long filialId, String dateStr) throws ParseException {
        Date date = dateTimeFormatter.parse(dateStr);
        Filial filial = filialRepository.getById(filialId);
        List<HotelDTO> response = new ArrayList<>();
        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
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
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoomFlatHotel(date, date, hotel)) {
                if (roomExcludeList.contains(guest.getBed().getRoom().getId())) continue;
                if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) continue;
                Hotel guestHotel = guest.getRoom().getFlat().getHotel();
                if (guestHotel.getId() == hotel.getId()) {
                    Room guestRoom = guest.getRoom();
                    Flat guestFlat = guestRoom.getFlat();
                    List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, guestRoom);
                    List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, guestFlat);
                    if (!flatLocksList.isEmpty()) { // Посчитать кол-во мест во всей секции и указать что они заняты
                        if (flatLocksList.get(0).getStatus().getId() == 4L) { // указывать что секции заняты только если они выкупалены организацией (ИД 4)
                            countBusyBeds += bedRepository.countByRoomFlat(guestFlat);
                            flatsExcludeList.add(guestFlat.getId());
                        }
                    } else if (!roomLocksList.isEmpty()) { // Посчитать кол-во мест в комнате и указать что они заняты
                        if (roomLocksList.get(0).getStatus().getId() == 3L) { // указывать что комнаты заняты только если они выкупалены организацией (ИД 3)
                            countBusyBeds += bedRepository.countByRoom(guestRoom);
                            roomExcludeList.add(guestRoom.getId());
                        }
                    } else countBusyBeds += 1;  // Просто указываем что гость занимает одно место
                }
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
    public List<HotelDTO> getAllByFilialId(Long filialId) throws ParseException {
        return hotelRepository.findAllByFilial(filialRepository.getById(filialId)).stream().map(hotel -> modelMapper.map(hotel, HotelDTO.class)).collect(Collectors.toList());
    }

    @Transactional
    public List<HotelDTO> getAllByCommendant() {
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
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoomFlatHotel(date, date, hotel)) {
                if (roomExcludeList.contains(guest.getBed().getRoom().getId())) continue;
                if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) continue;
                Hotel guestHotel = guest.getRoom().getFlat().getHotel();
                if (Objects.equals(guestHotel.getId(), hotel.getId())) {
                    Room guestRoom = guest.getRoom();
                    Flat guestFlat = guestRoom.getFlat();
                    List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, guestRoom);
                    List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, guestFlat);
                    if (!flatLocksList.isEmpty()) { // Посчитать кол-во мест во всей секции и указать что они заняты
                        if (flatLocksList.get(0).getStatus().getId() == 4L) { // указывать что секции заняты только если они выкупалены организацией (ИД 4)
                            countBusyBeds += bedRepository.countByRoomFlat(guestFlat);
                            flatsExcludeList.add(guestFlat.getId());
                        }
                    } else if (!roomLocksList.isEmpty()) { // Посчитать кол-во мест в комнате и указать что они заняты
                        if (roomLocksList.get(0).getStatus().getId() == 3L) { // указывать что комнаты заняты только если они выкупалены организацией (ИД 3)
                            countBusyBeds += bedRepository.countByRoom(guestRoom);
                            roomExcludeList.add(guestRoom.getId());
                        }
                    } else countBusyBeds += 1;  // Просто указываем что гость занимает одно место
                }
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

    public byte[] export(JasperPrint jasperPrint) throws JRException {
        Exporter exporter;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();
        return outputStream.toByteArray();
    }

    public byte[] getHotelReport(Long hotelId, boolean checkouted, String dateStartStr, String dateFinishStr) throws JRException, ParseException {
        List<FilialReportDTO> reportData = new ArrayList<>();
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        Hotel hotel = hotelRepository.findById(hotelId).get();
        Filial filial = hotel.getFilial();
        for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                List<Guest> guests = new ArrayList<>();
                if (checkouted)
                    guests = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(dateFinish, dateStart, room);
                else
                    guests = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndCheckoutedAndRoom(dateFinish, dateStart, checkouted, room);
                guests.forEach(guest -> {
                    FilialReportDTO record = new FilialReportDTO();
                    record.setFilial(filial.getName());
                    record.setHotel(hotel.getName());
                    record.setFlat(flat.getName());
                    record.setFloor(flat.getFloor());
                    record.setRoom(room.getId().toString());
                    record.setCheckouted(guest.getCheckouted() ? "+" : "-");
                    record.setDateStart(dateFormatter.format(guest.getDateStart()));
                    record.setDateFinish(dateFormatter.format(guest.getDateFinish()));
                    if (guest.getEmployee() != null)
                        record.setTabnum(guest.getEmployee().getTabnum());
                    record.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                    reportData.add(record);
                });
            }
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/FilialReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

    public byte[] getFloorReport(Long hotelId, Integer floor, String dateStr) throws JRException, ParseException {
        Date date = dateTimeFormatter.parse(dateStr);
        List<FilialReportDTO> reportData = new ArrayList<>();
        Hotel hotel = hotelRepository.findById(hotelId).get();
        Filial filial = hotel.getFilial();
        for (Flat flat : flatRepository.findAllByHotelAndFloorOrderById(hotel, floor)) {
            for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                guestRepository.findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).forEach(guest -> {
                    FilialReportDTO record = new FilialReportDTO();
                    record.setFilial(filial.getName());
                    record.setHotel(hotel.getName());
                    record.setFlat(flat.getName());
                    record.setFloor(flat.getFloor());
                    record.setRoom(room.getId().toString());
                    record.setDateStart(dateFormatter.format(guest.getDateStart()));
                    record.setDateFinish(dateFormatter.format(guest.getDateFinish()));
                    if (guest.getEmployee() != null)
                        record.setTabnum(guest.getEmployee().getTabnum());
                    record.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                    reportData.add(record);
                });
            }
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/FilialReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
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
                for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoomFlatHotel(tmp, tmp, hotel)) {
                    if (roomExcludeList.contains(guest.getBed().getRoom().getId())) {
                        continue;
                    }
                    if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) {
                        continue;
                    }
                    Hotel guestHotel = guest.getRoom().getFlat().getHotel();
                    if (Objects.equals(guestHotel.getId(), hotel.getId())) {
                        Room guestRoom = guest.getRoom();
                        Flat guestFlat = guestRoom.getFlat();
                        List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(tmp, tmp, guestRoom);
                        List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(tmp, tmp, guestFlat);
                        if (!flatLocksList.isEmpty()) { // Посчитать кол-во мест во всей секции и указать что они заняты
                            if (flatLocksList.get(0).getStatus().getId() == 4L) { // указывать что секции заняты только если они выкупалены организацией (ИД 4)
                                countBusyBeds += bedRepository.countByRoomFlat(guestFlat);
                                flatsExcludeList.add(guestFlat.getId());
                            }
                        } else if (!roomLocksList.isEmpty()) { // Посчитать кол-во мест в комнате и указать что они заняты
                            if (roomLocksList.get(0).getStatus().getId() == 3L) { // указывать что комнаты заняты только если они выкупалены организацией (ИД 3)
                                countBusyBeds += bedRepository.countByRoom(guestRoom);
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