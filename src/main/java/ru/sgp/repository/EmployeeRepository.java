package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Employee;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Employee findByTabnum(Integer tabnum);

    List<Employee> findAllByLastnameAndFirstnameAndSecondName(String lastname, String firstname, String secondName);

    Employee findByMvzId(String id);
}
