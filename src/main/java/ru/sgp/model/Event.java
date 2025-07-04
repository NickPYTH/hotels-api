package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Table(name = "events", schema = "hotel")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_start")
    private Date dateStart;

    @Column(name = "date_finish")
    private Date dateFinish;

    @OneToOne
    @JoinColumn(name = "eventkind")
    private EventKind kind;

    @OneToOne
    @JoinColumn(name = "hotel")
    private Hotel hotel;

}
