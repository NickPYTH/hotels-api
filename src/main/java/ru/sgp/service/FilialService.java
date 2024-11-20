package ru.sgp.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sgp.dto.FilialDTO;
import ru.sgp.dto.HotelDTO;
import ru.sgp.dto.report.FilialReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FilialService {
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    FlatRepository flatRepository;
    @Autowired
    StatusRepository statusRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    GuestRepository guestRepository;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private ReasonRepository reasonRepository;
    @Autowired
    private BedRepository bedRepository;

    @Transactional
    public List<FilialDTO> getAll(String dateStr) throws ParseException {
        Date date = dateTimeFormatter.parse(dateStr);
        List<FilialDTO> response = new ArrayList<>();
        for (Filial filial : filialRepository.findAll()) {
            FilialDTO filialDTO = new FilialDTO();
            filialDTO.setId(filial.getId());
            filialDTO.setName(filial.getName());
            List<HotelDTO> hotelDTOList = new ArrayList<>();
            int bedsCount = 0;
            int emptyBedsCount = 0;
            int emptyBedsCountWithBusy = 0;
            for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
                HotelDTO hotelDTO = new HotelDTO();
                hotelDTO.setId(hotel.getId());
                hotelDTO.setName(hotel.getName());
                hotelDTO.setFilialId(filial.getId());
                hotelDTOList.add(hotelDTO);
                for (Flat flat : flatRepository.findAllByHotelOrderById(hotel)) {
                    Status freeStatus = statusRepository.getById(1L); // 1 is free
                    for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                        bedsCount += room.getBedsCount();
                        emptyBedsCountWithBusy += room.getBedsCount() - guestRepository.findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).size();
                        if (flat.getStatus().getId() == freeStatus.getId() && room.getStatus().getId() == freeStatus.getId()) {
                            emptyBedsCount += room.getBedsCount() - guestRepository.findAllByRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).size();
                        }
                    }
                }
            }
            filialDTO.setBedsCount(bedsCount);
            filialDTO.setEmptyBedsCount(emptyBedsCount);
            filialDTO.setEmptyBedsCountWithBusy(emptyBedsCountWithBusy);
            filialDTO.setHotels(hotelDTOList);
            response.add(filialDTO);
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

    public byte[] getFilialReport(Long filialId, boolean checkouted, String dateStartStr, String dateFinishStr) throws JRException, ParseException {
        List<FilialReportDTO> reportData = new ArrayList<>();
        Date dateStart = dateFormatter.parse(dateStartStr);
        Date dateFinish = dateFormatter.parse(dateFinishStr);
        Filial filial = filialRepository.findById(filialId).orElse(null);
        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
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
                        record.setRoom(room.getName());
                        record.setDateStart(dateFormatter.format(guest.getDateStart()));
                        record.setDateFinish(dateFormatter.format(guest.getDateFinish()));
                        record.setCheckouted(guest.getCheckouted() ? "+" : "-");
                        if (guest.getEmployee() != null) {
                            record.setTabnum(guest.getEmployee().getTabnum().toString());
                            record.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
                        } else {
                            record.setTabnum("");
                            record.setGuestFilial(guest.getOrganization().getName());
                        }
                        record.setFio(guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
                        reportData.add(record);
                    });
                }
            }
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/FilialReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

    @Transactional
    public List<String> dataLoad(MultipartFile file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 400; i++) {
            XSSFRow row = worksheet.getRow(i);
            if (row == null) {
                break;
            }
            String filial = "";
            String hotel = "";
            String location = "";
            Double floor = 0.0;
            String roomName = "";
            String flatName = "";
            Double emptyBeds = 0.0;
            String description = "";
            String tech = "";
            for (int j = 0; j < 11; j++) {

                if (row.getCell(j).getCellType() == CellType.STRING) {
                    if (j == 0)
                        filial = row.getCell(j).getStringCellValue();
                    else if (j == 1)
                        hotel = row.getCell(j).getStringCellValue();
                    else if (j == 2)
                        location = row.getCell(j).getStringCellValue();
                    else if (j == 9)
                        tech = row.getCell(j).getStringCellValue();
                    else if (j == 10)
                        description = row.getCell(j).getStringCellValue();
                } else {
                    if (j == 3)
                        floor = row.getCell(j).getNumericCellValue();
                    else if (j == 4)
                        flatName = String.valueOf(row.getCell(j).getNumericCellValue());
                    else if (j == 5)
                        roomName = String.valueOf(row.getCell(j).getNumericCellValue());
                    else if (j == 6)
                        emptyBeds = row.getCell(j).getNumericCellValue();
                }
            }
            Filial filialModel = filialRepository.findByName(filial);
            if (filialModel == null) {
                filialModel = new Filial();
                filialModel.setName(filial);
                //filialRepository.save(filialModel);
            }
            Hotel hotelModel = hotelRepository.findByName(hotel);
            if (hotelModel == null) {
                hotelModel = new Hotel();
                hotelModel.setName(hotel);
                hotelModel.setLocation(location);
                hotelModel.setFilial(filialModel);
                //hotelRepository.save(hotelModel);
            }
            Flat flatModel = flatRepository.findByNameAndFloorAndHotel(flatName.split("\\.")[0], floor.intValue(), hotelModel);
            if (flatModel == null) {
                flatModel = new Flat();
                flatModel.setName(flatName.split("\\.")[0]);
                flatModel.setFloor(floor.intValue());
                flatModel.setStatus(statusRepository.getById(1L));
                flatModel.setHotel(hotelModel);
                if (tech.length() > 0) flatModel.setTech(true);
                else flatModel.setTech(false);
                flatRepository.save(flatModel);
            }
            Room roomModel = new Room();
            roomModel.setName(roomName.split("\\.")[0]);
            roomModel.setBedsCount(emptyBeds.intValue());
            for (Integer u = 1; u <= emptyBeds.intValue(); u++) {
                Bed bedModel = new Bed();
                bedModel.setRoom(roomModel);
                bedModel.setName(u.toString());
                bedRepository.save(bedModel);
            }
            roomModel.setStatus(statusRepository.getById(1L));
            roomModel.setFlat(flatModel);
            roomModel.setNote(description);
            roomRepository.save(roomModel);
        }
        List<String> response = new ArrayList<>();
        return response;
    }

    @Transactional
    public List<String> dataLoadContracts(MultipartFile file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 400; i++) {
            XSSFRow row = worksheet.getRow(i);
            if (row == null) {
                break;
            }
            String filial = "";
            String hotel = "";
            String organization = "";
            String reason = "";
            String billing = "";
            String docnum = "";
            Double cost = 0.0;
            for (int j = 0; j < 8; j++) {
                if (row.getCell(j) != null) {
                    if (row.getCell(j).getCellType() == CellType.STRING) {
                        if (j == 1)
                            filial = row.getCell(j).getStringCellValue();
                        if (j == 2)
                            hotel = row.getCell(j).getStringCellValue();
                        if (j == 3)
                            organization = row.getCell(j).getStringCellValue();
                        if (j == 4)
                            reason = row.getCell(j).getStringCellValue();
                        if (j == 5)
                            billing = row.getCell(j).getStringCellValue();
                        if (j == 6)
                            docnum = row.getCell(j).getStringCellValue();
                    } else {
                        if (j == 7)
                            cost = row.getCell(j).getNumericCellValue();
                    }
                }
            }
            Filial filialModel = filialRepository.findByName(filial);
            Hotel hotelModel = hotelRepository.findByName(hotel);
            Organization organizationModel = organizationRepository.findByName(organization);
            Contract contractModel = new Contract();
            contractModel.setFilial(filialModel);
            contractModel.setHotel(hotelModel);
            contractModel.setOrganization(organizationModel);
            Reason reasonModel = reasonRepository.findByName(reason);
            if (reasonModel == null) {
                reasonModel = new Reason();
                reasonModel.setName(reason);
                reasonModel.setIsDefault(true);
                reasonRepository.save(reasonModel);
                contractModel.setReason(reasonModel);
            } else contractModel.setReason(reasonModel);
            contractModel.setBilling(billing);
            contractModel.setDocnum(docnum);
            contractModel.setCost(cost.floatValue());
            contractRepository.save(contractModel);
        }
        List<String> response = new ArrayList<>();
        return response;
    }
}