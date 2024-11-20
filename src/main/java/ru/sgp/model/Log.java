package ru.sgp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Table(name = "logs", schema = "hotel")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status")
    private String status;

    @Column(name = "userl")
    private String user;

    @Column(name = "path")
    private String path;

    @Column(name = "duration")
    private Double duration;

    @Column(name = "message")
    private String message;

    @Column(name = "date")
    private Date date;

}
