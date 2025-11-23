package com.sistemaseventos.checkinapp.data.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class UserResponse {
    public String id;
    public String cpf;
    public String fullname;
    public String email;

    @SerializedName("birth_date") // Mapeia o JSON "birth_date" para o Java "birthDate"
    public Date birthDate;

    public boolean complete;
}
