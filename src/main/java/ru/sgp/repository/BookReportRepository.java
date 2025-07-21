package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.BookReport;

@Repository
public interface BookReportRepository extends JpaRepository<BookReport, Long> {

}
