package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Commendant;
import ru.sgp.model.User;

import java.util.List;

@Repository
public interface CommendantsRepository extends JpaRepository<Commendant, Long> {

    List<Commendant> findAllByUser(User user);

    void deleteAllByUser(User user);
}
