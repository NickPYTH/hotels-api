package ru.sgp.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private Integer tabnum;
    private String fio;
    private String filial;
    private Long filialId;
    private Long roleId;
    private String roleName;
    private List<Long> hotels;
    private String customPost;
}
