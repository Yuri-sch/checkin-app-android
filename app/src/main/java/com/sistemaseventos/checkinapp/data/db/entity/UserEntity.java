package com.sistemaseventos.checkinapp.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import java.util.Date;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String cpf;
    public String fullname;
    public String email;

    public Date birthDate; // Campo de data de nascimento
    public boolean complete;

    // Campo de controle de sincronização
    public boolean isSynced = true;
}
