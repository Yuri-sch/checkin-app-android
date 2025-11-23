package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.UserDao;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.UserResponse;
import com.sistemaseventos.checkinapp.data.network.dto.UserSyncDTO;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class UserRepository {
    private UserDao userDao;
    private ApiService apiService;
    private static final String TAG = "UserRepository";

    public UserRepository(Context context) {
        this.userDao = AppDatabase.getInstance(context).userDao();
        this.apiService = RetrofitClient.getApiService(context);
    }

    public UserEntity findUserByCpf(String cpf) {
        try {
            Response<UserResponse> response = apiService.findUserByCpf(cpf).execute();
            if (response.isSuccessful() && response.body() != null) {
                // Online: Recebeu dados, salva no cache local
                UserEntity user = mapResponseToEntity(response.body());
                user.isSynced = true;
                userDao.upsert(user);
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: buscando local", e);
        }
        // Offline: Busca no banco local
        return userDao.findByCpf(cpf);
    }

    public UserEntity registerSimpleUser(String cpf, String email) {
        // Usa o DTO de Sync que o backend espera
        UserSyncDTO request = new UserSyncDTO(cpf, email, "Participante (Novo)");

        try {
            Response<UserResponse> response = apiService.syncOfflineUser(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                // Sucesso Online
                UserEntity user = mapResponseToEntity(response.body());
                user.isSynced = true;
                userDao.upsert(user);
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: salvando localmente", e);
        }

        // Sucesso Offline (Cria temporário)
        UserEntity offlineUser = new UserEntity();
        offlineUser.id = UUID.randomUUID().toString();
        offlineUser.cpf = cpf;
        offlineUser.email = email;
        offlineUser.fullname = "Novo Usuário (Sincronizando...)";
        offlineUser.complete = false;
        offlineUser.isSynced = false; // Marca para sync

        userDao.upsert(offlineUser);
        return offlineUser;
    }

    // Sincronização (Chamado pelo Worker)
    public void syncPendingUsers() {
        List<UserEntity> pending = userDao.getUnsyncedUsers();
        for (UserEntity u : pending) {
            try {
                UserSyncDTO req = new UserSyncDTO(u.cpf, u.email, u.fullname);
                Response<UserResponse> res = apiService.syncOfflineUser(req).execute();

                if (res.isSuccessful() && res.body() != null) {
                    u.isSynced = true;
                    if (res.body().id != null) u.id = res.body().id; // Atualiza ID real
                    userDao.upsert(u);
                    Log.d(TAG, "Usuário sincronizado: " + u.cpf);
                }
            } catch (Exception e) {
                Log.e(TAG, "Falha ao sincronizar usuário: " + u.cpf);
            }
        }
    }

    private UserEntity mapResponseToEntity(UserResponse response) {
        UserEntity entity = new UserEntity();
        entity.id = response.id;
        entity.cpf = response.cpf;
        entity.fullname = response.fullname;
        entity.email = response.email;
        entity.birthDate = response.birthDate;
        entity.complete = response.complete;
        return entity;
    }
}
