package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Bed;
import ru.sgp.model.Flat;
import ru.sgp.model.Hotel;
import ru.sgp.model.Room;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {
    Optional<Bed> findByRoomAndName(Room room, String bedName);
    List<Bed> findAllByRoom(Room room);
    Integer countByRoomFlat(Flat flat);
    Integer countByRoom(Room room);
    void deleteAllByRoomFlatHotel(Hotel hotel);

    List<Bed> findAllByRoomFlatHotel(Hotel hotel);
}
