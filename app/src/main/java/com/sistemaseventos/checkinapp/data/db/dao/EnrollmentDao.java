package com.sistemaseventos.checkinapp.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
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

    @Query("SELECT * FROM enrollments WHERE usersId = :userId")
    List<EnrollmentEntity> getEnrollmentsForUser(String userId);

    @Query("DELETE FROM enrollments WHERE usersId = :userId")
    void deleteEnrollmentsForUser(String userId);

    @Query("SELECT * FROM enrollments WHERE isSynced = 0")
    List<EnrollmentEntity> getUnsyncedEnrollments();

    @Query("UPDATE enrollments SET checkIn = :date, isSynced = :synced WHERE id = :id")
    void updateCheckInStatus(String id, Date date, boolean synced);
}
