package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Room;
import ru.sgp.model.RoomLocks;

import java.util.Date;
import java.util.List;

@Repository
public interface RoomLocksRepository extends JpaRepository<RoomLocks, Long> {

    List<RoomLocks> findAllByDateStartBeforeAndDateFinishAfterAndRoom(Date tmp, Date tmp1, Room room);

    List<RoomLocks> findAllByRoom(Room room);
}
