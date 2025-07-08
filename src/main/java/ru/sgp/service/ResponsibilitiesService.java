package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.ResponsibilitiesDTO;
import ru.sgp.model.Employee;
import ru.sgp.model.Filial;
import ru.sgp.model.Hotel;
import ru.sgp.model.Responsibilities;
import ru.sgp.repository.EmployeeRepository;
import ru.sgp.repository.FilialRepository;
import ru.sgp.repository.HotelRepository;
import ru.sgp.repository.ResponsibilitiesRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResponsibilitiesService {
    @Autowired
    ResponsibilitiesRepository responsibilitiesRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    HotelRepository hotelRepository;
    @Transactional
    public ResponsibilitiesDTO create(ResponsibilitiesDTO responsibilitiesDTO) {
        Responsibilities responsibility = new Responsibilities();
        Hotel hotel = hotelRepository.getById(responsibilitiesDTO.getHotelId());
        Employee employee = employeeRepository.findByTabnumAndEndDate(responsibilitiesDTO.getTabnum(), null);
        responsibility.setEmployee(employee);
        responsibility.setHotel(hotel);
        responsibilitiesRepository.save(responsibility);
        return responsibilitiesDTO;
    }
    @Transactional
    public ResponsibilitiesDTO update(ResponsibilitiesDTO responsibilitiesDTO) {
        Responsibilities responsibility = responsibilitiesRepository.getById(responsibilitiesDTO.getId());
        Employee employee = employeeRepository.findByTabnumAndEndDate(responsibilitiesDTO.getTabnum(), null);
        Hotel hotel = hotelRepository.getById(responsibilitiesDTO.getHotelId());
        responsibility.setEmployee(employee);
        responsibility.setHotel(hotel);
        responsibilitiesRepository.save(responsibility);
        return responsibilitiesDTO;
    }
    public ResponsibilitiesDTO get(Long id) {
        Responsibilities responsibility = responsibilitiesRepository.getById(id);
        ResponsibilitiesDTO responsibilitiesDTO = new ResponsibilitiesDTO();
        responsibilitiesDTO.setId(responsibility.getId());
        responsibilitiesDTO.setEmployeeId(responsibility.getEmployee().getId());
        responsibilitiesDTO.setTabnum(responsibility.getEmployee().getTabnum());
        return responsibilitiesDTO;
    }
    public List<ResponsibilitiesDTO> getAll() {
        List<ResponsibilitiesDTO> response = new ArrayList<>();
        for (Responsibilities responsibility : responsibilitiesRepository.findAll()) {
            ResponsibilitiesDTO responsibilitiesDTO = new ResponsibilitiesDTO();
            responsibilitiesDTO.setId(responsibility.getId());
            responsibilitiesDTO.setEmployeeId(responsibility.getEmployee().getId());
            responsibilitiesDTO.setTabnum(responsibility.getEmployee().getTabnum());
            responsibilitiesDTO.setFio(responsibility.getEmployee().getLastname() + " " + responsibility.getEmployee().getFirstname() + " " + responsibility.getEmployee().getSecondName());
            responsibilitiesDTO.setHotel(responsibility.getHotel().getName());
            responsibilitiesDTO.setHotelId(responsibility.getHotel().getId());
            Filial filial = filialRepository.findByCode(responsibility.getEmployee().getIdFilial());
            responsibilitiesDTO.setFilial(filial.getName());
            responsibilitiesDTO.setFilialId(filial.getId());
            response.add(responsibilitiesDTO);
        }
        return response;
    }
    public List<ResponsibilitiesDTO> getAllByHotelId(Long hotelId) {
        List<ResponsibilitiesDTO> response = new ArrayList<>();
        Hotel hotel = hotelRepository.getById(hotelId);
        for (Responsibilities responsibility : responsibilitiesRepository.findAllByHotel(hotel)) {
            ResponsibilitiesDTO responsibilitiesDTO = new ResponsibilitiesDTO();
            responsibilitiesDTO.setId(responsibility.getId());
            responsibilitiesDTO.setEmployeeId(responsibility.getEmployee().getId());
            responsibilitiesDTO.setTabnum(responsibility.getEmployee().getTabnum());
            responsibilitiesDTO.setFio(responsibility.getEmployee().getLastname() + " " + responsibility.getEmployee().getFirstname() + " " + responsibility.getEmployee().getSecondName());
            responsibilitiesDTO.setHotel(responsibility.getHotel().getName());
            responsibilitiesDTO.setHotelId(responsibility.getHotel().getId());
            Filial filial = filialRepository.findByCode(responsibility.getEmployee().getIdFilial());
            responsibilitiesDTO.setFilial(filial.getName());
            responsibilitiesDTO.setFilialId(filial.getId());
            response.add(responsibilitiesDTO);
        }
        return response;
    }
}