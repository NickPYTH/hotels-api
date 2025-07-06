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
import ru.sgp.dto.GuestDTO;
import ru.sgp.dto.integration.AddGuestsForEventDTO;
import ru.sgp.dto.integration.checkSpaces.CheckSpacesDTO;
import ru.sgp.dto.integration.checkSpaces.DatePair;
import ru.sgp.dto.integration.checkSpaces.TabWithItr;
import ru.sgp.dto.integration.checkSpaces.response.CheckSpacesReservation;
import ru.sgp.dto.integration.checkSpaces.response.CheckSpacesResponse;
import ru.sgp.model.*;
import ru.sgp.repository.*;
import ru.sgp.utils.MyMapper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.sgp.spnego.SpnegoHelper.findUsernameByTabnum;

@Service
public class GuestService {
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    ReasonRepository reasonRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private FilialRepository filialRepository;
    @Autowired
    private FlatRepository flatRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private BedRepository bedRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomLocksRepository roomLocksRepository;
    @Autowired
    private FlatLocksRepository flatLocksRepository;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Autowired
    private PostRepository postRepository;

    public List<GuestDTO> update(GuestDTO guestDTO) throws Exception {
        List<GuestDTO> response = new ArrayList<>();
        ModelMapper modelMapper = new ModelMapper();
        Date dateStart = dateTimeFormatter.parse(guestDTO.getDateStart());
        Date dateFinish = dateTimeFormatter.parse(guestDTO.getDateFinish());

        // Определение места в зависимости от того существует оно или это будущее доп. место (свойство isExtra=true)
        Bed bed = null;
        Boolean isExtra = false;
        if (guestDTO.getBed().getId() != null) {
            bed = bedRepository.getById(guestDTO.getBed().getId());
            guestDTO.setBed(MyMapper.BedToBedDTO(bed));
            isExtra = bed.getIsExtra();
        } else {
            bed = new Bed();
            isExtra = true;
        }
        // -----

        Bed oldBed = null;

        // Проверка на создание или обновление карточки жильца
        boolean createRequest = guestDTO.getId() == null;
        Guest guest;
        if (createRequest) guest = new Guest();
        else {
            guest = guestRepository.getById(guestDTO.getId());
            oldBed = guest.getBed();
            // Проверка если жильца переселили на существующее доп. место чего нельзя допускать, кидать исключение сапорты пусть решают
            if (bed.getIsExtra()) {
                if (!Objects.equals(guest.getBed(), bed)) {
                    return response;
                }
            }
        }
        // -----

        // Запоминаем предыдущее состояние, для доп мест пофиг потом сделаю
        if (!isExtra) {
            GuestDTO guestBeforeState;
            if (createRequest) guestBeforeState = new GuestDTO();
                //else guestBeforeState = modelMapper.map(guest, GuestDTO.class); // TODO
            else guestBeforeState = new GuestDTO();
            response.add(guestBeforeState);
        } else response.add(new GuestDTO()); // TODO
        // -----

        // Определяем кто перед нами работник Газпрома или сторонник
        if (guestDTO.getTabnum() != null) {
            Employee employee = employeeRepository.findByTabnumAndEndDate(guestDTO.getTabnum(), null);
            guest.setEmployee(employee);
            Organization gts = organizationRepository.getById(11L); // Организация - ГТС
            guest.setOrganization(gts);
        } else {
            Organization org = organizationRepository.getById(guestDTO.getOrganization().getId());
            guest.setOrganization(org);
            guest.setEmployee(null);
        }
        // -----

        // Заполняем простые поля
        guest.setRegPoMestu(guestDTO.getRegPoMestu());
        guest.setFirstname(guestDTO.getFirstname());
        guest.setLastname(guestDTO.getLastname());
        guest.setSecondName(guestDTO.getSecondName());
        guest.setMale(guestDTO.getMale());
        guest.setMemo(guestDTO.getMemo());
        guest.setNote(guestDTO.getNote());
        guest.setCreditCard(guestDTO.getCreditCard());
        // -----

        if (bed.getRoom() == null) {
            // Значит это доп. место
            Room room = roomRepository.getById(guestDTO.getBed().getRoom().getId());
            Bed extraBed = new Bed();
            extraBed.setIsExtra(true);
            extraBed.setRoom(room);
            extraBed.setName("Extra");
            bedRepository.save(extraBed);
            guest.setBed(extraBed);
            bed = extraBed;
        } else guest.setBed(bed);

        // Если член семьи
        if (guestDTO.getFamilyMemberOfEmployee() != null) {
            Employee employee = employeeRepository.findByTabnumAndEndDate(guestDTO.getFamilyMemberOfEmployee(), null);
            guest.setFamilyMemberOfEmployee(employee);
        } else guest.setFamilyMemberOfEmployee(null);
        // -----

        // Устанавливаем договор
        if (guestDTO.getContract() != null) {
            Contract contract = contractRepository.getById(guestDTO.getContract().getId());
            guest.setContract(contract);
        } else {
            guest.setContract(null);
        }

        // Если это доп. место, то нет смысла проводить проверки
        if (!isExtra) {
            // Проверяем не пересекается ли дата проживания с кем-то на выбранном месте БРОНИ
            List<Reservation> reservations = new ArrayList<>();

            // Проверяем пересечения периодов проживания с самими собой в других филиалах, проверяю по ФИО, чтобы задеть и работников и сторонников, а также чтобы было указано другое место
            if (guest.getEmployee() != null) { // Если ты не работник, то нет смысла проверять с бронями - бронируются только работники !!! TODO
                reservations = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndTabnumAndBedIsNotAndBedRoomFlatHotelFilial(dateFinish, dateStart, guest.getEmployee().getTabnum(), guest.getBed(), guest.getBed().getRoom().getFlat().getHotel().getFilial());
                if (!reservations.isEmpty()) {
                    Reservation reservationTmp = reservations.get(0);
                    GuestDTO tmp = new GuestDTO();
                    tmp.setError("datesError");
                    tmp.setLastname(guest.getEmployee().getLastname());
                    tmp.setFirstname(guest.getEmployee().getFirstname());
                    tmp.setSecondName(guest.getEmployee().getSecondName());
                    tmp.setBed(MyMapper.BedToBedDTO(reservationTmp.getBed()));
                    tmp.setDateStart(dateTimeFormatter.format(reservationTmp.getDateStart()));
                    tmp.setDateFinish(dateTimeFormatter.format(reservationTmp.getDateFinish()));
                    response.add(tmp);
                    guest.setBed(oldBed);
                    return response;
                }
            }
            // -----
            reservations = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, guest.getBed());
            if (!reservations.isEmpty()) {
                Reservation reservationTmp = reservations.get(0);
                GuestDTO tmp = new GuestDTO();
                tmp.setError("datesError");
                tmp.setLastname(guest.getEmployee().getLastname());
                tmp.setFirstname(guest.getEmployee().getFirstname());
                tmp.setSecondName(guest.getEmployee().getSecondName());
                tmp.setBed(MyMapper.BedToBedDTO(reservationTmp.getBed()));
                tmp.setDateStart(dateTimeFormatter.format(reservationTmp.getDateStart()));
                tmp.setDateFinish(dateTimeFormatter.format(reservationTmp.getDateFinish()));
                response.add(tmp);
                guest.setBed(oldBed);
                return response;
            }
            // -----

            // Проверяем не пересекается ли дата проживания с кем-то на выбранном месте ЗАПИСИ О ПРОЖИВАНИИ
            List<Guest> guests = new ArrayList<>();

            // Разрешаю селить несколько раз в один и тот же филиал
            // Проверяем пересечения периодов проживания с самими собой в других филиалах, проверяю по ФИО чтобы задеть и работников и сторонников, а также чтобы было указано другое место
//            guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndFirstnameAndLastnameAndSecondNameAndBedIsNotAndBedRoomFlatHotelFilial(dateFinish, dateStart, guest.getFirstname(), guest.getLastname(), guest.getSecondName(), guest.getBed(), guest.getBed().getRoom().getFlat().getHotel().getFilial());
//            if (!guests.isEmpty()) {
//                Guest guestTmp = guests.get(0);
//                GuestDTO tmp = new GuestDTO();
//                tmp.setError("datesError");
//                tmp.setLastname(guestTmp.getLastname());
//                tmp.setFirstname(guestTmp.getFirstname());
//                tmp.setSecondName(guestTmp.getSecondName());
//                tmp.setBed(MyMapper.BedToBedDTO(guestTmp.getBed()));
//                tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
//                tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
//                response.add(tmp);
//                guest.setBed(oldBed);
//                return response;
//            }
            // -----

// Разрешаю селить несколько раз в один и тот же филиал
//            if (createRequest)
//                guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, guest.getBed());
//            else
//                guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBedAndIdIsNot(dateFinish, dateStart, guest.getBed(), guest.getId());
//
//            if (!guests.isEmpty()) {
//                Guest guestTmp = guests.get(0);
//                GuestDTO tmp = new GuestDTO();
//                tmp.setError("datesError");
//                tmp.setLastname(guestTmp.getLastname());
//                tmp.setFirstname(guestTmp.getFirstname());
//                tmp.setSecondName(guestTmp.getSecondName());
//                tmp.setBed(MyMapper.BedToBedDTO(guestTmp.getBed()));
//                tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
//                tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
//                response.add(tmp);
//                guest.setBed(oldBed);
//                return response;
//            } else {
//                guest.setDateStart(dateStart);
//                guest.setDateFinish(dateFinish);
//            }

            guest.setDateStart(dateStart);
            guest.setDateFinish(dateFinish);

            // -----


            // Проверяем не пытаемся ли заселить в секцию/комнату со статусом закрыто/выкуплено организацией
            if (oldBed != null && !bed.getRoom().equals(oldBed.getRoom())) {
                List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(dateFinish, dateStart, bed.getRoom());
                if (!roomLocksList.isEmpty()) {
                    GuestDTO tmp = new GuestDTO();
                    if (roomLocksList.get(0).getStatus().getId() == 3L) tmp.setError("roomOrg");
                    else tmp.setError("roomLock");
                    response.add(tmp);
                    guest.setBed(oldBed);
                    return response;
                }
            } else if (oldBed == null) {
                List<RoomLocks> roomLocksList = roomLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndRoom(dateFinish, dateStart, bed.getRoom());
                if (!roomLocksList.isEmpty()) {
                    GuestDTO tmp = new GuestDTO();
                    if (roomLocksList.get(0).getStatus().getId() == 3L) tmp.setError("roomOrg");
                    else tmp.setError("roomLock");
                    response.add(tmp);
                    guest.setBed(oldBed);
                    return response;
                }
            }
            if (oldBed != null && !bed.getRoom().getFlat().equals(oldBed.getRoom().getFlat())) {
                List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(dateFinish, dateStart, bed.getRoom().getFlat());
                if (!flatLocksList.isEmpty()) {
                    GuestDTO tmp = new GuestDTO();
                    if (flatLocksList.get(0).getStatus().getId() == 4L) tmp.setError("flatOrg");
                    else tmp.setError("flatLock");
                    response.add(tmp);
                    guest.setBed(oldBed);
                    return response;
                }
            } else if (oldBed == null) {
                List<FlatLocks> flatLocksList = flatLocksRepository.findAllByDateStartBeforeAndDateFinishAfterAndFlat(dateFinish, dateStart, bed.getRoom().getFlat());
                if (!flatLocksList.isEmpty()) {
                    GuestDTO tmp = new GuestDTO();
                    if (flatLocksList.get(0).getStatus().getId() == 4L) tmp.setError("flatOrg");
                    else tmp.setError("flatLock");
                    response.add(tmp);
                    guest.setBed(oldBed);
                    return response;
                }
            }
            // -----
        } else {
            guest.setDateStart(dateStart);
            guest.setDateFinish(dateFinish);
        }
        // -----

        guestRepository.save(guest); // Сохраняем
        guestDTO.setId(guest.getId());
        response.add(guestDTO); // Добавляем обновленную сущность для логов
        return response;
    }

    @Transactional
    public List<GuestDTO> getAll() {
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        List<GuestDTO> response = new ArrayList<>();
        List<Guest> guests;
        // Если роль Администратор или Работник ОСР, отдаем все записи
        if (user.getRole().getId() == 1L || user.getRole().getId() == 5L) {
            guests = guestRepository.findAll();
        } else {
            // Если дежурный или работник филиала, то отдаем записи по филиалу работника/дежурного по скольку их место работы 99% совпадает с филиалом общежития
            Filial filial = filialRepository.findByCode(user.getEmployee().getIdFilial());
            guests = guestRepository.findAllByBedRoomFlatHotelFilial(filial);
        }
        for (Guest guest : guests)
            response.add(MyMapper.GuestToGuestDTO(guest));
        return response;
    }

    @Transactional
    public GuestDTO delete(Long id) {
        Guest guest = guestRepository.getById(id);
        Bed bed = guest.getBed();

        // Если есть бронь связанная с записью о проживании - чистим связь
        Optional<Reservation> reservationOptional = reservationRepository.findByGuest(guest);
        if (reservationOptional.isPresent()) {
            Reservation reservation = reservationOptional.get();
            reservation.setGuest(null);

            // Нет записи о проживании -> нет дат подтверждающих бронь. Переносим даты подтверждения в даты брони, без них бронь не будет выводится
            reservation.setDateStart(reservation.getDateStartConfirmed());
            reservation.setDateFinish(reservation.getDateFinishConfirmed());
            // -----

            reservationRepository.save(reservation);
        }
        // -----

        guestRepository.deleteById(id);

        // Если место на котором живет гость дополнительное - удаляем его тоже
        if (bed.getIsExtra()) {
            //bedRepository.deleteById(bed.getId());
        }
        // -----

        return new GuestDTO();
    }

    @Transactional
    public GuestDTO checkout(Long id) {
        Optional<Guest> guestOpt = guestRepository.findById(id);
        if (guestOpt.isPresent()) {
            guestOpt.get().setCheckouted(true);
            guestRepository.save(guestOpt.get());
        }
        return new GuestDTO();
    }

    public GuestDTO getFioByTabnum(Integer tabnum) {
        Employee employee = employeeRepository.findByTabnumAndEndDate(tabnum, null);
        GuestDTO guestDTO = new GuestDTO();
        if (employee != null) {
            guestDTO.setFirstname(employee.getFirstname());
            guestDTO.setLastname(employee.getLastname());
            guestDTO.setSecondName(employee.getSecondName());
            guestDTO.setEmail(findUsernameByTabnum(tabnum));
            guestDTO.setMale(employee.getMale() == 1);
        }
        return guestDTO;
    }

    public GuestDTO getTabnumByFio(String lastname, String firstname, String secondName) {
        List<Employee> employees = employeeRepository.findAllByLastnameAndFirstnameAndSecondNameAndEndDate(lastname, firstname, secondName, null);
        GuestDTO guestDTO = new GuestDTO();
        if (!employees.isEmpty()) {
            guestDTO.setFirstname(employees.get(0).getFirstname());
            guestDTO.setLastname(employees.get(0).getLastname());
            guestDTO.setSecondName(employees.get(0).getSecondName());
            guestDTO.setTabnum(employees.get(0).getTabnum());
            guestDTO.setMale(employees.get(0).getMale() == 1);
        }
        return guestDTO;
    }

    public List<GuestDTO> getAllByOrganizationId(Long id) {
        ModelMapper modelMapper = new ModelMapper();
        return guestRepository.findAllByOrganization(organizationRepository.getById(id)).stream().map(guest -> modelMapper.map(guest, GuestDTO.class)).collect(Collectors.toList());
    }

    public List<String> getGuestsLastnames() {
        return guestRepository.findDistinctLastname();
    }

    @Transactional
    public List<GuestDTO> addGuestsForEvent(AddGuestsForEventDTO data) throws Exception {
        ModelMapper modelMapper = new ModelMapper();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        Optional<Hotel> hotelOpt = hotelRepository.findById(data.getHotelId());
        if (hotelOpt.isEmpty()) throw new Exception("Hotel with id " + data.getHotelId() + " not found");
        Hotel hotel = hotelOpt.get();
        Date dateStart = dateFormatter.parse(data.getDateStart());
        Date dateFinish = dateFormatter.parse(data.getDateEnd());
        List<GuestDTO> response = new ArrayList<>();
        List<Employee> empListOrderBySex = new ArrayList<>();
        for (Integer tabNumber : data.getTabNumbers()) {
            Employee employee = employeeRepository.findByTabnumAndEndDate(tabNumber, null);
            if (employee == null)
                throw new Exception("Employee with " + tabNumber + " not found");
            empListOrderBySex.add(employee);
        }
        empListOrderBySex.sort(((e1, e2) -> e1.getMale() < e2.getMale() ? 1 : -1));
        List<Guest> guests = new ArrayList<>();
        for (Employee employee : empListOrderBySex) {
            for (Bed bed : bedRepository.findAllByRoomFlatHotelAndIsExtra(hotel, false)) {
                Flat flat = bed.getRoom().getFlat();
                if (guests.stream().anyMatch((guest) -> guest.getBed().equals(bed))) continue;
                if (employee.getMale() == 1 && guests.stream().anyMatch((guest) -> guest.getBed().getRoom().getFlat().getId() == flat.getId() && !guest.getMale())) // Если работник мужчина, то проверяем чтобы в комнате не было женщин
                    continue;
                List<Guest> livingGuestsFlat = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBedRoomFlat(dateFinish, dateStart, bed.getRoom().getFlat());
                if (employee.getMale() == 2 && livingGuestsFlat.stream().anyMatch((guest) -> guest.getMale()))
                    continue;
                List<Guest> livingGuestsBed = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndBed(dateFinish, dateStart, bed);
                if (livingGuestsBed.isEmpty()) { // Проверяем что бы тут никто не жил
                    // Проверяем наличие мужчин в комнате если селим женщину
                    Guest guest = new Guest();
                    guest.setEmployee(employee);
                    guest.setOrganization(organizationRepository.getById(11L));
                    guest.setRegPoMestu(false);
                    guest.setFirstname(employee.getFirstname());
                    guest.setLastname(employee.getLastname());
                    guest.setSecondName(employee.getSecondName());
                    guest.setDateStart(dateStart);
                    guest.setDateFinish(dateFinish);
                    guest.setBed(bed);
                    guest.setCheckouted(false);
                    guest.setMale(employee.getMale() == 1);
                    // Пока будут константами уточнить
                    guest.setContract(contractRepository.getById(865L));
                    guest.setMemo(data.getEventName());
                    if (guest.getContract() != null) {
                        guest.getContract().setBilling(contractRepository.getById(865L).getBilling());
                        guest.getContract().setReason(reasonRepository.getById(4L));
                    }
                    // -----
                    guests.add(guest);
                    break;
                }
            }
        }
        if (guests.size() != data.getTabNumbers().size()) throw new Exception("Space error");
        for (Guest guest : guests) {
            guestRepository.save(guest);
            response.add(modelMapper.map(guest, GuestDTO.class));
        }
        return response;
    }

    @Transactional
    public List<String> loadGuestsFrom1C(MultipartFile file) throws IOException, ParseException {
        SimpleDateFormat dateTimeFormatterDotes = new SimpleDateFormat("dd.MM####yyyy HH:mm");
        Hotel hotel = hotelRepository.getById(182L);
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        List<Guest> test = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            XSSFRow row = worksheet.getRow(i);
            if (row == null)
                break;

            String guestStr = row.getCell(0).getStringCellValue();
            String dateStartStr = row.getCell(1).getStringCellValue();
            Date dateStart = dateTimeFormatterDotes.parse(dateStartStr);
            String dateFinishStr = row.getCell(2).getStringCellValue();
            Date dateFinish = dateTimeFormatterDotes.parse(dateFinishStr);
            Double flatNameD = row.getCell(3).getNumericCellValue();
            String flatName = String.valueOf(flatNameD.intValue());
            Flat flat = flatRepository.findByNameAndHotel(flatName, hotel);
            //Contract contract = contractRepository.getById(1240L); // for Ermak
            Contract contract = contractRepository.getById(1241L); // for Obssch 100 mest
            for (Bed bed : bedRepository.findAllByRoomFlatAndIsExtra(flat, false)) {
                List<Guest> guests = guestRepository.findAllByBed(bed);
                boolean guestExists = false;
                for (Guest guest : guests) {
                    if ((dateStart.before(guest.getDateStart()) && dateFinish.before(guest.getDateStart())) || // Левый диапазон
                            dateStart.after(guest.getDateFinish()) && dateFinish.after(guest.getDateFinish())) { // Правый диапазон

                    } else {
                        guestExists = true;
                        break;
                    }
                }
                if (!guestExists) { // Тогда селим и break
                    Guest guest = new Guest();
                    guest.setFirstname("");
                    guest.setLastname(guestStr);
                    guest.setSecondName("");
                    guest.setMale(true);
                    guest.setNote("1C int " + guestStr);
                    guest.setDateStart(dateStart);
                    guest.setDateFinish(dateFinish);
                    guest.setEmployee(null);
                    guest.setOrganization(organizationRepository.getById(11L));
                    guest.setRegPoMestu(false);
                    guest.setMemo("-");
                    guest.setContract(contract);
                    guest.setCheckouted(false);
                    guest.setBed(bed);
                    guestRepository.save(guest);
                    test.add(guest);
                    break;
                }
            }


        }
        return new ArrayList<>();
    }

    public List<GuestDTO> manyGuestUpload(MultipartFile file, Boolean mode) throws IOException {
        List<GuestDTO> guests = new ArrayList<>();
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);
        for (int i = 1; i <= 1000; i++) {
            XSSFRow row = worksheet.getRow(i);
            if (row == null)
                break;
            if (mode) {
                Employee employee = null;
                if (row.getCell(0).getCellType() == CellType.STRING) {
                    Integer tabnum = Integer.valueOf(row.getCell(0).getStringCellValue());
                    employee = employeeRepository.findByTabnumAndEndDate(tabnum, null);
                } else {
                    Integer tabnum = (int) row.getCell(0).getNumericCellValue();
                    employee = employeeRepository.findByTabnumAndEndDate(tabnum, null);
                }
                if (employee != null) {
                    GuestDTO guestDTO = new GuestDTO();
                    guestDTO.setLastname(employee.getLastname());
                    guestDTO.setFirstname(employee.getFirstname());
                    guestDTO.setSecondName(employee.getSecondName());
                    guestDTO.setTabnum(employee.getTabnum());
                    guests.add(guestDTO);
                }
            } else {
                String lastname = row.getCell(0).getStringCellValue().trim();
                String firstname = row.getCell(1).getStringCellValue().trim();
                String secondName = row.getCell(2).getStringCellValue().trim();
                List<Employee> employees = employeeRepository.findAllByLastnameAndFirstnameAndSecondNameAndEndDate(lastname, firstname, secondName, null);
                if (!employees.isEmpty()) {
                    GuestDTO guestDTO = new GuestDTO();
                    guestDTO.setLastname(lastname);
                    guestDTO.setFirstname(firstname);
                    guestDTO.setSecondName(secondName);
                    guestDTO.setTabnum(employees.get(0).getTabnum());
                    guests.add(guestDTO);
                }
            }
        }
        return guests;
    }

    @Transactional
    public CheckSpacesResponse checkSpaces(CheckSpacesDTO data) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        CheckSpacesResponse response = new CheckSpacesResponse();
        response.setGuests(new ArrayList<>());

        Hotel ermak = hotelRepository.getById(327L);
        Hotel proizvodstvennaya = hotelRepository.getById(182L);

        // Разбиваем гостей по признаку ИТР
        List<TabWithItr> employeeItr = new ArrayList<>();
        List<TabWithItr> employeeNoItr = new ArrayList<>();
        for (TabWithItr tabWithItr : data.getGuests()) {
            if (tabWithItr.getIsItr()) employeeItr.add(tabWithItr);
            else employeeNoItr.add(tabWithItr);
        }
        // -----

        // Сортируем оба списка гостей по полу, сначла руководитель, потом Ж
        employeeItr.sort((o1, o2) -> {
            boolean o1isBoss = isLeadershipPosition(o1.getTabNumber());
            boolean o2isBoss = isLeadershipPosition(o2.getTabNumber());
            int leadershipCompare = Boolean.compare(o2isBoss, o1isBoss);
            if (leadershipCompare != 0) {
                return leadershipCompare;
            }
            if (!o1isBoss && !o2isBoss) {
                return Integer.compare(
                        o1.getMale() == 1 ? 1 : 0,
                        o2.getMale() == 1 ? 1 : 0
                );
            }
            return 0;
        });
        employeeNoItr.sort((e1, e2) -> e2.getMale().compareTo(e1.getMale()));
        // -----

        List<Bed> ermakBeds = bedRepository.findAllByRoomFlatHotelAndIsExtra(ermak, false);
        List<Bed> proizvodstvennayaBeds = bedRepository.findAllByRoomFlatHotelAndIsExtra(proizvodstvennaya, false);
        List<Reservation> tmpReservationsErmak = new ArrayList<>();
        List<Reservation> tmpReservationsProizvod = new ArrayList<>();

        // Проверка возможности расселения гостей со статусом ИТР в гостинницу Ермак
        for (TabWithItr employee : employeeItr) {
            for (DatePair datePair : employee.getDates()) {
                // Определяем дату начала и дату окончания
                Date dateStart = dateFormat.parse(datePair.getDate());
                LocalDateTime dateStartLD = dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime dateFinishLD = dateStartLD.plusDays(datePair.getDaysCount() - 1);
                Date dateFinish = dateFormat.parse(dateFinishLD.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                String df = dateFormat.format(dateFinish) + " 23:59";
                dateFinish = dateTimeFormatter.parse(df);
                // -----
                if (!tryCheckIn(dateStart, dateFinish, employee, ermakBeds, tmpReservationsErmak)) {
                    CheckSpacesReservation record = new CheckSpacesReservation();
                    record.setTabNumber(employee.getTabNumber());
                    record.setDateStart(dateFormat.format(dateStart));
                    record.setDateFinish(dateFormat.format(dateFinish));
                    record.setRange(dateFormat.format(dateStart) + " - " + dateFormat.format(dateFinish));
                    record.setStatus("error");
                    record.setErrorCode(0);
                    record.setHotel("ermak");
                    response.getGuests().add(record);
                } else {
                    CheckSpacesReservation record = new CheckSpacesReservation();
                    record.setTabNumber(employee.getTabNumber());
                    record.setDateStart(dateFormat.format(dateStart));
                    record.setDateFinish(dateFormat.format(dateFinish));
                    record.setDaysCount(datePair.getDaysCount());
                    record.setRange(dateFormat.format(dateStart) + " - " + dateFormat.format(dateFinish));
                    record.setStatus("ok");
                    record.setErrorCode(null);
                    record.setHotel("ermak");
                    response.getGuests().add(record);
                }
            }
        }
        // -----

        // Проверка возможности расселения гостей без статуса ИТР в Общежитие на Производственной
        for (TabWithItr employee : employeeNoItr) {
            for (DatePair datePair : employee.getDates()) {
                // Определяем дату начала и дату окончания
                Date dateStart = dateFormat.parse(datePair.getDate());
                LocalDateTime dateStartLD = dateStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime dateFinishLD = dateStartLD.plusDays(datePair.getDaysCount() - 1);
                Date dateFinish = dateFormat.parse(dateFinishLD.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                String df = dateFormat.format(dateFinish) + " 23:59";
                dateFinish = dateTimeFormatter.parse(df);
                // -----
                if (!tryCheckIn(dateStart, dateFinish, employee, proizvodstvennayaBeds, tmpReservationsProizvod)) {
                    CheckSpacesReservation record = new CheckSpacesReservation();
                    record.setTabNumber(employee.getTabNumber());
                    record.setDateStart(dateFormat.format(dateStart));
                    record.setDateFinish(dateFormat.format(dateFinish));
                    record.setRange(dateFormat.format(dateStart) + " - " + dateFormat.format(dateFinish));
                    record.setStatus("error");
                    record.setErrorCode(0);
                    record.setHotel("proizvod");
                    response.getGuests().add(record);
                } else {
                    CheckSpacesReservation record = new CheckSpacesReservation();
                    record.setTabNumber(employee.getTabNumber());
                    record.setDateStart(dateFormat.format(dateStart));
                    record.setDateFinish(dateFormat.format(dateFinish));
                    record.setDaysCount(datePair.getDaysCount());
                    record.setRange(dateFormat.format(dateStart) + " - " + dateFormat.format(dateFinish));
                    record.setStatus("ok");
                    record.setErrorCode(null);
                    record.setHotel("proizvod");
                    response.getGuests().add(record);
                }
            }
        }
        // -----

        // Очистка временных записей
        //for (Reservation r : tmpReservationsErmak) reservationRepository.delete(r);
        for (Reservation r : tmpReservationsProizvod) reservationRepository.delete(r);
        // -----

        return response;
    }

    public boolean isLeadershipPosition(Integer tabNumber) {
        if (tabNumber == null) return false;
        Employee employee = employeeRepository.findByTabnumAndEndDate(tabNumber, null);
        Post post = postRepository.getById(Long.valueOf(employee.getIdPoststaff()));
        if (Objects.equals(post.getPersk(), "10")) return true;
        if (Objects.equals(post.getPersk(), "11")) return true;
        if (Objects.equals(post.getPersk(), "12")) return true;
        if (Objects.equals(post.getPersk(), "13")) return true;
        if (Objects.equals(post.getPersk(), "14")) return true;
        return false;
    }

    // Метод для проверки возможности заселения гостя в перечень переданных мест
    boolean tryCheckIn(Date dateStart, Date dateFinish, TabWithItr employee, List<Bed> beds, List<Reservation> tmpReservations) {
        for (Bed bed : beds) {
            // Проверяем свободно ли место от броней или гостей на эти даты
            boolean bedBusyReservation = reservationRepository.existsByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed);
            boolean bedBusyGuest = guestRepository.existsByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bed);
            if (bedBusyReservation || bedBusyGuest) continue;
            // -----

            // Если селим руководителя, то смотрим, чтобы в секции никто не жил
            if (isLeadershipPosition(employee.getTabNumber())) {
                boolean neighborhoodExist = false;
                Flat flat = bed.getRoom().getFlat();
                for (Room room: roomRepository.findAllByFlatOrderById(flat)) {
                    for (Bed bedRoom : bedRepository.findAllByRoom(room)) {
                        List<Reservation> reservationNeighborhoods = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bedRoom);
                        List<Guest> guestNeighborhoods = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bedRoom);
                        if (!reservationNeighborhoods.isEmpty() || !guestNeighborhoods.isEmpty()) neighborhoodExist = true;
                    }
                }
                if (neighborhoodExist) continue;
            } else { // Иначе смотрим, чтобы не было соседа руководителя
                boolean bossNeighborhoodExist = false;
                Flat flat = bed.getRoom().getFlat();
                for (Room room: roomRepository.findAllByFlatOrderById(flat)) {
                    for (Bed bedRoom : bedRepository.findAllByRoom(room)) {
                        List<Reservation> reservationNeighborhoods = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bedRoom);
                        List<Guest> guestNeighborhoods = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bedRoom);
                        if (!reservationNeighborhoods.isEmpty()) {
                            if (isLeadershipPosition(reservationNeighborhoods.get(0).getTabnum()))
                                bossNeighborhoodExist = true;
                        }
                        if (!guestNeighborhoods.isEmpty()) {
                            if (guestNeighborhoods.get(0).getEmployee() != null) { // Если наш работник, то есть смысл провреки на руководителя
                                if (isLeadershipPosition(guestNeighborhoods.get(0).getEmployee().getTabnum()))
                                    bossNeighborhoodExist = true;
                            }
                        }
                    }
                }
                if (bossNeighborhoodExist) continue;
            }
            // -----

            // Если селим женщину то, ищем соседа мужчину, если есть скипаем место
            if (employee.getMale() == 0) {
                boolean maleNeighborhoodExist = false;
                Room room = bed.getRoom();
                for (Bed bedRoom : bedRepository.findAllByRoom(room)) {
                    List<Reservation> reservationNeighborhoods = reservationRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bedRoom);
                    List<Guest> guestNeighborhoods = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, bedRoom);
                    if (!guestNeighborhoods.isEmpty()) {
                        if (guestNeighborhoods.get(0).getMale()) maleNeighborhoodExist = true;
                    }
                    if (!reservationNeighborhoods.isEmpty()) {
                        if (reservationNeighborhoods.get(0).getMale()) maleNeighborhoodExist = true;
                    }
                }
                if (maleNeighborhoodExist) continue;
            }
            // -----

            // Создаем временную бронь и добавляем в список веремнных броней, после они будут удалены
            Reservation reservation = new Reservation();
            reservation.setTabnum(employee.getTabNumber());
            reservation.setDateStart(dateStart);
            reservation.setDateFinish(dateFinish);
            reservation.setBed(bed);
            reservation.setMale(employee.getMale() == 1);
            reservation.setNote("RESERVATION CHECK! NEED TO DELETE if U SEE THIS RECORD!");
            reservationRepository.save(reservation);
            reservation.setFirstname(reservation.getId().toString());
            reservation.setLastname(reservation.getId().toString());
            reservation.setSecondName(reservation.getId().toString());
            reservationRepository.save(reservation);
            tmpReservations.add(reservation);
            return true;
            // -----
        }
        return false;
    }

}