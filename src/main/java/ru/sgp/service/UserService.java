package ru.sgp.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.controller.UserController;
import ru.sgp.dto.UserDTO;
import ru.sgp.model.*;
import ru.sgp.repository.*;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    private FilialRepository filialRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private RoleRepository roleRepository;
    Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private CommendantsRepository commendantsRepository;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    public UserDTO getCurrent() {
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        UserDTO userDTO = new UserDTO();
        User user = userRepository.findByUsername(username);
        logger.info("USERNAME " + username);
        if (user == null) return userDTO;
        userDTO.setRoleId(user.getRole().getId());
        if (user.getEmployee() != null) {
            userDTO.setFilialId(filialRepository.findByCode(user.getEmployee().getIdFilial()).getId());
        }
        userDTO.setFio(user.getEmployee().getLastname() + " " + user.getEmployee().getFirstname().charAt(0) + ". " + user.getEmployee().getSecondName().charAt(0) + ".");
        return userDTO;
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

    @Transactional
    public List<UserDTO> getAll() {
        List<UserDTO> response = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            Employee employee = user.getEmployee();
            Filial filial = filialRepository.findByCode(employee.getIdFilial());
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setRoleId(user.getRole().getId());
            userDTO.setRoleName(user.getRole().getUsername());
            userDTO.setTabnum(employee.getTabnum());
            userDTO.setFio(employee.getLastname() + " " + employee.getFirstname() + " " + employee.getSecondName());
            if (user.getRole().getId() == 2L) {
                List<Long> hotelsIds = new ArrayList<>();
                for (Commendant commendant : commendantsRepository.findAllByUser(user)) {
                    userDTO.setFilial(commendant.getHotel().getFilial().getName());
                    userDTO.setFilialId(commendant.getHotel().getFilial().getId());
                    hotelsIds.add(commendant.getHotel().getId());
                }
                userDTO.setHotels(hotelsIds);
            } else {
                userDTO.setFilial(filial.getName());
                userDTO.setFilialId(filial.getId());
            }
            response.add(userDTO);
        }
        return response;
    }

    @Transactional
    public UserDTO create(UserDTO userDTO) {
        User user = new User();
        Employee employee = employeeRepository.findByTabnum(userDTO.getTabnum());
        if (userRepository.findByEmployee(employee) == null) {
            Role role = roleRepository.getById(userDTO.getRoleId());
            user.setEmployee(employee);
            user.setUsername(userDTO.getUsername());
            user.setRole(role);
            if (role.getId() == 2L) {
                userDTO.getHotels().forEach(id -> {
                    Commendant commendant = new Commendant();
                    Hotel hotel = hotelRepository.getById(id);
                    commendant.setHotel(hotel);
                    commendant.setUser(user);
                    commendantsRepository.save(commendant);
                });
            }
            if (role.getId() == 3L) {
                Filial filial = filialRepository.findByCode(employee.getIdFilial());
                hotelRepository.findAllByFilial(filial).forEach(hotel -> {
                    Commendant commendant = new Commendant();
                    commendant.setHotel(hotel);
                    commendant.setUser(user);
                    commendantsRepository.save(commendant);
                });
            }
            userRepository.save(user);
            userDTO.setId(user.getId());
        }
        return userDTO;
    }

    @Transactional
    public UserDTO update(UserDTO userDTO) {
        User user = userRepository.getById(userDTO.getId());
        commendantsRepository.deleteAllByUser(user);
        Employee employee = employeeRepository.findByTabnum(userDTO.getTabnum());
        Role role = roleRepository.getById(userDTO.getRoleId());
        user.setEmployee(employee);
        user.setUsername(userDTO.getUsername());
        user.setRole(role);
        if (role.getId() == 2L) {
            userDTO.getHotels().forEach(id -> {
                Commendant commendant = new Commendant();
                Hotel hotel = hotelRepository.getById(id);
                commendant.setHotel(hotel);
                commendant.setUser(user);
                commendantsRepository.save(commendant);
            });
        }
        if (role.getId() == 3L) {
            Filial filial = filialRepository.findByCode(employee.getIdFilial());
            hotelRepository.findAllByFilial(filial).forEach(hotel -> {
                Commendant commendant = new Commendant();
                commendant.setHotel(hotel);
                commendant.setUser(user);
                commendantsRepository.save(commendant);
            });
        }
        userRepository.save(user);
        return userDTO;
    }

    @Transactional
    public byte[] getCheckoutReport(Long guestId, Integer roomNumber, String periodStartStr, String periodEndStr) throws JRException, ParseException {
        Date dateStart = dateTimeFormatter.parse(periodStartStr);
        Date dateFinish = dateTimeFormatter.parse(periodEndStr);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        Guest guest = guestRepository.getById(guestId);
        Room room = guest.getBed().getRoom();
        Flat flat = room.getFlat();
        Hotel hotel = flat.getHotel();
        Filial filial = hotel.getFilial();
        JasperReport jasperReport;
        if (filial.getId() == 930L)
            jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/CheckoutReportUEZS.jrxml"));
        else
            jasperReport = JasperCompileManager.compileReport(JRLoader.getResourceInputStream("reports/CheckoutReportGeneral.jrxml"));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("filial", filial.getName());
        parameters.put("hotelName", hotel.getName());
        parameters.put("hotelLocation", hotel.getLocation());
        parameters.put("reportNumber", filial.getCode().toString() + "/" + guest.getId().toString()); // 1930 + id guest
        parameters.put("reportDate", dateFormat.format(new Date()));
        parameters.put("fioFull", guest.getLastname() + " " + guest.getFirstname() + " " + guest.getSecondName());
        parameters.put("fio", guest.getLastname() + " " + guest.getFirstname().charAt(0) + ". " + guest.getSecondName().charAt(0) + ".");
        if (guest.getEmployee() != null) {
            parameters.put("tabnum", guest.getEmployee().getTabnum().toString());
        } else parameters.put("tabnum", "");
        if (guest.getEmployee() != null) {
            Filial guestFilial = filialRepository.findByCode(guest.getEmployee().getIdFilial());
            String guestJob = guestFilial.getName();
            parameters.put("guestJob", guestJob);
        } else {
            if (guest.getOrganization() != null)
            parameters.put("guestJob", guest.getOrganization().getName());
            else parameters.put("guestJob", "");
        }
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        User user = userRepository.findByUsername(username);
        Employee respEmp = user.getEmployee();
        parameters.put("respName", respEmp.getLastname() + " " + respEmp.getFirstname().charAt(0) + ". " + respEmp.getSecondName().charAt(0) + ".");
        parameters.put("roomNumber", flat.getName());
        parameters.put("korpusNumber", "");
        parameters.put("checkInDate", dateFormat.format(dateStart));
        parameters.put("checkInTime", timeFormat.format(dateStart));
        parameters.put("checkOutDate", dateFormat.format(dateFinish));
        parameters.put("checkOutTime", timeFormat.format(dateFinish));
        if (dateFormat.format(guest.getDateFinish()).equals(dateFormat.format(dateFinish))) {
            Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(dateStart));
            Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(dateFinish));
            String daysCount = String.valueOf(TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS));
            if (filial.getId() == 930L) {
                Double hoursCount = (double) TimeUnit.MINUTES.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS) / 60;
                parameters.put("daysCount", String.valueOf((int)(hoursCount /6)*0.25));
            } else
                parameters.put("daysCount", daysCount.equals("0") ? "1" : daysCount);
        } else {
            Date cuttedGuestStartDate = dateFormatter.parse(dateTimeFormatter.format(dateStart));
            Date cuttedGuestFinishDate = dateFormatter.parse(dateTimeFormatter.format(dateFinish));
            String daysCount = String.valueOf(TimeUnit.DAYS.convert(cuttedGuestFinishDate.getTime() - cuttedGuestStartDate.getTime(), TimeUnit.MILLISECONDS) + 1);
            if (filial.getId() == 930L) {
                Long hoursCount = TimeUnit.MINUTES.convert(dateFinish.getTime() - dateStart.getTime(), TimeUnit.MILLISECONDS);
                parameters.put("daysCount", String.valueOf((int)((double)hoursCount /6)*0.25));
            } else
                parameters.put("daysCount", daysCount.equals("0") ? "1" : daysCount);
        }
        parameters.put("date", dateFormat.format(new Date()));
        if (filial.getId() == 930L) {
            parameters.put("date", dateFormat.format(dateFinish));
            parameters.put("reportDate", dateFormat.format(dateFinish));
        }
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters);
        return export(jasperPrint);
    }

    @Transactional
    public UserDTO updateRole(Long roleId) {
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        UserDTO userDTO = new UserDTO();
        User user = userRepository.findByUsername(username);
        user.setRole(roleRepository.findById(roleId).get());
        userRepository.save(user);
        return userDTO;
    }
}