package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Employee;
import ru.sgp.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String currentUser);

    User findByEmployee(Employee employee);

}
