package ru.sgp.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.FlatDTO;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
    StatusRepository statusRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    CommendantsRepository commendantsRepository;
    @Autowired
    UserRepository userRepository;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    @Transactional
    public List<HotelDTO> getAll(Long filialId, String dateStr) throws ParseException {
        Date date = dateTimeFormatter.parse(dateStr);
        Filial filial = filialRepository.getById(filialId);
        List<HotelDTO> response = new ArrayList<>();
        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
            HotelDTO hotelDTO = new HotelDTO();
            hotelDTO.setId(hotel.getId());
            hotelDTO.setName(hotel.getName());
            hotelDTO.setFilialId(filialId);
            hotelDTO.setFilialName(filial.getName());
            List<FlatDTO> flatDTOList = new ArrayList<>();
            int bedsCount = 0;
            int emptyBedsCount = 0;
            int emptyBedsCountWithBusy = 0;
            for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
                FlatDTO flatDTO = new FlatDTO();
                flatDTO.setId(flat.getId());
                flatDTO.setName(flat.getName());
                flatDTO.setFloor(flat.getFloor());
                flatDTO.setStatusId(flat.getStatus().getId());
                flatDTO.setHotelId(flat.getHotel().getId());
                if (flat.getCategory() != null) {
                    flatDTO.setCategoryId(flat.getCategory().getId());
                    flatDTO.setCategory(flat.getCategory().getName());
                }
                flatDTOList.add(flatDTO);
                Status freeStatus = statusRepository.getById(1L); // 1 is free
                for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                    bedsCount += room.getBedsCount();
                    emptyBedsCountWithBusy += room.getBedsCount() - guestRepository.findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).size();
                    if (flat.getStatus().getId() == freeStatus.getId() && room.getStatus().getId() == freeStatus.getId()) {
                        emptyBedsCount += room.getBedsCount() - guestRepository.findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).size();
                    }
                }
            }
            hotelDTO.setBedsCount(bedsCount);
            hotelDTO.setEmptyBedsCount(emptyBedsCount);
            hotelDTO.setEmptyBedsCountWithBusy(emptyBedsCountWithBusy);
            hotelDTO.setFlats(flatDTOList);
            response.add(hotelDTO);
        }
        return response;
    }

    public List<HotelDTO> getAllByCommendant(String dateStr) throws ParseException {
        Date date = dateTimeFormatter.parse(dateStr);
        List<HotelDTO> response = new ArrayList<>();
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        for (Commendant commendant : commendantsRepository.findAllByUser(user)) {
            HotelDTO hotelDTO = new HotelDTO();
            Hotel hotel = commendant.getHotel();
            hotelDTO.setId(hotel.getId());
            hotelDTO.setName(hotel.getName());
            hotelDTO.setFilialId(hotel.getFilial().getId());
            hotelDTO.setFilialName(hotel.getFilial().getName());
            List<FlatDTO> flatDTOList = new ArrayList<>();
            int bedsCount = 0;
            int emptyBedsCount = 0;
            int emptyBedsCountWithBusy = 0;
            for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
                FlatDTO flatDTO = new FlatDTO();
                flatDTO.setId(flat.getId());
                flatDTO.setName(flat.getName());
                flatDTO.setFloor(flat.getFloor());
                flatDTO.setStatusId(flat.getStatus().getId());
                flatDTO.setHotelId(flat.getHotel().getId());
                if (flat.getCategory() != null) {
                    flatDTO.setCategoryId(flat.getCategory().getId());
                    flatDTO.setCategory(flat.getCategory().getName());
                }
                flatDTOList.add(flatDTO);
                Status freeStatus = statusRepository.getById(1L); // 1 is free
                for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                    bedsCount += room.getBedsCount();
                    emptyBedsCountWithBusy += room.getBedsCount() - guestRepository.findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).size();
                    if (flat.getStatus().getId() == freeStatus.getId() && room.getStatus().getId() == freeStatus.getId()) {
                        emptyBedsCount += room.getBedsCount() - guestRepository.findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).size();
                    }
                }
            }
            hotelDTO.setBedsCount(bedsCount);
            hotelDTO.setEmptyBedsCount(emptyBedsCount);
            hotelDTO.setEmptyBedsCountWithBusy(emptyBedsCountWithBusy);
            hotelDTO.setFlats(flatDTOList);
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
                    record.setFloor(flat.getFloor().toString());
                    record.setFloor(flat.getFloor().toString());
                    record.setRoom(room.getId().toString());
                    record.setCheckouted(guest.getCheckouted() ? "+" : "-");
                    record.setDateStart(dateFormatter.format(guest.getDateStart()));
                    record.setDateFinish(dateFormatter.format(guest.getDateFinish()));
                    if (guest.getEmployee() != null)
                        record.setTabnum(guest.getEmployee().getTabnum().toString());
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
                    record.setFloor(flat.getFloor().toString());
                    record.setFloor(flat.getFloor().toString());
                    record.setRoom(room.getId().toString());
                    record.setDateStart(dateFormatter.format(guest.getDateStart()));
                    record.setDateFinish(dateFormatter.format(guest.getDateFinish()));
                    if (guest.getEmployee() != null)
                        record.setTabnum(guest.getEmployee().getTabnum().toString());
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
            //Integer days = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS)));
            Integer bedsByDays = bedsCount.get();

            LocalDate start = dateStart.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            LocalDate end = dateFinish.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            int daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS)));
            int count = 0;
            while(count<=daysCount){
                Date tmp = dateFormatter.parse(start.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                Integer countBusyBeds = 0; // расчет занятых мест в отеле за конкретный день
                for (Guest guest: guestRepository.findAllByDateStartBeforeAndDateFinishAfter(tmp, tmp)){
                    Hotel hotel1 = guest.getRoom().getFlat().getHotel();
                    if (hotel1.getId() == hotel.getId()){
                        countBusyBeds+=1;
                    }
                }
                HotelsStatsReportDTO record1 = new HotelsStatsReportDTO();
                record1.setDate(dateFormatter.format(tmp));
                record1.setHotelId(hotel.getId());
                record1.setType("emptyBeds");
                record1.setValue(bedsByDays-countBusyBeds);
                response.add(record1);

                HotelsStatsReportDTO record2 = new HotelsStatsReportDTO();
                record2.setDate(dateFormatter.format(tmp));
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