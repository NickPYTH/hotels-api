package ru.sgp.dto;

import lombok.Data;

@Data
public class HistoryDTO {
    private Long id;
    private String entityType;
    private Long entityId;
    private LogDTO request;
    private String stateBefore;
    private String stateAfter;
}
