package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class UserResponse {
    public String id;
    public String cpf;
    public String fullname;
    public String email;

    // Este é o campo que estava faltando ou com nome diferente
    @SerializedName("birth_date")
    public Date birthDate;

    public boolean complete;
}
