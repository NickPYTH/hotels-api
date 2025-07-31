package ru.sgp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import ru.sgp.dto.ReservationDTO;
import ru.sgp.dto.integration.AddGuestsForEventDTO;
import ru.sgp.dto.integration.cancelReservation.CancelReservationRequest;
import ru.sgp.dto.integration.cancelReservation.CancelReservationResponse;
import ru.sgp.dto.integration.cancelReservation.TabAndReservation;
import ru.sgp.dto.integration.checkEmployee.CheckEmployeeRequest;
import ru.sgp.dto.integration.checkEmployee.CheckEmployeeResponse;
import ru.sgp.dto.integration.checkSpaces.CheckSpacesDTO;
import ru.sgp.dto.integration.checkSpaces.DatePair;
import ru.sgp.dto.integration.checkSpaces.TabWithItr;
import ru.sgp.dto.integration.checkSpaces.response.CheckSpacesReservation;
import ru.sgp.dto.integration.checkSpaces.response.CheckSpacesResponse;
import ru.sgp.model.*;
import ru.sgp.repository.*;
import ru.sgp.utils.MyMapper;
import ru.sgp.utils.SecurityManager;

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
    @Autowired
    private BookReportRepository bookReportRepository;
    @Autowired
    private RecordBookReportRepository recordBookReportRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private EventKindRepository eventKindRepository;
    @Autowired
    private HistoryService historyService;


    public List<GuestDTO> update(GuestDTO guestDTO) throws Exception {
        List<GuestDTO> response = new ArrayList<>();
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
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
            else guestBeforeState = MyMapper.GuestToGuestDTO(guest);
            response.add(guestBeforeState);
        } else response.add(new GuestDTO());
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

            // Проверяем не пересекается ли дата проживания с кем-то на выбранном месте ЗАПИСИ О ПРОЖИВАНИИ TODO
            //List<Guest> guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBedAndIdIsNot(dateFinish, dateStart, guest.getBed(), guest.getId());

            // Разрешаю селить несколько раз в один и тот же филиал
            // Проверяем пересечения периодов проживания с самими собой в других филиалах, проверяю по ФИО чтобы задеть и работников и сторонников, а также чтобы было указано другое место
            //guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndFirstnameAndLastnameAndSecondNameAndBedIsNotAndBedRoomFlatHotelFilial(dateFinish, dateStart, guest.getFirstname(), guest.getLastname(), guest.getSecondName(), guest.getBed(), guest.getBed().getRoom().getFlat().getHotel().getFilial());
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
            List<Guest> guests;
            if (createRequest)
                guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, guest.getBed());
            else
                guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndBedAndIdIsNot(dateFinish, dateStart, guest.getBed(), guest.getId());

            if (!guests.isEmpty()) {
                Guest guestTmp = guests.get(0);
                GuestDTO tmp = new GuestDTO();
                tmp.setError("datesError");
                tmp.setLastname(guestTmp.getLastname());
                tmp.setFirstname(guestTmp.getFirstname());
                tmp.setSecondName(guestTmp.getSecondName());
                tmp.setBed(MyMapper.BedToBedDTO(guestTmp.getBed()));
                tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
                tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
                response.add(tmp);
                guest.setBed(oldBed);
                return response;
            } else {
                guest.setDateStart(dateStart);
                guest.setDateFinish(dateFinish);
            }

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
    public CheckSpacesResponse checkSpaces(CheckSpacesDTO data, Boolean needBook) throws ParseException, JsonProcessingException {
        long startTime = System.nanoTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        CheckSpacesResponse response = new CheckSpacesResponse();
        response.setGuests(new ArrayList<>());

        // 1. Загружаем ВСЕ данные один раз (брони, гостей, кровати)
        List<Reservation> allReservations = reservationRepository.findAllByDateFinishGreaterThanEqual(dateFormat.parse("31-12-2025"));
        List<Reservation> needToBook = new ArrayList<>();
        List<Guest> allGuests = guestRepository.findAllByDateFinishGreaterThanEqual(dateFormat.parse("31-12-2025"));
        //Берем все места в ермаке кроме Бильярда, Акт зала, 305, 201, 205 и кроме Extra
        List<Bed> ermakBeds = bedRepository.findAllByRoomFlatHotelId(327L).stream().filter(bed -> bed.getRoom().getFlat().getId() != 5070 // акт
                && bed.getRoom().getFlat().getId() != 5071 // бильярд
                && bed.getRoom().getFlat().getId() != 5061 // 305
                && bed.getRoom().getFlat().getId() != 5044 // 201
                && bed.getRoom().getFlat().getId() != 5048 // 205
                && !bed.getName().equals("Extra")
        ).collect(Collectors.toList()
        );
        List<Bed> proizvodBeds = bedRepository.findAllByRoomFlatHotelId(182L).stream().filter(bed -> !bed.getName().equals("Extra")).collect(Collectors.toList());

        // 2. Разделяем гостей на ИТР и не-ИТР
        Map<Boolean, List<TabWithItr>> employeesByItr = data.getGuests().stream()
                .collect(Collectors.partitioningBy(TabWithItr::getIsItr));

        // 3. Проверяем заселение для ИТР (Ермак)
        processEmployeesInMemory(
                employeesByItr.get(true),
                ermakBeds,
                allReservations,
                needToBook,
                allGuests,
                "ermak",
                response,
                dateFormat,
                proizvodBeds
        );

        // 4. Проверяем заселение для не-ИТР (Производственная)
        processEmployeesInMemory(
                employeesByItr.get(false),
                proizvodBeds,
                allReservations,
                needToBook,
                allGuests,
                "proizvod",
                response,
                dateFormat,
                proizvodBeds
        );
        Double duration = (System.nanoTime() - startTime) / 1E9;

        // Логируем запрос в отдельный протокол
        BookReport bookReport = new BookReport();
        bookReport.setDate(new Date());
        if (data.getAuthor() != null) bookReport.setUsername(data.getAuthor());
        else bookReport.setUsername(SecurityManager.getCurrentUser());
        bookReport.setDuration(duration.floatValue());
        bookReport.setWithBook(needBook);
        bookReportRepository.save(bookReport);
        boolean isError = false;
        for (Reservation reservation : needToBook) {
            RecordBookReport recordBookReport = new RecordBookReport();
            recordBookReport.setBookReport(bookReport);
            recordBookReport.setTabnumber(reservation.getTabnum());
            recordBookReport.setFio("");
            recordBookReport.setDateStart(reservation.getDateStart());
            recordBookReport.setDateFinish(reservation.getDateFinish());
            recordBookReport.setStatus(reservation.getBed() == null ? "error" : "ok");
            recordBookReportRepository.save(recordBookReport);
            if (reservation.getBed() == null) isError = true;
            reservation.setBookReportId(bookReport.getId());
        }
        bookReport.setStatus(isError ? "error" : "ok");
        // -----

        // Если передан параметр о бронировании
        if (needBook) {
            CheckSpacesResponse bookedResponse = new CheckSpacesResponse();
            List<CheckSpacesReservation> bookedGuest = new ArrayList<>();
            for (Reservation reservation : needToBook) {
                if (reservation.getBed() == null) {
                    CheckSpacesReservation book = new CheckSpacesReservation();
                    book.setStatus("error");
                    book.setTabNumber(reservation.getTabnum());
                    book.setDateStart(dateFormat.format(reservation.getDateStart()));
                    book.setDateFinish(dateFormat.format(reservation.getDateFinish()));
                    book.setBookReportId(reservation.getBookReportId());
                    bookedGuest.add(book);
                } else {
                    reservationRepository.save(reservation);
                    historyService.updateReservation(null, new ReservationDTO(), MyMapper.ReservationToReservationDTO(reservation));
                    CheckSpacesReservation book = new CheckSpacesReservation();
                    book.setReservationId(reservation.getId());
                    book.setHotel(reservation.getBed().getRoom().getFlat().getHotel().getId() == 327L ? "ermak" : "proizvod");
                    book.setStatus("ok");
                    book.setTabNumber(reservation.getTabnum());
                    book.setDateStart(dateFormat.format(reservation.getDateStart()));
                    book.setDateFinish(dateFormat.format(reservation.getDateFinish()));
                    book.setBookReportId(reservation.getBookReportId());
                    bookedGuest.add(book);
                }
            }
            bookedResponse.setGuests(bookedGuest);
            return bookedResponse;
        }
        // -----

        return response;
    }

    // Метод для проверки заселения в памяти
    private void processEmployeesInMemory(
            List<TabWithItr> employees,
            List<Bed> beds,
            List<Reservation> allReservations,
            List<Reservation> needToBook,
            List<Guest> allGuests,
            String hotelName,
            CheckSpacesResponse response,
            SimpleDateFormat dateFormat,
            List<Bed> proizvodBeds // Дублирую параметр только для того чтобы селить не поместившихся ИТР на производ-ую
    ) throws ParseException {
        // Сортируем сотрудников: сначала руководители, потом мужчины
        employees.sort(Comparator
                .comparing((TabWithItr e) -> !isLeadershipPosition(e.getTabNumber())) // Руководители вперед
                .thenComparing(e -> e.getMale() == 0) // Мужчины вперед
        );

        for (TabWithItr employee : employees) {
            for (DatePair datePair : employee.getDates()) {
                Date dateStart = dateFormat.parse(datePair.getDate());
                Date dateFinish = calculateEndDate(dateStart, datePair.getDaysCount());

                // Проверяем, есть ли свободное место
                Bed isSuccess = tryCheckInMemory(
                        dateStart,
                        dateFinish,
                        employee,
                        beds,
                        allReservations,
                        allGuests
                );

                // Если расселяем ИТР и не хватило места проубем его поселить на производстенную
                if (employee.getIsItr() && isSuccess == null) {
                    isSuccess = tryCheckInMemory(
                            dateStart,
                            dateFinish,
                            employee,
                            proizvodBeds,
                            allReservations,
                            allGuests
                    );
                }

                // Если место нашлось то добавляем в список для бронирования с заполнеными параметрами
                // Иначе тоже добавляем то не заполняем место договор и тд только работника для дальнейшей идентификации
                Reservation tmp = new Reservation();
                if (hotelName.equals("ermak")) {
                    Contract contract = contractRepository.getById(1873L);
                    tmp.setContract(contract);
                } else {
                    Contract contract = contractRepository.getById(1901L);
                    tmp.setContract(contract);
                }
                tmp.setTabnum(employee.getTabNumber());
                Employee emp = employeeRepository.findByTabnumAndEndDate(employee.getTabNumber(), null);
                if (emp != null) {
                    tmp.setFirstname(emp.getFirstname());
                    tmp.setLastname(emp.getLastname());
                    tmp.setSecondName(emp.getSecondName());
                    tmp.setFromFilial(filialRepository.findByCode(emp.getIdFilial()));
                }
                EventKind eventKind = eventKindRepository.getById(2L); // Обучение
                tmp.setEventKind(eventKind);
                tmp.setMale(employee.getMale() == 1);
                if (isSuccess != null) tmp.setBed(isSuccess);
                tmp.setDateStart(dateStart);
                tmp.setDateFinish(dateFinish);
                tmp.setMale(employee.getMale() == 0);
                tmp.setNote(employee.getGroupNumber() + " || " + employee.getEventName());
                allReservations.add(tmp);
                needToBook.add(tmp);


                // Записываем результат
                response.getGuests().add(createReservationRecord(
                        employee,
                        dateStart,
                        dateFinish,
                        hotelName,
                        isSuccess != null,
                        dateFormat
                ));
            }
        }
    }

    // Проверка возможности заселения (в памяти)
    private Bed tryCheckInMemory(
            Date dateStart,
            Date dateFinish,
            TabWithItr employee,
            List<Bed> beds,
            List<Reservation> allReservations,
            List<Guest> allGuests
    ) {
        for (Bed bed : beds) {
            if (bed == null) continue;
            // 1. Проверяем, свободна ли кровать
            boolean isBedFree = isBedFreeInMemory(bed, dateStart, dateFinish, allReservations, allGuests);
            if (!isBedFree) continue;

            // 2. Проверяем соседей-руководителей
            boolean hasLeaderNeighbor = hasLeaderInRoomInMemory(
                    bed.getRoom(),
                    dateStart,
                    dateFinish,
                    allReservations,
                    allGuests
            );
            if (hasLeaderNeighbor) continue;

            // 3. Если сотрудник — руководитель, проверяем всю секцию на наличие записей
            if (isLeadershipPosition(employee.getTabNumber())) {
                boolean hasNeighbors = hasAnyReservationsInFlatInMemory(
                        bed.getRoom().getFlat(),
                        dateStart,
                        dateFinish,
                        allReservations,
                        allGuests
                );
                if (hasNeighbors) continue;
            }

            // 4. Если сотрудник — женщина, проверяем соседей-мужчин
            if (employee.getMale() == 0) {
                boolean hasMaleNeighbor = hasMaleInRoomInMemory(
                        bed.getRoom(),
                        dateStart,
                        dateFinish,
                        allReservations,
                        allGuests
                );
                if (hasMaleNeighbor) continue;
            }

            // Если все проверки пройдены — место свободно
            return bed;
        }
        return null;
    }

    // Проверяет, свободна ли кровать в указанные даты
    private boolean isBedFreeInMemory(
            Bed bed,
            Date dateStart,
            Date dateFinish,
            List<Reservation> allReservations,
            List<Guest> allGuests
    ) {
        // Проверка броней findAllByDateStartLessThanAndDateFinishGreaterThanAndBed(dateFinish, dateStart, guest.getBed());
        boolean hasReservation = allReservations.stream()
                .filter(r -> r.getBed() != null && r.getBed().equals(bed))
                .anyMatch(r ->
                        r.getDateStart() != null && r.getDateFinish() != null &&
                                r.getDateStart().getTime() <= dateFinish.getTime() &&
                                r.getDateFinish().getTime() >= dateStart.getTime()
                );

        // Проверка гостей
        boolean hasGuest = allGuests.stream()
                .filter(g -> g.getBed() != null && g.getBed().equals(bed))
                .anyMatch(g ->
                        g.getDateStart() != null &&
                                g.getDateFinish() != null &&
                                g.getDateStart().getTime() <= dateFinish.getTime() &&
                                g.getDateFinish().getTime() >= dateStart.getTime()
                );

        return !hasReservation && !hasGuest;
    }

    // Проверяет, есть ли брони/гости во всей секции (квартире)
    private boolean hasAnyReservationsInFlatInMemory(
            Flat flat,
            Date dateStart,
            Date dateFinish,
            List<Reservation> allReservations,
            List<Guest> allGuests
    ) {
        return allReservations.stream().anyMatch(r ->
                r.getBed() != null &&
                r.getBed().getRoom().getFlat().equals(flat) &&
                        r.getDateStart() != null &&
                        r.getDateFinish() != null &&
                        r.getDateStart().before(dateFinish) &&
                        r.getDateFinish().after(dateStart)
        ) || allGuests.stream().anyMatch(g ->
                g.getBed().getRoom().getFlat().equals(flat) &&
                        g.getDateStart() != null &&
                        g.getDateFinish() != null &&
                        g.getDateStart().before(dateFinish) &&
                        g.getDateFinish().after(dateStart)
        );
    }

    // Проверяет, есть ли руководитель в комнате
    private boolean hasLeaderInRoomInMemory(
            Room room,
            Date dateStart,
            Date dateFinish,
            List<Reservation> allReservations,
            List<Guest> allGuests
    ) {
        for (Reservation reservation : allReservations.stream().filter(r ->
                r.getBed() != null &&
                r.getBed().getRoom().getFlat().equals(room.getFlat()) &&
                        r.getDateStart() != null &&
                        r.getDateFinish() != null &&
                        r.getDateStart().before(dateFinish) &&
                        r.getDateFinish().after(dateStart)
        ).collect(Collectors.toList())) {
            if (isLeadershipPosition(reservation.getTabnum())) return true;
        }
        for (Guest guest : allGuests.stream().filter(r ->
                r.getBed().getRoom().getFlat().equals(room.getFlat()) &&
                        r.getDateStart() != null &&
                        r.getDateFinish() != null &&
                        r.getDateStart().before(dateFinish) &&
                        r.getDateFinish().after(dateStart)
        ).collect(Collectors.toList())) {
            if (guest.getEmployee() != null)
                if (isLeadershipPosition(guest.getEmployee().getTabnum())) return true;
        }
        return false;
    }

    // Проверяет, есть ли мужчины в комнате
    private boolean hasMaleInRoomInMemory(
            Room room,
            Date dateStart,
            Date dateFinish,
            List<Reservation> allReservations,
            List<Guest> allGuests
    ) {
        return allReservations.stream().anyMatch(r ->
                r.getBed() != null &&
                r.getBed().getRoom().equals(room) &&
                        r.getDateStart() != null &&
                        r.getDateFinish() != null &&
                        r.getDateStart().before(dateFinish) &&
                        r.getDateFinish().after(dateStart) &&
                        r.getMale()
        ) || allGuests.stream().anyMatch(g ->
                g.getBed().getRoom().equals(room) &&
                        g.getDateStart() != null &&
                        g.getDateFinish() != null &&
                        g.getDateStart().before(dateFinish) &&
                        g.getDateFinish().after(dateStart) &&
                        g.getMale()
        );
    }

    // Вспомогательный метод для расчета даты выезда
    private Date calculateEndDate(Date startDate, int daysCount) throws ParseException {
        LocalDateTime endDateTime = startDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .plusDays(daysCount - 1);
        // Формат: день-месяц-год часы:минуты (DD-MM-YYYY HH:mm)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String endDateStr = endDateTime.format(formatter) + " 23:59";
        return new SimpleDateFormat("dd-MM-yyyy HH:mm").parse(endDateStr);
    }

    // Создает запись о результате проверки
    private CheckSpacesReservation createReservationRecord(
            TabWithItr employee,
            Date dateStart,
            Date dateFinish,
            String hotel,
            boolean isSuccess,
            SimpleDateFormat dateFormat
    ) {
        CheckSpacesReservation record = new CheckSpacesReservation();
        record.setTabNumber(employee.getTabNumber());
        record.setDateStart(dateFormat.format(dateStart));
        record.setDateFinish(dateFormat.format(dateFinish));
        record.setRange(dateFormat.format(dateStart) + " - " + dateFormat.format(dateFinish));
        record.setStatus(isSuccess ? "ok" : "error");
        record.setErrorCode(isSuccess ? null : 0);
        record.setHotel(hotel);
        return record;
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

    public List<CheckEmployeeResponse> checkEmployee(CheckEmployeeRequest body) throws ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        List<CheckEmployeeResponse> response = new ArrayList<>();
        for (Reservation reservation : reservationRepository.findAllByTabnumAndDateStartLessThanAndDateFinishGreaterThan(body.getTabNumber(), dateFormatter.parse(body.getDateFinish()), dateFormatter.parse(body.getDateStart()))) {
            CheckEmployeeResponse record = new CheckEmployeeResponse();
            record.setHotel(reservation.getBed().getRoom().getFlat().getHotel().getId() == 327L ? "ermak" : "proizvod");
            record.setReservationId(reservation.getId());
            record.setDateStart(dateFormatter.format(reservation.getDateStart()));
            record.setDateFinish(dateFormatter.format(reservation.getDateFinish()));
            record.setTabNumber(body.getTabNumber());
            response.add(record);
        }
        return response;
    }

    @Transactional
    public List<CancelReservationResponse> reservationCancel(CancelReservationRequest request) {
        List<CancelReservationResponse> response = new ArrayList<>();
        // Логируем запрос в отдельный протокол
        BookReport bookReport = new BookReport();
        bookReport.setDate(new Date());
        if (request.getAuthor() != null) bookReport.setUsername(request.getAuthor() + " remove");
        else bookReport.setUsername(SecurityManager.getCurrentUser() + " remove");
        bookReport.setStatus("ok");
        bookReportRepository.save(bookReport);

        for (TabAndReservation reservation : request.getReservations()) {
            CancelReservationResponse record = new CancelReservationResponse();
            record.setReservationId(reservation.getReservationId());
            record.setTabNumber(reservation.getTabNumber());
            Optional<Reservation> reservationOpt = reservationRepository.findByIdAndTabnum(reservation.getReservationId(), reservation.getTabNumber());
            if (reservationOpt.isPresent()) {
                record.setStatus("ok");
                reservationRepository.delete(reservationOpt.get());
            } else record.setStatus("error");

            RecordBookReport recordBookReport = new RecordBookReport();
            recordBookReport.setBookReport(bookReport);
            recordBookReport.setTabnumber(reservation.getTabNumber());
            recordBookReport.setFio("");
            recordBookReport.setStatus(record.getStatus());
            recordBookReportRepository.save(recordBookReport);

            response.add(record);
        }
        return response;
    }
}
