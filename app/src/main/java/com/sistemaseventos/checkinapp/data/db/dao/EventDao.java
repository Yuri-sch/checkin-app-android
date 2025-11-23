package com.sistemaseventos.checkinapp.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.sistemaseventos.checkinapp.data.db.entity.EventEntity;
import java.util.List;

@Dao
public interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<EventEntity> events);

    @Query("SELECT * FROM events")
    List<EventEntity> getAllEvents();

    @Query("SELECT * FROM events WHERE eventName LIKE :query")
    List<EventEntity> searchEventsByName(String query);

    // NOVO: Limpa apenas eventos que vieram da API (sincronizados)
    // Assim não apagamos os eventos criados offline que ainda vão subir
    @Query("DELETE FROM events WHERE isSynced = 1")
    void deleteSyncedEvents();
}
