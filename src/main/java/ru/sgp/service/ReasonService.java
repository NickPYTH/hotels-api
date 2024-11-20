package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.ReasonDTO;
import ru.sgp.model.Reason;
import ru.sgp.repository.ReasonRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReasonService {
    @Autowired
    ReasonRepository reasonRepository;

    public List<ReasonDTO> getAll() {
        List<ReasonDTO> response = new ArrayList<>();
        for (Reason reason : reasonRepository.findAll()) {
            ReasonDTO reasonDTO = new ReasonDTO();
            reasonDTO.setId(reason.getId());
            reasonDTO.setName(reason.getName());
            reasonDTO.setIsDefault(reason.getIsDefault());
            response.add(reasonDTO);
        }
        return response;
    }

    public ReasonDTO get(Long id) {
        Reason reason = reasonRepository.getById(id);
        ReasonDTO reasonDTO = new ReasonDTO();
        reasonDTO.setId(reason.getId());
        reasonDTO.setName(reason.getName());
        reasonDTO.setIsDefault(reason.getIsDefault());
        return reasonDTO;
    }

    @Transactional
    public ReasonDTO update(ReasonDTO reasonDTO) {
        Reason reason = reasonRepository.getById(reasonDTO.getId());
        reason.setName(reasonDTO.getName());
        reason.setIsDefault(reasonDTO.getIsDefault());
        reasonRepository.save(reason);
        return reasonDTO;
    }

    @Transactional
    public ReasonDTO create(ReasonDTO reasonDTO) {
        Reason reason = new Reason();
        reason.setName(reasonDTO.getName());
        reason.setIsDefault(reasonDTO.getIsDefault());
        reasonRepository.save(reason);
        reasonDTO.setId(reason.getId());
        return reasonDTO;
    }

}