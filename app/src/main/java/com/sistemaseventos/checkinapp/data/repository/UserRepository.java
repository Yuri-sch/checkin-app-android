package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.EnrollmentDao; // Importante
import com.sistemaseventos.checkinapp.data.db.dao.UserDao;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.SimpleUserRequest;
import com.sistemaseventos.checkinapp.data.network.dto.UserResponse;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class UserRepository {
    private UserDao userDao;
    private EnrollmentDao enrollmentDao; // Adicionado
    private ApiService apiService;
    private static final String TAG = "UserRepository";

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
        this.enrollmentDao = db.enrollmentDao(); // Inicializa
        this.apiService = RetrofitClient.getApiService(context);
    }

    // ... (findUserByCpf permanece igual)
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
            Log.e(TAG, "Offline ou Erro API na busca: " + e.getMessage());
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
            Log.e(TAG, "Offline: salvando cadastro localmente", e);
        }

        UserEntity offlineUser = new UserEntity();
        offlineUser.id = UUID.randomUUID().toString(); // ID TEMPORÁRIO
        offlineUser.cpf = cpf;
        offlineUser.email = email;
        offlineUser.fullname = "Novo Usuário (Sincronizando...)";
        offlineUser.complete = false;
        offlineUser.isSynced = false;

        userDao.upsert(offlineUser);
        return offlineUser;
    }

    // AQUI ESTÁ A CORREÇÃO DA SINCRONIZAÇÃO
    public void syncPendingUsers() {
        List<UserEntity> pending = userDao.getUnsyncedUsers();
        for (UserEntity u : pending) {
            String oldTempId = u.id; // Guarda o ID temporário
            try {
                SimpleUserRequest req = new SimpleUserRequest(u.cpf, u.email);
                Response<UserResponse> res = apiService.registerSimpleUser(req).execute();

                if (res.isSuccessful() && res.body() != null) {
                    String realId = res.body().id; // ID real do servidor

                    // 1. Atualiza os dados do objeto usuário
                    u.isSynced = true;
                    u.id = realId;
                    if (res.body().fullname != null) u.fullname = res.body().fullname;

                    // 2. MIGRA AS INSCRIÇÕES (Salva os dados offline!)
                    // Troca o ID temporário pelo Real em todas as inscrições locais desse usuário
                    enrollmentDao.migrateUserEnrollments(oldTempId, realId);

                    // 3. Salva o usuário novo (com ID real)
                    userDao.upsert(u);

                    // 4. Remove o usuário temporário antigo para não duplicar
                    // (Precisamos de um método deleteById ou delete objeto na DAO)
                    // Como alteramos o ID do objeto 'u', precisamos criar um objeto dummy com o ID velho para deletar
                    UserEntity tempUser = new UserEntity();
                    tempUser.id = oldTempId;
                    // userDao.delete(tempUser); // Adicione delete na UserDao se não tiver

                    Log.d(TAG, "Usuário sincronizado e migrado: " + u.cpf);
                }
            } catch (Exception e) {
                Log.e(TAG, "Falha sync user: " + u.cpf);
            }
        }
    }

    // ... (mapResponseToEntity permanece igual)
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
