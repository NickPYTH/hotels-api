package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.BookReport;
import ru.sgp.model.RecordBookReport;

import java.util.List;

@Repository
public interface RecordBookReportRepository extends JpaRepository<RecordBookReport, Long> {

    List<RecordBookReport> findAllByBookReport(BookReport bookReport);
}
