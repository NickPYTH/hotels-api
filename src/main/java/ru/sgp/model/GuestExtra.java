package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "guests_extras", schema = "hotel")
public class GuestExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "guest")
    private Guest guest;

    @OneToOne
    @JoinColumn(name = "extra")
    private Extra extra;

}
