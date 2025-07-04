package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Employee;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Employee findByTabnumAndEndDate(Integer tabnum, String endDate);

    List<Employee> findAllByLastnameAndFirstnameAndSecondNameAndEndDate(String lastname, String firstname, String secondName, String endDate);

    Employee findByMvzId(String id);
}
