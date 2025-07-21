package ru.sgp.dto.integration;

import lombok.Data;

@Data
public class BookReportDTO {
    private String bookId;
    private String date;
    private String username;
    private String duration;
    private String bookStatus;
    private String tabnumber;
    private String fio;
    private String dateStart;
    private String dateFinish;
    private String status;
    private Boolean withBook;
}
