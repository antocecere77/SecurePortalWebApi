package com.portal.security;

import java.util.List;

import lombok.Data;

@Data
public class UtentiSecurity {
    private String id;
    private String userId;
    private String password;
    private String attivo;

    private List<String> ruoli;
}
