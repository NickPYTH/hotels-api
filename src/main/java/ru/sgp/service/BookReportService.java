package ru.sgp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sgp.dto.integration.BookReportDTO;
import ru.sgp.model.BookReport;
import ru.sgp.model.Employee;
import ru.sgp.model.RecordBookReport;
import ru.sgp.repository.BookReportRepository;
import ru.sgp.repository.EmployeeRepository;
import ru.sgp.repository.RecordBookReportRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookReportService {
    @Autowired
    BookReportRepository bookReportRepository;
    @Autowired
    RecordBookReportRepository recordBookReportRepository;
    @Autowired
    EmployeeRepository employeeRepository;

    @Transactional
    public List<BookReportDTO> getAll() {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        List<BookReportDTO> response = new ArrayList<>();
        for (BookReport bookReport : bookReportRepository.findAll()) {
            for (RecordBookReport recordBookReport : recordBookReportRepository.findAllByBookReport(bookReport)) {
                BookReportDTO bookReportDTO = new BookReportDTO();
                bookReportDTO.setBookId(bookReport.getId().toString());
                bookReportDTO.setWithBook(bookReport.getWithBook());
                bookReportDTO.setDate(dateTimeFormatter.format(bookReport.getDate()));
                bookReportDTO.setUsername(bookReport.getUsername());
                bookReportDTO.setDuration(bookReport.getDuration().toString());
                bookReportDTO.setBookStatus(bookReport.getStatus());
                bookReportDTO.setTabnumber(recordBookReport.getTabnumber().toString());
                Employee employee = employeeRepository.findByTabnumAndEndDate(recordBookReport.getTabnumber(), null);
                if (employee != null)
                    bookReportDTO.setFio(employee.getLastname() + " " + employee.getFirstname() + " " + employee.getSecondName());
                else bookReportDTO.setFio(recordBookReport.getFio());
                bookReportDTO.setDateStart(dateFormatter.format(recordBookReport.getDateStart()));
                bookReportDTO.setDateFinish(dateFormatter.format(recordBookReport.getDateFinish()));
                bookReportDTO.setStatus(recordBookReport.getStatus());
                response.add(bookReportDTO);
            }
        }
        return response;
    }
}