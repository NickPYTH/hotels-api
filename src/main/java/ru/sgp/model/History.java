package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "history", schema = "hotel")
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entityType")
    private String entityType;

    @Column(name = "entityId")
    private Long entityId;

    @OneToOne
    @JoinColumn(name = "request")
    private Log request;

    @Column(name = "state_before")
    private String stateBefore;

    @Column(name = "state_after")
    private String stateAfter;

}
