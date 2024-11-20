package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "commendants", schema = "hotel")
public class Commendant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "hotel")
    private Hotel hotel;

    @OneToOne
    @JoinColumn(name = "userc")
    private User user;

}
