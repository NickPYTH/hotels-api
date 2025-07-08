package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "dict_kostl", schema = "hotel")
public class MVZ {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", insertable = false, updatable = false)
    private String name;

    @Column(name = "id_filial")
    private Long filialId;

    @Column(name = "note")
    private String organization;

}
