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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.sgp.dto.FilialDTO;
import ru.sgp.dto.HotelDTO;
import ru.sgp.dto.report.FilialReportDTO;
import ru.sgp.dto.report.ShortReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static ru.sgp.spnego.SpnegoHelper.findUsernameByTabnum;

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
    ModelMapper modelMapper = new ModelMapper();

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private ReasonRepository reasonRepository;
    @Autowired
    private BedRepository bedRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ResponsibilitiesRepository responsibilitiesRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommendantsRepository commendantsRepository;

    @Transactional
    public List<FilialDTO> getAllWithStats(String dateStr) throws ParseException {
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

    @Transactional
    public List<FilialDTO> getAll() throws ParseException {
        return filialRepository.findAll().stream().map(filial -> modelMapper.map(filial, FilialDTO.class)).collect(Collectors.toList());
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
                        record.setFloor(flat.getFloor());
                        record.setRoom(room.getName());
                        record.setDateStart(dateFormatter.format(guest.getDateStart()));
                        record.setDateFinish(dateFormatter.format(guest.getDateFinish()));
                        record.setCheckouted(guest.getCheckouted() ? "+" : "-");
                        Date cuttedStartDate = null;
                        Date cuttedFinishDate = null;
                        int daysCount = 0;
                        try {
                            cuttedStartDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateStart()));
                            cuttedFinishDate = dateFormatter.parse(dateTimeFormatter.format(guest.getDateFinish()));
                            daysCount = Integer.parseInt(String.valueOf(TimeUnit.DAYS.convert(cuttedFinishDate.getTime() - cuttedStartDate.getTime(), TimeUnit.MILLISECONDS)));
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                        if (daysCount == 0) daysCount = 1;
                        record.setNights(daysCount);
                        if (guest.getContract() != null) {
                            record.setContract(guest.getContract().getDocnum());
                            record.setContractPrice(guest.getContract().getCost());
                            record.setPeriodPrice(daysCount * guest.getContract().getCost());
                        } else {
                            record.setContract("");
                            record.setContractPrice(0f);
                            record.setPeriodPrice(0f);
                        }
                        record.setReason(guest.getReason() != null ? guest.getReason().getName() : "");
                        record.setBilling(guest.getBilling());
                        if (guest.getEmployee() != null) {
                            record.setTabnum(guest.getEmployee().getTabnum());
                            record.setGuestFilial(filialRepository.findByCode(guest.getEmployee().getIdFilial()).getName());
                        } else {
                            record.setTabnum(0);
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

    public byte[] getShortReport() throws JRException {
        List<ShortReportDTO> reportData = new ArrayList<>();
        for (Filial filial : filialRepository.findAll()) {
            for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
                ShortReportDTO record = new ShortReportDTO();
                record.setFilial(filial.getName());
                record.setHotel(hotel.getName());
                AtomicReference<Integer> bedsCount = new AtomicReference<>(0);
                flatRepository.findAllByHotelOrderById(hotel).forEach(flat -> {
                    roomRepository.findAllByFlatOrderById(flat).forEach(room -> {
                        bedsCount.updateAndGet(v -> v + room.getBedsCount());
                    });
                });
                record.setAll(bedsCount.get());
                Integer countBusyBeds = 0;
                List<Long> flatsExcludeList = new ArrayList<>();
                List<Long> roomExcludeList = new ArrayList<>();
                for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoomFlatHotel(new Date(), new Date(), hotel)) {
                    if (roomExcludeList.contains(guest.getBed().getRoom().getId())) continue;
                    if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) continue;
                    Hotel guestHotel = guest.getRoom().getFlat().getHotel();
                    if (guestHotel.getId() == hotel.getId()) {
                        Room guestRoom = guest.getRoom();
                        Flat guestFlat = guestRoom.getFlat();
                        if (guestFlat.getStatus().getId() == 4L) { // Посчитать кол-во мест во всей секции и указать что они заняты
                            countBusyBeds += bedRepository.countByRoomFlat(guestFlat);
                            flatsExcludeList.add(guestFlat.getId());
                        } else if (guestRoom.getStatus().getId() == 3L) { // Посчитать кол-во мест в комнате и указать что они заняты
                            countBusyBeds += bedRepository.countByRoom(guestRoom);
                            roomExcludeList.add(guestRoom.getId());
                        } else countBusyBeds += 1;  // Просто указываем что гость занимает одно место
                    }
                }
                if (record.getAll() > 0) {
                    record.setBusy(countBusyBeds);
                    record.setVacant(record.getAll() - record.getBusy());
                    record.setPercent((int) (((float) record.getBusy() / (float) record.getAll()) * 100));
                } else {
                    record.setBusy(0);
                    record.setVacant(0);
                    record.setPercent(0);
                }
                reportData.add(record);
            }
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/ShortReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("date", dateTimeFormatter.format(new Date()));
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

    @Transactional
    public List<String> dataLoad(MultipartFile file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 900; i++) {
            XSSFRow row = worksheet.getRow(i);
            if (row == null) {
                break;
            }
            int filial = 0;
            String hotel = "";
            String location = "";
            Double floor = 0.0;
            String roomName = "";
            String flatName = "";
            Double emptyBeds = 0.0;
            String description = "";
            String tech = "";
            for (int j = 0; j < 10; j++) {

                if (row.getCell(j).getCellType() == CellType.STRING) {
                    if (j == 0) {
                        //filial = row.getCell(j).getStringCellValue();
                    } else if (j == 2) {
                        hotel = row.getCell(j).getStringCellValue();
                    } else if (j == 3) {
                        location = row.getCell(j).getStringCellValue();
                    } else if (j == 8)
                        tech = row.getCell(j).getStringCellValue();
                    else if (j == 9)
                        description = row.getCell(j).getStringCellValue();
                } else {
                    if (j == 4)
                        floor = row.getCell(j).getNumericCellValue();
                    else if (j == 0) {
                        filial = (int) row.getCell(j).getNumericCellValue();
                    } else if (j == 5)
                        flatName = String.valueOf(row.getCell(j).getNumericCellValue());
                    else if (j == 6)
                        roomName = String.valueOf(row.getCell(j).getNumericCellValue());
                    else if (j == 7)
                        emptyBeds = row.getCell(j).getNumericCellValue();
                }
            }
            Filial filialModel = filialRepository.findByCode(filial);
            if (filialModel == null) {
                filialModel = new Filial();
                //filialModel.setName(filial);
                //filialRepository.save(filialModel);
            }
            Hotel hotelModel = hotelRepository.findByNameAndFilial(hotel.trim(), filialModel);
//            for (Hotel h : hotelRepository.findAll()) {
//                if (h.getFilial().getId() != 910L) {
//                    bedRepository.deleteAllByRoomFlatHotel(h);
//                    roomRepository.deleteAllByFlatHotel(h);
//                    flatRepository.deleteAllByHotel(h);
//                    hotelRepository.delete(h);
//                }
//            }

            if (hotelModel == null) {
                hotelModel = new Hotel();
                hotelModel.setName(hotel.trim());
                hotelModel.setLocation(location.trim());
                hotelModel.setFilial(filialModel);
                hotelRepository.save(hotelModel);
            }
            Flat flatModel = flatRepository.findByNameAndFloorAndHotel(flatName.split("\\.")[0], floor.intValue(), hotelModel);
            if (flatModel == null) {
                flatModel = new Flat();
                flatModel.setName(flatName.trim().split("\\.")[0]);
                flatModel.setFloor(floor.intValue());
                flatModel.setStatus(statusRepository.getById(1L));
                flatModel.setHotel(hotelModel);
                if (tech.length() > 0) flatModel.setTech(true);
                else flatModel.setTech(false);
                flatRepository.save(flatModel);
            }
            Room roomModel = new Room();
            roomModel.setName(roomName.trim().split("\\.")[0]);
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
                            filial = row.getCell(j).getStringCellValue().trim();
                        if (j == 2)
                            hotel = row.getCell(j).getStringCellValue().trim();
                        if (j == 3)
                            organization = row.getCell(j).getStringCellValue().trim();
                        if (j == 4)
                            reason = row.getCell(j).getStringCellValue().trim();
                        if (j == 5)
                            billing = row.getCell(j).getStringCellValue().trim();
                        if (j == 6)
                            docnum = row.getCell(j).getStringCellValue().trim();
                    } else {
                        if (j == 7)
                            cost = row.getCell(j).getNumericCellValue();
                    }
                }
            }
            Filial filialModel = filialRepository.findByName(filial);
            Hotel hotelModel = hotelRepository.findByNameAndFilial(hotel, filialModel);
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
            contractModel.setYear(2025);
            contractRepository.save(contractModel);
        }
        List<String> response = new ArrayList<>();
        return response;
    }

    @Transactional
    public List<String> loadResponsibilities(MultipartFile file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 100; i++) {
            XSSFRow row = worksheet.getRow(i);
            if (row == null)
                break;
            int filialCode = 0;
            String hotelName = "";
            int tabnum = 0;
            for (int j = 0; j < 5; j++) {
                if (row.getCell(j) != null) {
                    if (row.getCell(j).getCellType() == CellType.STRING) {
                        if (j == 0)
                            filialCode = Integer.parseInt(row.getCell(j).getStringCellValue().trim());
                        if (j == 2)
                            hotelName = row.getCell(j).getStringCellValue().trim();
                    } else {
                        if (j == 4)
                            tabnum = (int) row.getCell(j).getNumericCellValue();
                    }
                }
            }
            Filial filial = filialRepository.findByCode(filialCode);
            Hotel hotel = hotelRepository.findByNameAndFilial(hotelName, filial);
            Employee employee = employeeRepository.findByTabnum(tabnum);
            Responsibilities responsibility = new Responsibilities();
            responsibility.setEmployee(employee);
            responsibility.setHotel(hotel);
            responsibilitiesRepository.save(responsibility);
        }
        List<String> response = new ArrayList<>();
        return response;
    }

    @Transactional
    public List<String> loadUsers(MultipartFile file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 200; i++) {
            XSSFRow row = worksheet.getRow(i);
            if (row == null)
                break;
            int filialCode = 0;
            String hotelName = "";
            int tabnum = 0;
            String roleName = "";
            for (int j = 0; j < 6; j++) {
                if (row.getCell(j) != null) {
                    if (row.getCell(j).getCellType() == CellType.STRING) {
                        if (j == 2)
                            hotelName = row.getCell(j).getStringCellValue().trim();
                        if (j == 5)
                            roleName = row.getCell(j).getStringCellValue().trim();
                    } else {
                        if (j == 0)
                            filialCode = (int) row.getCell(j).getNumericCellValue();
                        if (j == 4)
                            tabnum = (int) row.getCell(j).getNumericCellValue();
                    }
                }
            }

            Filial filial = filialRepository.findByCode(filialCode);
            Hotel hotel = hotelRepository.findByNameAndFilial(hotelName, filial);
            Employee employee = employeeRepository.findByTabnum(tabnum);
            if (employee != null) {
                if (userRepository.findByEmployee(employee) == null) {
                    if (roleName.length() == 6) {
                        Role role = roleRepository.getById(2L);
                        User user = new User();
                        user.setEmployee(employee);
                        user.setRole(role);
                        user.setUsername(findUsernameByTabnum(tabnum));
                        try {
                            userRepository.save(user);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (roleName.length() == 8) {
                        Role role = roleRepository.getById(3L);
                        User user = new User();
                        user.setEmployee(employee);
                        user.setRole(role);
                        user.setUsername(findUsernameByTabnum(tabnum));
                        userRepository.save(user);
                    }
                } else {
                    if (roleName.length() == 6) {
                        Commendant commendant = new Commendant();
                        commendant.setUser(userRepository.findByEmployee(employee));
                        commendant.setHotel(hotel);
                        commendantsRepository.save(commendant);
                    } else if (roleName.length() == 8) {
                        Filial filialTmp = filialRepository.findByCode(employee.getIdFilial());
                        hotelRepository.findAllByFilial(filialTmp).forEach(hotelTmp -> {
                            Commendant commendant = new Commendant();
                            commendant.setHotel(hotelTmp);
                            commendant.setUser(userRepository.findByEmployee(employee));
                            commendantsRepository.save(commendant);
                        });
                    }
                }
            }
        }
        List<String> response = new ArrayList<>();
        return response;
    }

}