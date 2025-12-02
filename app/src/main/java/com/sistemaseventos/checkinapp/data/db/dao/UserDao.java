package com.sistemaseventos.checkinapp.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
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

    @Query("SELECT * FROM users WHERE isSynced = 0")
    List<UserEntity> getUnsyncedUsers();

    @Delete
    void delete(UserEntity user);

    // ADICIONE ESTE MÉTODO:
    @Query("DELETE FROM users WHERE id = :id")
    void deleteById(String id);
}
