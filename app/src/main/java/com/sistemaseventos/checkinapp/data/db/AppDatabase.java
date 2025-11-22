package com.sistemaseventos.checkinapp.data.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

// Importe as novas DAOs e Entidades
import com.sistemaseventos.checkinapp.data.db.dao.EnrollmentDao;
import com.sistemaseventos.checkinapp.data.db.dao.EventDao;
import com.sistemaseventos.checkinapp.data.db.dao.UserDao;
import com.sistemaseventos.checkinapp.data.db.entity.EnrollmentEntity;
import com.sistemaseventos.checkinapp.data.db.entity.EventEntity;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;

// Adicione as novas entidades ao array @Database
@Database(entities = {UserEntity.class, EventEntity.class, EnrollmentEntity.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    // Adicione os novos métodos abstratos da DAO
    public abstract EventDao eventDao();
    public abstract EnrollmentDao enrollmentDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "checkin_database"
                            )
                            // Em desenvolvimento, permite que o Room destrua e recrie
                            // o banco se o schema (versão) mudar.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}