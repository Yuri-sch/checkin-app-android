package com.sistemaseventos.checkinapp.data.network.dto;

public class UserSyncDTO {
    public String cpf;
    public String email;
    public String fullname;
    // Adicione outros campos se o seu backend exigir na sincronização

    public UserSyncDTO(String cpf, String email, String fullname) {
        this.cpf = cpf;
        this.email = email;
        this.fullname = fullname;
    }
}
