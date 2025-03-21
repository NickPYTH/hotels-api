package ru.sgp.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.MVZDTO;
import ru.sgp.model.Employee;
import ru.sgp.model.MVZ;
import ru.sgp.repository.EmployeeRepository;
import ru.sgp.repository.FilialRepository;
import ru.sgp.repository.GuestRepository;
import ru.sgp.repository.MVZRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class MVZService {
    @Autowired
    MVZRepository mvzRepository;
    @Autowired
    FilialRepository filialRepository;
    @Autowired
    GuestRepository guestRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Transactional
    public MVZDTO update(MVZDTO MVZDTO) {
        ModelMapper modelMapper = new ModelMapper();
        mvzRepository.save(modelMapper.map(MVZDTO, MVZ.class));
        return MVZDTO;
    }
    public MVZDTO get(String id) {
        MVZ mvz = mvzRepository.findById(id);
        MVZDTO mvzDTO = new MVZDTO();
        Employee employee = employeeRepository.findByMvzId(mvz.getId());
        mvzDTO.setMvz(mvz.getId());
        mvzDTO.setMvzName(mvz.getName());
        mvzDTO.setOrganization(mvz.getOrganization());
        mvzDTO.setFilial(filialRepository.findByCode(Math.toIntExact(mvz.getFilialId())).getName());
        mvzDTO.setEmployeeTab(employee.getTabnum().toString());
        mvzDTO.setEmployeeFio(employee.getLastname() + " " + employee.getFirstname() + " " + employee.getSecondName());
        return mvzDTO;
    }
    public List<MVZDTO> getAll() {
        List<MVZDTO> result = new ArrayList<>();
        for (Employee employee : employeeRepository.findAll()) {
            MVZDTO mvzDTO = new MVZDTO();
            if (employee.getMvzId() != null && !employee.getMvzId().isEmpty()) {
                MVZ mvz = mvzRepository.findById(employee.getMvzId());
                mvzDTO.setMvz(mvz.getId());
                mvzDTO.setMvzName(mvz.getName());
                mvzDTO.setOrganization(mvz.getOrganization());
                mvzDTO.setFilial(filialRepository.findByCode(Math.toIntExact(mvz.getFilialId())).getName());
                mvzDTO.setEmployeeTab(employee.getTabnum().toString());
                mvzDTO.setEmployeeFio(employee.getLastname() + " " + employee.getFirstname() + " " + employee.getSecondName());
                result.add(mvzDTO);
            }
        }
        return result;
    }
}