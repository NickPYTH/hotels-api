package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.ReportNames;

@Repository
public interface ReportNamesRepository extends JpaRepository<ReportNames, Long> {

    ReportNames findByEnName(String enName);
}
