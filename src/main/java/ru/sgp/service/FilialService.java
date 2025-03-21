package ru.sgp.service;

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
import ru.sgp.dto.report.ShortReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    ContractRepository contractRepository;
    @Autowired
    ReasonRepository reasonRepository;
    @Autowired
    BedRepository bedRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    ResponsibilitiesRepository responsibilitiesRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CommendantsRepository commendantsRepository;
    @Autowired
    RoomLocksRepository roomLocksRepository;
    @Autowired
    FlatLocksRepository flatLocksRepository;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Transactional
    public List<FilialDTO> getAll() throws ParseException {
        ModelMapper modelMapper = new ModelMapper();
        return filialRepository.findAllByOrderByNameAsc().stream().map(filial -> modelMapper.map(filial, FilialDTO.class)).collect(Collectors.toList());
    }
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
                    List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, flat);
                    for (Room room : roomRepository.findAllByFlatOrderById(flat)) {
                        bedsCount += room.getBedsCount();
                        emptyBedsCountWithBusy += room.getBedsCount() - guestRepository.findAllByBedRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).size();
                        List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, room);
                        if (flatLocksList.isEmpty() && roomLocksList.isEmpty()) {
                            emptyBedsCount += room.getBedsCount() - guestRepository.findAllByBedRoomAndCheckoutedAndDateStartLessThanEqualAndDateFinishGreaterThan(room, false, date, date).size();
                        }
                    }
                }
            }
            filialDTO.setBedsCount(bedsCount);
            filialDTO.setEmptyBedsCount(emptyBedsCount);
            filialDTO.setEmptyBedsCountWithBusy(emptyBedsCountWithBusy);
            filialDTO.setHotels(hotelDTOList);
            filialDTO.setExcluded(filial.getExcluded());
            response.add(filialDTO);
        }
        return response;
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
                if (row.getCell(j) != null)
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
                continue;
            }
            Hotel hotelModel = hotelRepository.findByNameAndFilial(hotel.trim(), filialModel);


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
            String note = "";
            for (int j = 0; j < 11; j++) {
                if (row.getCell(j) != null) {
                    if (row.getCell(j).getCellType() == CellType.STRING) {
                        if (j == 1)
                            filial = row.getCell(j).getStringCellValue().trim();
                        if (j == 2)
                            hotel = row.getCell(j).getStringCellValue().trim();
                        if (j == 3)
                            organization = row.getCell(j).getStringCellValue().trim();
                        if (j == 6)
                            reason = row.getCell(j).getStringCellValue().trim();
                        if (j == 7)
                            billing = row.getCell(j).getStringCellValue().trim();
                        if (j == 4)
                            docnum = row.getCell(j).getStringCellValue().trim();
                        if (j == 8)
                            note = row.getCell(j).getStringCellValue().trim();
                    } else {
                        if (j == 5)
                            cost = row.getCell(j).getNumericCellValue();
                    }
                }
            }
            Filial filialModel = filialRepository.findByName(filial);
            if (filialModel == null) break;
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
            contractModel.setNote(note);
            contractRepository.save(contractModel);
        }
        List<String> response = new ArrayList<>();
        return response;
    }
    @Transactional
    public List<String> loadResponsibilities(MultipartFile file) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 600; i++) {
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
    @Transactional
    public FilialDTO getWithStats(String dateStr, Long filialId) throws ParseException {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date date = dateTimeFormatter.parse(dateStr);
        Filial filial = filialRepository.getById(filialId);
        FilialDTO filialDTO = new FilialDTO();
        filialDTO.setId(filial.getId());
        filialDTO.setName(filial.getName());
        List<HotelDTO> hotelDTOList = new ArrayList<>();
        int bedsCountFilial = 0;
        int emptyBedsCountFilial = 0;
        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
            HotelDTO hotelDTO = new HotelDTO();
            hotelDTO.setId(hotel.getId());
            hotelDTO.setName(hotel.getName());
            hotelDTO.setFilialId(filial.getId());
            hotelDTOList.add(hotelDTO);
        }

        for (Hotel hotel : hotelRepository.findAllByFilial(filial)) {
            ShortReportDTO record = new ShortReportDTO();
            record.setFilial(filial.getName());
            record.setHotel(hotel.getName());
            AtomicReference<Integer> bedsCount = new AtomicReference<>(0);
            flatRepository.findAllByHotelOrderById(hotel).forEach(flat -> {
                if (!flat.getTech())
                    roomRepository.findAllByFlatOrderById(flat).forEach(room -> {
                        bedsCount.updateAndGet(v -> v + room.getBedsCount());
                    });
            });
            record.setAll(bedsCount.get());
            Integer countBusyBeds = 0;
            List<Long> flatsExcludeList = new ArrayList<>();
            List<Long> roomExcludeList = new ArrayList<>();
            for (Guest guest : guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlatHotel(date, date, hotel)) {
                if (roomExcludeList.contains(guest.getBed().getRoom().getId())) continue;
                if (flatsExcludeList.contains(guest.getBed().getRoom().getFlat().getId())) continue;
                Hotel guestHotel = guest.getBed().getRoom().getFlat().getHotel();
                if (Objects.equals(guestHotel.getId(), hotel.getId())) {
                    Room guestRoom = guest.getBed().getRoom();
                    Flat guestFlat = guestRoom.getFlat();
                    List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(date, date, guestRoom);
                    List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(date, date, guestFlat);
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
            if (record.getAll() > 0) {
                record.setBusy(countBusyBeds);
                bedsCountFilial += record.getAll();
                emptyBedsCountFilial += record.getAll() - record.getBusy();
            }
        }


        filialDTO.setBedsCount(bedsCountFilial);
        filialDTO.setEmptyBedsCount(emptyBedsCountFilial);
        //filialDTO.setEmptyBedsCountWithBusy(emptyBedsCountWithBusy);
        filialDTO.setHotels(hotelDTOList);
        filialDTO.setExcluded(filial.getExcluded());
        return filialDTO;
    }
}