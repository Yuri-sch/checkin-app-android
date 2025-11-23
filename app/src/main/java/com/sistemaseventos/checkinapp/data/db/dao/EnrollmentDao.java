package com.sistemaseventos.checkinapp.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import java.util.List;

@Dao
public interface EnrollmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<EnrollmentEntity> enrollments);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(EnrollmentEntity enrollment);

    @Query("SELECT * FROM enrollments WHERE usersId = :userId")
    List<EnrollmentEntity> getEnrollmentsForUser(String userId);

    @Query("DELETE FROM enrollments WHERE usersId = :userId")
    void deleteEnrollmentsForUser(String userId);

    // O MÉTODO QUE FALTAVA: Busca tudo que não foi sincronizado (isSynced = 0 ou false)
    @Query("SELECT * FROM enrollments WHERE isSynced = 0")
    List<EnrollmentEntity> getUnsyncedEnrollments();

    // Adicione isto na Interface EnrollmentDao
    @androidx.room.Query("UPDATE enrollments SET checkIn = :date, isSynced = :synced WHERE id = :id")
    void updateCheckInStatus(String id, java.util.Date date, boolean synced);

    @androidx.room.Delete
    void delete(EnrollmentEntity enrollment);
}
