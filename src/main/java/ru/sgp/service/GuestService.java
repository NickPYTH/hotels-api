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
import ru.sgp.dto.GuestDTO;
import ru.sgp.dto.integration.AddGuestsForEventDTO;
import ru.sgp.dto.report.GuestReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;
import ru.sgp.utils.SecurityManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    ModelMapper modelMapper = new ModelMapper();
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private FilialRepository filialRepository;
    @Autowired
    private FlatRepository flatRepository;
    @Autowired
    private ReservationRepository reservationRepository;


    public byte[] export(JasperPrint jasperPrint) throws JRException {
        Exporter exporter;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exporter = new JRXlsxExporter();
        //exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();
        return outputStream.toByteArray();
    }

    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
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

    @Transactional
    public List<GuestDTO> getAll() {
        SecurityManager.getCurrentUser();
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        List<GuestDTO> response = new ArrayList<>();
        List<Guest> guests = new ArrayList<>();
        if (user.getRole().getId() == 1L || user.getRole().getId() == 5L) {
            guests = guestRepository.findAll();
        } else {
            Filial filial = filialRepository.findByCode(user.getEmployee().getIdFilial());
            guests = guestRepository.findAllByBedRoomFlatHotelFilial(filial);
        }
        for (Guest guest : guests) {
            GuestDTO guestDTO = new GuestDTO();
            if (guest.getEmployee() != null) {
                Employee employee = guest.getEmployee();
                guestDTO.setTabnum(employee.getTabnum());
                guestDTO.setFilialEmployee(employee.getIdFilial().toString());
            }
            if (guest.getContract() != null) guestDTO.setContractId(guest.getContract().getId());
            guestDTO.setId(guest.getId());
            guestDTO.setFirstname(guest.getFirstname());
            guestDTO.setLastname(guest.getLastname());
            guestDTO.setSecondName(guest.getSecondName());
            guestDTO.setNote(guest.getNote());
            guestDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
            guestDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
            guestDTO.setBedId(guest.getBed().getId());
            guestDTO.setBedName(guest.getBed().getName());
            guestDTO.setRoomId(guest.getBed().getRoom().getId());
            guestDTO.setRoomName(guest.getBed().getRoom().getName());
            guestDTO.setFlatName(guest.getBed().getRoom().getFlat().getName());
            guestDTO.setFlatId(guest.getBed().getRoom().getFlat().getId());
            guestDTO.setHotelName(guest.getBed().getRoom().getFlat().getHotel().getName());
            guestDTO.setHotelId(guest.getBed().getRoom().getFlat().getHotel().getId());
            guestDTO.setFilialName(guest.getBed().getRoom().getFlat().getHotel().getFilial().getName());
            guestDTO.setFilialId(guest.getBed().getRoom().getFlat().getHotel().getFilial().getId());
            guestDTO.setMemo(guest.getMemo());
            guestDTO.setMale(guest.getMale());
            guestDTO.setCheckouted(guest.getCheckouted());
            if (guest.getContract() != null) {
                guestDTO.setContractId(guest.getContract().getId());
                guestDTO.setBilling(guest.getContract().getBilling());
                guestDTO.setReason(guest.getContract().getReason().getName());
            }
            if (guest.getOrganization() != null) {
                guestDTO.setOrganizationId(guest.getOrganization().getId());
                guestDTO.setOrganizationName(guest.getOrganization().getName());
            }
            guestDTO.setRegPoMestu(guest.getRegPoMestu());
            response.add(guestDTO);
        }
        return response;
    }

    @Transactional
    public GuestDTO checkout(Long id) {
        Guest guest = guestRepository.findById(id).get();
        guest.setCheckouted(true);
        guestRepository.save(guest);
        return new GuestDTO();
    }

    public List<GuestDTO> update(GuestDTO guestDTO) throws Exception {
        List<GuestDTO> response = new ArrayList<>();
        ModelMapper modelMapper = new ModelMapper();
        Date dateStart = dateTimeFormatter.parse(guestDTO.getDateStart());
        Date dateFinish = dateTimeFormatter.parse(guestDTO.getDateFinish());

        // Определение места в зависимости от того существует оно или это будущее доп. место (свойство isExtra=true)
        Bed bed = null;
        Boolean isExtra = false;
        if (guestDTO.getBedId() != null) {
            bed = bedRepository.getById(guestDTO.getBedId());
            isExtra = bed.getIsExtra();
        }
        else {
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
            else guestBeforeState = modelMapper.map(guest, GuestDTO.class);
            response.add(guestBeforeState);
        } else response.add(new GuestDTO()); // TODO
        // -----

        // Определяем кто перед нами работник Газпрома или сторонник
        if (guestDTO.getTabnum() != null) {
            Employee employee = employeeRepository.findByTabnum(guestDTO.getTabnum());
            guest.setEmployee(employee);
            Organization gts = organizationRepository.getById(11L); // Организация - ГТС
            guest.setOrganization(gts);
        } else {
            Organization org = organizationRepository.getById(guestDTO.getOrganizationId());
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
        // -----

        if (bed.getRoom() == null){
            // Значит это доп. место
            Room room = roomRepository.getById(guestDTO.getRoomId());
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
            Employee employee = employeeRepository.findByTabnum(guestDTO.getFamilyMemberOfEmployee());
            guest.setFamilyMemberOfEmployee(employee);
        } else guest.setFamilyMemberOfEmployee(null);
        //

        // Устанавливаем договор
        if (guestDTO.getContractId() != null) {
            Contract contract = contractRepository.getById(guestDTO.getContractId());
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
                    tmp.setFilialName(reservationTmp.getBed().getRoom().getFlat().getHotel().getFilial().getName());
                    tmp.setHotelName(reservationTmp.getBed().getRoom().getFlat().getHotel().getName());
                    tmp.setFlatName(reservationTmp.getBed().getRoom().getFlat().getName());
                    tmp.setRoomName(reservationTmp.getBed().getRoom().getName());
                    tmp.setBedName(reservationTmp.getBed().getName());
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
                tmp.setFilialName(reservationTmp.getBed().getRoom().getFlat().getHotel().getFilial().getName());
                tmp.setHotelName(reservationTmp.getBed().getRoom().getFlat().getHotel().getName());
                tmp.setFlatName(reservationTmp.getBed().getRoom().getFlat().getName());
                tmp.setRoomName(reservationTmp.getBed().getRoom().getName());
                tmp.setBedName(reservationTmp.getBed().getName());
                tmp.setDateStart(dateTimeFormatter.format(reservationTmp.getDateStart()));
                tmp.setDateFinish(dateTimeFormatter.format(reservationTmp.getDateFinish()));
                response.add(tmp);
                guest.setBed(oldBed);
                return response;
            }
            // -----

            // Проверяем не пересекается ли дата проживания с кем-то на выбранном месте ЗАПИСИ О ПРОЖИВАНИИ
            List<Guest> guests = new ArrayList<>();

            // Проверяем пересечения периодов проживания с самими собой в других филиалах, проверяю по ФИО чтобы задеть и работников и сторонников, а также чтобы было указано другое место
            guests = guestRepository.findAllByDateStartLessThanAndDateFinishGreaterThanAndFirstnameAndLastnameAndSecondNameAndBedIsNotAndBedRoomFlatHotelFilial(dateFinish, dateStart, guest.getFirstname(), guest.getLastname(), guest.getSecondName(), guest.getBed(), guest.getBed().getRoom().getFlat().getHotel().getFilial());
            if (!guests.isEmpty()) {
                Guest guestTmp = guests.get(0);
                GuestDTO tmp = new GuestDTO();
                tmp.setError("datesError");
                tmp.setLastname(guestTmp.getLastname());
                tmp.setFirstname(guestTmp.getFirstname());
                tmp.setSecondName(guestTmp.getSecondName());
                tmp.setFilialName(guestTmp.getBed().getRoom().getFlat().getHotel().getFilial().getName());
                tmp.setHotelName(guestTmp.getBed().getRoom().getFlat().getHotel().getName());
                tmp.setFlatName(guestTmp.getBed().getRoom().getFlat().getName());
                tmp.setRoomName(guestTmp.getBed().getRoom().getName());
                tmp.setBedName(guestTmp.getBed().getName());
                tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
                tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
                response.add(tmp);
                guest.setBed(oldBed);
                return response;
            }
            // -----


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
                tmp.setFilialName(guestTmp.getBed().getRoom().getFlat().getHotel().getFilial().getName());
                tmp.setHotelName(guestTmp.getBed().getRoom().getFlat().getHotel().getName());
                tmp.setFlatName(guestTmp.getBed().getRoom().getFlat().getName());
                tmp.setRoomName(guestTmp.getBed().getRoom().getName());
                tmp.setBedName(guestTmp.getBed().getName());
                tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
                tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
                response.add(tmp);
                guest.setBed(oldBed);
                return response;
            } else {
                guest.setDateStart(dateStart);
                guest.setDateFinish(dateFinish);
            }
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
        if (!isExtra) {
            guestDTO.setRoomName(guest.getBed().getRoom().getName());
            guestDTO.setFlatName(guest.getBed().getRoom().getFlat().getName());
        }
        guestDTO.setBedName(guest.getBed().getName());
        response.add(guestDTO); // Добавляем обновленную сущность для логов
        return response;
    }

    public GuestDTO getFioByTabnum(Integer tabnum) {
        Employee employee = employeeRepository.findByTabnum(tabnum);
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
        List<Employee> employees = employeeRepository.findAllByLastnameAndFirstnameAndSecondName(lastname, firstname, secondName);
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

    @Transactional
    public GuestDTO delete(Long id) {
        Guest guest = guestRepository.getById(id);
        Bed bed = guest.getBed();
        if (bed.getIsExtra()) bedRepository.delete(bed);
        GuestDTO guestDTO = new GuestDTO();
        Optional<Reservation> reservationOptional = reservationRepository.findByGuest(guest);
        if (reservationOptional.isPresent()) {
            Reservation reservation = reservationOptional.get();
            reservation.setGuest(null);
            reservation.setDateStart(reservation.getDateStartConfirmed());
            reservation.setDateFinish(reservation.getDateFinishConfirmed());
            reservationRepository.save(reservation);
        }
        guestRepository.deleteById(id);
        guestDTO.setId(id);
        return guestDTO;
    }

    public byte[] getGuestReport() throws JRException {
        List<GuestReportDTO> guestReportDTOS = new ArrayList<>();

        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        List<GuestDTO> response = new ArrayList<>();
        List<Guest> guests = new ArrayList<>();
        if (user.getRole().getId() == 1L || user.getRole().getId() == 5L) {
            guests = guestRepository.findAll();
        } else {
            Filial filial = filialRepository.findByCode(user.getEmployee().getIdFilial());
            guests = guestRepository.findAllByBedRoomFlatHotelFilial(filial);
        }
        for (Guest guest : guests) {
            GuestReportDTO guestReportDTO = new GuestReportDTO();
            guestReportDTO.setId(guest.getId().toString());
            guestReportDTO.setSurname(guest.getLastname());
            guestReportDTO.setSecondName(guest.getSecondName());
            guestReportDTO.setName(guest.getFirstname());
            guestReportDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
            guestReportDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
            guestReportDTO.setRoom(guest.getBed().getRoom().getFlat().getName());
            guestReportDTO.setHotel(guest.getBed().getRoom().getFlat().getHotel().getName());
            guestReportDTO.setFilial(guest.getBed().getRoom().getFlat().getHotel().getFilial().getName());
            if (guest.getOrganization() != null)
                guestReportDTO.setOrg(guest.getOrganization().getName());
            guestReportDTO.setRepPoMestu(guest.getRegPoMestu() ? "+" : "-");
            guestReportDTO.setCz(guest.getMemo());
            guestReportDTO.setMale(guest.getMale() ? "man" : "woman");
            if (guest.getCheckouted() != null)
                guestReportDTO.setCheckouted(guest.getCheckouted() ? "+" : "-");
            else guestReportDTO.setCheckouted("");
            if (guest.getContract() != null) {
                guestReportDTO.setContract(guest.getContract().getDocnum());
                guestReportDTO.setBilling(guest.getContract().getBilling());
                guestReportDTO.setReason(guest.getContract().getReason().getName());
            } else {
                guestReportDTO.setContract("");
                guestReportDTO.setBilling("");
                guestReportDTO.setReason("");
            }
            guestReportDTO.setBed(guest.getBed().getRoom().getName());
            guestReportDTOS.add(guestReportDTO);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/GuestReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(guestReportDTOS);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

    public List<GuestDTO> getAllByOrganizationId(Long id) {
        return guestRepository.findAllByOrganization(organizationRepository.getById(id)).stream().map(guest -> modelMapper.map(guest, GuestDTO.class)).collect(Collectors.toList());
    }

    public List<String> getGuestsLastnames() {
        return guestRepository.findDistinctLastname();
    }

    @Transactional
    public List<GuestDTO> addGuestsForEvent(AddGuestsForEventDTO data) throws Exception {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        Optional<Hotel> hotelOpt = hotelRepository.findById(data.getHotelId());
        if (hotelOpt.isEmpty()) throw new Exception("Hotel with id " + data.getHotelId() + " not found");
        Hotel hotel = hotelOpt.get();
        Date dateStart = dateFormatter.parse(data.getDateStart());
        Date dateFinish = dateFormatter.parse(data.getDateEnd());
        List<GuestDTO> response = new ArrayList<>();
        List<Employee> empListOrderBySex = new ArrayList<>();
        for (Integer tabNumber : data.getTabNumbers()) {
            Employee employee = employeeRepository.findByTabnum(tabNumber);
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
                    guest.setMale(employee.getMale() == 1 ? true : false);
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
                    employee = employeeRepository.findByTabnum(tabnum);
                } else {
                    Integer tabnum = (int) row.getCell(0).getNumericCellValue();
                    employee = employeeRepository.findByTabnum(tabnum);
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
                List<Employee> employees = employeeRepository.findAllByLastnameAndFirstnameAndSecondName(lastname, firstname, secondName);
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
}