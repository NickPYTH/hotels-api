package ru.sgp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sgp.dto.HistoryDTO;
import ru.sgp.service.HistoryService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/history")
public class HistoryController {
    @Autowired
    private HistoryService historyService;
    @GetMapping(path = "/getEntityHistory")
    public ResponseEntity<List<HistoryDTO>> getGuestHistory(@RequestParam Long entityId, @RequestParam String entityType) {
        return new ResponseEntity<>(historyService.getGuestHistory(entityId, entityType), HttpStatus.OK);
    }
}
