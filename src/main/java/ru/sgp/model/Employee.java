package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "dict_employee", schema = "hotel")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_filial")
    private Integer idFilial;

    @Column(name = "id_department")
    private Integer idDepartment;

    @Column(name = "id_post")
    private Integer idPost;

    @Column(name = "id_poststaff")
    private Integer idPoststaff;

    @Column(name = "tabnum")
    private Integer tabnum;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "secondName")
    private String secondName;

    @Column(name = "lastname")
    private String lastname;

}
