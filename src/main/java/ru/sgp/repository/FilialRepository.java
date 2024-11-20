package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Filial;

@Repository
public interface FilialRepository extends JpaRepository<Filial, Long> {

    Filial findByCode(int l);

    Filial findByName(String filial);
}
