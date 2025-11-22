package com.sistemaseventos.checkinapp.data.network.dto;

public class SimpleUserRequest {
    public String cpf;
    public String email;

    public SimpleUserRequest(String cpf, String email) {
        this.cpf = cpf;
        this.email = email;
    }
}
