package ru.sgp.service;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
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
import java.util.ArrayList;
import java.util.List;

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
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private CommendantsRepository commendantsRepository;
    Logger logger = LoggerFactory.getLogger(UserController.class);
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
    public UserDTO updateRole(Long roleId) {
        String username = ru.sgp.utils.SecurityManager.getCurrentUser();
        UserDTO userDTO = new UserDTO();
        User user = userRepository.findByUsername(username);
        user.setRole(roleRepository.findById(roleId).get());
        userRepository.save(user);
        return userDTO;
    }
}