package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.LogDTO;
import ru.sgp.repository.LogRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogService {
    @Autowired
    LogRepository logRepository;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    @Transactional
    public List<LogDTO> getAll() {
        List<LogDTO> response = new ArrayList<>();
        logRepository.findTop2000ByOrderByIdDesc().forEach(log -> {
            LogDTO dto = new LogDTO();
            dto.setId(log.getId());
            dto.setStatus(log.getStatus());
            dto.setUser(log.getUser());
            dto.setPath(log.getPath());
            dto.setDuration(log.getDuration());
            dto.setMessage(log.getMessage());
            dto.setDate(dateTimeFormatter.format(log.getDate()));
            response.add(dto);
        });
        return response;
    }
}