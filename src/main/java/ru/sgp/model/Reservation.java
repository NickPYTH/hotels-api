package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Table(name = "reservation", schema = "hotel")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tabnum")
    private Integer tabnum;

    @Column(name = "lastname")
    private String lastname;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "secondname")
    private String secondname;

    @Column(name = "date_start")
    private Date dateStart;

    @Column(name = "date_finish")
    private Date dateFinish;

    @Column(name = "date_start_confirmed")
    private Date dateStartConfirmed;

    @Column(name = "date_finish_confirmed")
    private Date dateFinishConfirmed;

    @OneToOne
    @JoinColumn(name = "bed")
    private Bed bed;

    @OneToOne
    @JoinColumn(name = "event")
    private Event event;

    @OneToOne
    @JoinColumn(name = "from_filial")
    private Filial fromFilial;

    @Column(name = "note")
    private String note;

    @OneToOne
    @JoinColumn(name = "guest")
    private Guest guest;

    @OneToOne
    @JoinColumn(name = "status")
    private ReservationStatus status;

}
