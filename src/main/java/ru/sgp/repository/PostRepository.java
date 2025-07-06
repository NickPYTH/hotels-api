package ru.sgp.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sgp.model.Post;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}
