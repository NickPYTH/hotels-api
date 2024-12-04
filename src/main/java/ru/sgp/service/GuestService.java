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
import ru.sgp.dto.GuestDTO;
import ru.sgp.dto.report.GuestReportDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static ru.sgp.spnego.SpnegoHelper.findUsernameByTabnum;

@Service
public class GuestService {
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    StatusRepository statusRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    ReasonRepository reasonRepository;

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

    @Transactional
    public List<GuestDTO> getAll() {
        List<GuestDTO> response = new ArrayList<>();
        for (Guest guest : guestRepository.findAll()) {
            GuestDTO guestDTO = new GuestDTO();
            if (guest.getEmployee() != null) {
                guestDTO.setTabnum(guest.getEmployee().getTabnum());
            }
            guestDTO.setId(guest.getId());
            guestDTO.setFirstname(guest.getFirstname());
            guestDTO.setLastname(guest.getLastname());
            guestDTO.setSecondName(guest.getSecondName());
            guestDTO.setNote(guest.getNote());
            guestDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
            guestDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
            guestDTO.setRoomId(guest.getRoom().getId());
            guestDTO.setRoomName(guest.getRoom().getName());
            guestDTO.setFlatName(guest.getRoom().getFlat().getName());
            guestDTO.setFlatId(guest.getRoom().getFlat().getId());
            guestDTO.setHotelName(guest.getRoom().getFlat().getHotel().getName());
            guestDTO.setHotelId(guest.getRoom().getFlat().getHotel().getId());
            guestDTO.setFilialName(guest.getRoom().getFlat().getHotel().getFilial().getName());
            guestDTO.setFilialId(guest.getRoom().getFlat().getHotel().getFilial().getId());
            guestDTO.setMemo(guest.getMemo());
            guestDTO.setBilling(guest.getBilling());
            guestDTO.setReason(guest.getReason().getName());
            guestDTO.setMale(guest.getMale());
            guestDTO.setCheckouted(guest.getCheckouted());
            if (guest.getOrganization() != null)
                guestDTO.setOrganization(guest.getOrganization().getName());
            guestDTO.setRegPoMestu(guest.getRegPoMestu());
            response.add(guestDTO);
        }
        return response;
    }

    @Transactional
    public GuestDTO create(GuestDTO guestDTO) throws ParseException {
        Room room = roomRepository.getById(guestDTO.getRoomId());
        Guest guest = new Guest();
        Organization orgTmp;
        if (guestDTO.getTabnum() != null) {
            Employee employee = employeeRepository.findByTabnum(guestDTO.getTabnum());
            guest.setEmployee(employee);
            orgTmp = organizationRepository.getById(11L); // ADM
            guest.setOrganization(orgTmp);
        } else { // Установка подрядной организации без договора если такое имеется
            orgTmp = organizationRepository.findByName(guestDTO.getOrganization());
            if (orgTmp == null) {
                if (guestDTO.getOrganization().length() > 0) {
                    orgTmp = new Organization();
                    orgTmp.setName(guestDTO.getOrganization());
                    organizationRepository.save(orgTmp);
                    guest.setOrganization(orgTmp);
                }
            } else guest.setOrganization(orgTmp);
        }
        guest.setRegPoMestu(guestDTO.getRegPoMestu());
        guest.setFirstname(guestDTO.getFirstname());
        guest.setLastname(guestDTO.getLastname());
        guest.setSecondName(guestDTO.getSecondName());
        guest.setMale(guestDTO.getMale());
        if (guestDTO.getContractId() != null) {
            Contract contract = contractRepository.getById(guestDTO.getContractId());
            guest.setContract(contract);
        } else {
            List<Contract> contract1List = contractRepository.findAllByFilialAndHotelAndOrganization(room.getFlat().getHotel().getFilial(), room.getFlat().getHotel(), organizationRepository.getById(2L));
            if (contract1List.size() > 0) {
                guest.setContract(contract1List.get(0));
            }
        }
        guest.setCheckouted(false);

        Date dateStart = dateTimeFormatter.parse(guestDTO.getDateStart());
        Date dateFinish = dateTimeFormatter.parse(guestDTO.getDateFinish());
        guest.setDateStart(dateStart);
        guest.setDateFinish(dateFinish);

        // Проверка существования жильца в заданном диапазоне и оргнанизации
        List<Guest> guests = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndOrganizationAndLastnameAndFirstnameAndSecondNameAndCheckouted(dateFinish, dateStart, guest.getOrganization(), guest.getLastname(), guest.getFirstname(), guest.getSecondName(), false);
        if (guests.size() > 0) {
            Guest guestTmp = guests.get(0);
            GuestDTO tmp = new GuestDTO();
            tmp.setError("Dates range error");
            tmp.setHotelName(guestTmp.getRoom().getFlat().getHotel().getName());
            tmp.setFlatName(guestTmp.getRoom().getFlat().getName());
            tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
            tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
            return tmp;
        } else {
            guest.setDateStart(dateStart);
            guest.setDateFinish(dateFinish);
        }
        // -----

        // Проверка комнаты на всем выбранном периоде
        List<Guest> guests1 = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndCheckoutedAndRoom(dateFinish, dateStart, false, room);
        if (guests1.size() >= room.getBedsCount()) {
            Guest guestTmp = guests1.get(0);
            GuestDTO tmp = new GuestDTO();
            tmp.setError("Room busy");
            tmp.setNote(guest.getLastname() + " " + guest.getLastname() + " " + guest.getSecondName());
            tmp.setHotelName(guestTmp.getRoom().getFlat().getHotel().getName());
            tmp.setFlatName(guestTmp.getRoom().getFlat().getName());
            tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
            tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
            return tmp;
        }
        // -----

        if (guestDTO.getBedName() != null) {
            Optional<Bed> bedOpt = bedRepository.findByRoomAndName(room, guestDTO.getBedName());
            if (bedOpt.isPresent()) {
                guest.setBed(bedOpt.get());
            }
        }

        guest.setRoom(room);
        guest.setMemo(guestDTO.getMemo());
        guest.setBilling(guestDTO.getBilling());
        Reason reason = reasonRepository.findByName(guestDTO.getReason());
        if (reason == null) {
            Reason newReason = new Reason();
            newReason.setIsDefault(false);
            newReason.setName(guestDTO.getReason());
            reasonRepository.save(newReason);
            guest.setReason(newReason);
        } else guest.setReason(reason);
        guestRepository.save(guest);
        if (guestRepository.findAllByRoomAndCheckouted(room, false).size() == room.getBedsCount()) {
            //room.setStatus(statusRepository.getById(2L)); // 2 mean no beds
            roomRepository.save(room);
        }
        guestDTO.setId(guest.getId());
        return guestDTO;
    }

    @Transactional
    public GuestDTO checkout(Long id) {
        Guest guest = guestRepository.findById(id).get();
        Room room = guest.getRoom();
        guest.setCheckouted(true);
        guestRepository.save(guest);
        GuestDTO guestDTO = new GuestDTO();
        if (guestRepository.findAllByRoomAndCheckouted(room, false).size() != room.getBedsCount()) {
            room.setStatus(statusRepository.getById(1L)); // 2 mean no beds
            roomRepository.save(room);
        }
        return guestDTO;
    }

    @Transactional
    public GuestDTO update(GuestDTO guestDTO) throws ParseException {
        Room room = roomRepository.getById(guestDTO.getRoomId());
        Guest guest = guestRepository.getById(guestDTO.getId());
        if (guestDTO.getTabnum() != null) {
            Employee employee = employeeRepository.findByTabnum(guestDTO.getTabnum());
            guest.setEmployee(employee);
            Organization tmp = organizationRepository.getById(11L); // ADM
            guest.setOrganization(tmp);
        } else {
            Organization org = organizationRepository.findByName(guestDTO.getOrganization());
            if (org == null) {
                if (guestDTO.getOrganization().length() > 0) {
                    Organization newOrganization = new Organization();
                    newOrganization.setName(guestDTO.getOrganization());
                    organizationRepository.save(newOrganization);
                    guest.setOrganization(newOrganization);
                }
            } else guest.setOrganization(org);
        }
        guest.setRegPoMestu(guestDTO.getRegPoMestu());
        guest.setFirstname(guestDTO.getFirstname());
        guest.setLastname(guestDTO.getLastname());
        guest.setSecondName(guestDTO.getSecondName());
        if (guestDTO.getContractId() != null) {
            Contract contract = contractRepository.getById(guestDTO.getContractId());
            guest.setContract(contract);
        }
        guest.setMale(guestDTO.getMale());

        Date dateStart = dateTimeFormatter.parse(guestDTO.getDateStart());
        Date dateFinish = dateTimeFormatter.parse(guestDTO.getDateFinish());
        List<Guest> guests = guestRepository.findAllByDateStartBeforeAndDateFinishAfterAndOrganizationAndLastnameAndFirstnameAndSecondNameAndCheckouted(dateFinish, dateStart, guest.getOrganization(), guest.getLastname(), guest.getFirstname(), guest.getSecondName(), false);
        if (guests.size() == 1) {
            Guest guestTmp = guests.get(0);
            if (guestTmp.getId() == guest.getId()) {
                guest.setDateStart(dateStart);
                guest.setDateFinish(dateFinish);
            } else {
                GuestDTO tmp = new GuestDTO();
                tmp.setError("Dates range error");
                tmp.setHotelName(guestTmp.getRoom().getFlat().getHotel().getName());
                tmp.setFlatName(guestTmp.getRoom().getFlat().getName());
                tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
                tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
                return tmp;
            }
        } else if (guests.size() > 1) {
            Guest guestTmp = guests.get(0);
            GuestDTO tmp = new GuestDTO();
            tmp.setError("Dates range error");
            tmp.setHotelName(guestTmp.getRoom().getFlat().getHotel().getName());
            tmp.setFlatName(guestTmp.getRoom().getFlat().getName());
            tmp.setDateStart(dateTimeFormatter.format(guestTmp.getDateStart()));
            tmp.setDateFinish(dateTimeFormatter.format(guestTmp.getDateFinish()));
            return tmp;
        } else {
            guest.setDateStart(dateStart);
            guest.setDateFinish(dateFinish);
        }

        guest.setMemo(guestDTO.getMemo());
        guest.setBilling(guestDTO.getBilling());
        Reason reason = reasonRepository.findByName(guestDTO.getReason());
        if (reason == null) {
            Reason newReason = new Reason();
            newReason.setIsDefault(false);
            newReason.setName(guestDTO.getReason());
            reasonRepository.save(newReason);
            guest.setReason(newReason);
        } else guest.setReason(reason);
        guest.setRoom(room);
        guestRepository.save(guest);
        return guestDTO;
    }

    public GuestDTO getFioByTabnum(Integer tabnum) {
        Employee employee = employeeRepository.findByTabnum(tabnum);
        GuestDTO guestDTO = new GuestDTO();
        if (employee != null) {
            guestDTO.setFirstname(employee.getFirstname());
            guestDTO.setLastname(employee.getLastname());
            guestDTO.setSecondName(employee.getSecondName());
            guestDTO.setEmail(findUsernameByTabnum(tabnum));
        }
        return guestDTO;
    }

    public GuestDTO delete(Long id) {
        GuestDTO guestDTO = new GuestDTO();
        guestRepository.deleteById(id);
        guestDTO.setId(id);
        return guestDTO;
    }

    public byte[] getGuestReport() throws JRException {
        List<GuestReportDTO> guestReportDTOS = new ArrayList<>();
        for (Guest guest : guestRepository.findAll()) {
            GuestReportDTO guestReportDTO = new GuestReportDTO();
            guestReportDTO.setId(guest.getId().toString());
            guestReportDTO.setSurname(guest.getLastname());
            guestReportDTO.setSecondName(guest.getSecondName());
            guestReportDTO.setName(guest.getFirstname());
            guestReportDTO.setDateStart(dateTimeFormatter.format(guest.getDateStart()));
            guestReportDTO.setDateFinish(dateTimeFormatter.format(guest.getDateFinish()));
            guestReportDTO.setRoom(guest.getRoom().getFlat().getName());
            guestReportDTO.setHotel(guest.getRoom().getFlat().getHotel().getName());
            guestReportDTO.setFilial(guest.getRoom().getFlat().getHotel().getFilial().getName());
            guestReportDTO.setOrg(guest.getOrganization().getName());
            guestReportDTO.setRepPoMestu(guest.getRegPoMestu() ? "+" : "-");
            guestReportDTO.setCz(guest.getMemo());
            guestReportDTO.setBilling(guest.getBilling());
            guestReportDTO.setReason(guest.getReason().getName());
            guestReportDTO.setMale(guest.getMale() ? "man" : "woman");
            guestReportDTO.setCheckouted(guest.getCheckouted() ? "+" : "-");
            if (guest.getContract() != null)
                guestReportDTO.setContract(guest.getContract().getDocnum());
            else
                guestReportDTO.setContract("");
            guestReportDTO.setBed(guest.getRoom().getName());
            guestReportDTOS.add(guestReportDTO);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/GuestReport.jrxml"));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(guestReportDTOS);
        Map<String, Object> parameters = new HashMap<>();
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return export(jasperPrint);
    }

}