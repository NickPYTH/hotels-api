package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Flat;
import ru.sgp.model.Hotel;
import ru.sgp.model.Room;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByFlatOrderById(Flat flat);
    void deleteAllByFlatHotel(Hotel hotel);

    int countByFlatHotel(Hotel hotel);
}
