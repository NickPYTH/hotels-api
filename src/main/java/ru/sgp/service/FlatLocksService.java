package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.FlatLocksDTO;
import ru.sgp.model.Flat;
import ru.sgp.model.FlatLocks;
import ru.sgp.repository.FlatLocksRepository;
import ru.sgp.repository.FlatRepository;
import ru.sgp.repository.StatusRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FlatLocksService {
    @Autowired
    FlatRepository flatRepository;
    @Autowired
    StatusRepository statusRepository;
    @Autowired
    FlatLocksRepository flatLocksRepository;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Transactional
    public FlatLocksDTO create(FlatLocksDTO flatLocksDTO) throws ParseException {
        if (flatLocksDTO.getId() == 1L) return flatLocksDTO;
        Date s1 = dateFormatter.parse(flatLocksDTO.getDateStart());
        Date f1 = dateFormatter.parse(flatLocksDTO.getDateFinish());
        Flat flat = flatRepository.getById(flatLocksDTO.getFlatId());
        FlatLocks flatLocks = new FlatLocks();
        List<FlatLocks> flatLocksList = flatLocksRepository.findAllByFlat(flat);
        for (FlatLocks flatLock : flatLocksList) {
            if (f1.before(flatLock.getDateStart()) || flatLock.getDateFinish().before(s1)) {

            } else {
                flatLocksDTO.setError("Date range error");
                return flatLocksDTO;
            }
        }
        flatLocks.setDateStart(dateFormatter.parse(flatLocksDTO.getDateStart()));
        flatLocks.setDateFinish(dateFormatter.parse(flatLocksDTO.getDateFinish()));
        flatLocks.setFlat(flatRepository.getById(flatLocksDTO.getFlatId()));
        flatLocks.setStatus(statusRepository.getById(flatLocksDTO.getStatusId()));
        flatLocksRepository.save(flatLocks);
        flatLocksDTO.setId(flatLocks.getId());
        return flatLocksDTO;
    }
    @Transactional
    public FlatLocksDTO update(FlatLocksDTO flatLocksDTO) throws ParseException {
        if (flatLocksDTO.getStatusId() == 1L) {
            flatLocksRepository.deleteById(flatLocksDTO.getId());
            return flatLocksDTO;
        }
        FlatLocks flatLocks = flatLocksRepository.getById(flatLocksDTO.getId());
        flatLocks.setDateStart(dateFormatter.parse(flatLocksDTO.getDateStart()));
        flatLocks.setDateFinish(dateTimeFormatter.parse(flatLocksDTO.getDateFinish() + " 23:59"));
        flatLocks.setFlat(flatRepository.getById(flatLocksDTO.getFlatId()));
        flatLocks.setStatus(statusRepository.getById(flatLocksDTO.getStatusId()));
        flatLocksRepository.save(flatLocks);
        return flatLocksDTO;
    }
    @Transactional
    public FlatLocksDTO get(Long id) {
        FlatLocks flatLock = flatLocksRepository.getById(id);
        FlatLocksDTO flatLocksDTO = new FlatLocksDTO();
        flatLocksDTO.setId(flatLock.getId());
        flatLocksDTO.setDateStart(dateFormatter.format(flatLock.getDateStart()));
        flatLocksDTO.setDateFinish(dateFormatter.format(flatLock.getDateFinish()));
        flatLocksDTO.setFlatId(flatLock.getFlat().getId());
        flatLocksDTO.setStatusId(flatLock.getStatus().getId());
        return flatLocksDTO;
    }
    public List<FlatLocksDTO> getAllByFlat(Long flatId) {
        Flat flat = flatRepository.getById(flatId);
        List<FlatLocksDTO> response = new ArrayList<>();
        for (FlatLocks flatLocks : flatLocksRepository.findAllByFlat(flat)) {
            FlatLocksDTO flatLocksDTO = new FlatLocksDTO();
            flatLocksDTO.setId(flatLocks.getId());
            flatLocksDTO.setDateStart(dateFormatter.format(flatLocks.getDateStart()));
            flatLocksDTO.setDateFinish(dateFormatter.format(flatLocks.getDateFinish()));
            flatLocksDTO.setFlatId(flatId);
            flatLocksDTO.setStatusId(flatLocks.getStatus().getId());
            response.add(flatLocksDTO);
        }
        return response;
    }
    public Long delete(Long flatLockId) throws ParseException {
        flatLocksRepository.deleteById(flatLockId);
        return flatLockId;
    }
}