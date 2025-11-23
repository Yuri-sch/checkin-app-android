package com.sistemaseventos.checkinapp.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentWithEvent;
import java.util.List;
import java.util.Date;

@Dao
public interface EnrollmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<EnrollmentEntity> enrollments);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(EnrollmentEntity enrollment);

    @Delete
    void delete(EnrollmentEntity enrollment);

    // Método antigo (mantido por compatibilidade se necessário)
    @Query("SELECT * FROM enrollments WHERE usersId = :userId")
    List<EnrollmentEntity> getEnrollmentsForUser(String userId);

    // --- NOVO MÉTODO COM JOIN ---
    @Transaction // Necessário para @Relation
    @Query("SELECT * FROM enrollments WHERE usersId = :userId")
    List<EnrollmentWithEvent> getEnrollmentsWithEvents(String userId);

    @Query("DELETE FROM enrollments WHERE usersId = :userId AND isSynced = 1")
    void deleteSyncedEnrollmentsForUser(String userId);

    @Query("SELECT * FROM enrollments WHERE isSynced = 0")
    List<EnrollmentEntity> getUnsyncedEnrollments();

    @Query("UPDATE enrollments SET checkIn = :date, isSynced = :synced WHERE id = :id")
    void updateCheckInStatus(String id, Date date, boolean synced);

    @Query("UPDATE enrollments SET usersId = :newUserId WHERE usersId = :oldUserId")
    void migrateUserEnrollments(String oldUserId, String newUserId);

    // Novo método para cancelar localmente (apenas marca status)
    @Query("UPDATE enrollments SET status = 'CANCELED', isSynced = :synced WHERE id = :id")
    void cancelEnrollmentStatus(String id, boolean synced);
}
