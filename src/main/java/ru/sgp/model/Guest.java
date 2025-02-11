package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Table(name = "guests", schema = "hotel")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @Column(name = "secondname")
    private String secondName;

    @Column(name = "note")
    private String note;

    @Column(name = "date_start")
    private Date dateStart;

    @Column(name = "date_finish")
    private Date dateFinish;

    @OneToOne
    @JoinColumn(name = "organization")
    private Organization organization;

    @Column(name = "regpomestu")
    private Boolean regPoMestu;

    @Column(name = "male")
    private Boolean male;

    @Column(name = "memo")
    private String memo;

    @Column(name = "checkouted")
    private Boolean checkouted;

    @OneToOne
    @JoinColumn(name = "contract")
    private Contract contract;

    @OneToOne
    @JoinColumn(name = "employee")
    private Employee employee;

    @OneToOne
    @JoinColumn(name = "bed")
    private Bed bed;

}
