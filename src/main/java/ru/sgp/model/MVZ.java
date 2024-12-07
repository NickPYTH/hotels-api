package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "mvz", schema = "hotel")
public class MVZ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employeeTab")
    private String employeeTab;

    @Column(name = "employeeFio")
    private String employeeFio;

    @Column(name = "mvz")
    private String mvz;

    @Column(name = "mvz_name")
    private String mvzName;

    @OneToOne
    @JoinColumn(name = "filial")
    private Filial filial;

    @Column(name = "organization")
    private String organization;

}
