package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Table(name = "flat_locks", schema = "hotel")
public class FlatLocks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_start")
    private Date dateStart;

    @Column(name = "date_finish")
    private Date dateFinish;

    @OneToOne
    @JoinColumn(name = "flat")
    private Flat flat;

    @OneToOne
    @JoinColumn(name = "status")
    private Status status;

}
