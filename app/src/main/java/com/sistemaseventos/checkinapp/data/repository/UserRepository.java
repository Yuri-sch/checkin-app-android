package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.UserDao;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.SimpleUserRequest;
import com.sistemaseventos.checkinapp.data.network.dto.UserResponse;
import java.util.List;
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
                UserEntity user = mapResponseToEntity(response.body());
                user.isSynced = true;
                userDao.upsert(user);
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: buscando local", e);
        }
        return userDao.findByCpf(cpf);
    }

    public UserEntity registerSimpleUser(String cpf, String email) {
        SimpleUserRequest request = new SimpleUserRequest(cpf, email);
        try {
            Response<UserResponse> response = apiService.registerSimpleUser(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                UserEntity user = mapResponseToEntity(response.body());
                user.isSynced = true;
                userDao.upsert(user);
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "Offline: salvando localmente", e);
        }

        // Lógica Offline
        UserEntity offlineUser = new UserEntity();
        offlineUser.id = java.util.UUID.randomUUID().toString();
        offlineUser.cpf = cpf;
        offlineUser.email = email;
        offlineUser.fullname = "Novo Usuário (Sincronizando...)";
        offlineUser.complete = false;
        offlineUser.isSynced = false;

        userDao.upsert(offlineUser);
        return offlineUser;
    }

    // Método chamado pelo SyncWorker
    public void syncPendingUsers() {
        // Agora este método existe no DAO!
        List<UserEntity> pending = userDao.getUnsyncedUsers();

        for (UserEntity u : pending) {
            try {
                SimpleUserRequest req = new SimpleUserRequest(u.cpf, u.email);
                Response<UserResponse> res = apiService.registerSimpleUser(req).execute();
                if (res.isSuccessful() && res.body() != null) {
                    u.isSynced = true;
                    if (res.body().id != null) u.id = res.body().id;
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

        // Agora response.birthDate existe no DTO e entity.birthDate existe na Entidade!
        entity.birthDate = response.birthDate;

        entity.complete = response.complete;
        return entity;
    }
}
