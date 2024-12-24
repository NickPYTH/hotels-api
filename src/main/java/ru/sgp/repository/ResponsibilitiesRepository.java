package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Hotel;
import ru.sgp.model.Responsibilities;

import java.util.List;

@Repository
public interface ResponsibilitiesRepository extends JpaRepository<Responsibilities, Long> {
    List<Responsibilities> findAllByHotel(Hotel hotel);

    void deleteAllByHotel(Hotel h);
}
