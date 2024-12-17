package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "rooms", schema = "hotel")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "beds_count")
    private Integer bedsCount;

//    @OneToOne  // todo delete statuses
//    @JoinColumn(name = "status")
//    private Status status;

    @OneToOne
    @JoinColumn(name = "flat")
    private Flat flat;

    @Column(name = "note")
    private String note;

    @Column(name = "description")
    private String description;

}
