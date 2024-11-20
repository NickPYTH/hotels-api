package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

}
