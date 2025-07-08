package ru.sgp.dto;

import lombok.Data;

@Data
public class EmployeeDTO {
    private Long id;
    private Integer idFilial;
    private Integer idDepartment;
    private Integer idPost;
    private Integer idPoststaff;
    private Integer tabnum;
    private String firstname;
    private String secondName;
    private String lastname;
}
