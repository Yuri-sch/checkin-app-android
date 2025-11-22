package com.sistemaseventos.checkinapp.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users WHERE cpf = :cpf LIMIT 1")
    UserEntity findByCpf(String cpf);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserEntity user);

    // Este é o método que estava faltando na sua imagem
    @Query("SELECT * FROM users WHERE isSynced = 0")
    List<UserEntity> getUnsyncedUsers();
}
