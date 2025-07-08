package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "dict_post_staff", schema = "hotel")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_post")
    private Integer idPost;

    @Column(name = "id_department")
    private Integer idDepartment;

    @Column(name = "name")
    private String name;

    @Column(name = "longname")
    private String longname;

    @Column(name = "persk")
    private String persk;

}
