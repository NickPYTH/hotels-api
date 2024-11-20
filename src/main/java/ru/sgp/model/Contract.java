package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "contracts", schema = "hotel")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "filial")
    private Filial filial;

    @OneToOne
    @JoinColumn(name = "hotel")
    private Hotel hotel;

    @OneToOne
    @JoinColumn(name = "organization")
    private Organization organization;

    @Column(name = "docnum")
    private String docnum;

    @Column(name = "billing")
    private String billing;

    @OneToOne
    @JoinColumn(name = "reason")
    private Reason reason;

    @Column(name = "cost")
    private Float cost;

    @Column(name = "note")
    private String note;

}
