package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Table(name = "event", schema = "hotel")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @OneToOne
    @JoinColumn(name = "type")
    private EventType type;

    @Column(name = "date_start")
    private Date dateStart;

    @Column(name = "date_finish")
    private Date dateFinish;

    @OneToOne
    @JoinColumn(name = "hotel")
    private Hotel hotel;

    @Column(name = "man_count")
    private Integer manCount;

    @Column(name = "woman_count")
    private Integer womenCount;

}
