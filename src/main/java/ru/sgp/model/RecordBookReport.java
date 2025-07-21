package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Table(name = "record_book_report", schema = "hotel")
public class RecordBookReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "book_report")
    private BookReport bookReport;

    @Column(name = "tabnumber")
    private Integer tabnumber;

    @Column(name = "fio")
    private String fio;

    @Column(name = "date_start")
    private Date dateStart;

    @Column(name = "date_finish")
    private Date dateFinish;

    @Column(name = "status")
    private String status;
}
