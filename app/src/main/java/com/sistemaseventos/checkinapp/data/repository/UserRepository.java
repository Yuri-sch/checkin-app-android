package com.sistemaseventos.checkinapp.data.repository;

import android.content.Context;
import android.util.Log;
import com.sistemaseventos.checkinapp.data.db.AppDatabase;
import com.sistemaseventos.checkinapp.data.db.dao.EnrollmentDao;
import com.sistemaseventos.checkinapp.data.db.dao.UserDao;
import com.sistemaseventos.checkinapp.data.db.entity.UserEntity;
import com.sistemaseventos.checkinapp.data.network.ApiService;
import com.sistemaseventos.checkinapp.data.network.RetrofitClient;
import com.sistemaseventos.checkinapp.data.network.dto.SimpleUserRequest;
import com.sistemaseventos.checkinapp.data.network.dto.UserResponse;
import com.sistemaseventos.checkinapp.data.network.dto.UserSyncDTO; // Importe o DTO correto
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class UserRepository {
    private UserDao userDao;
    private EnrollmentDao enrollmentDao;
    private ApiService apiService;
    private static final String TAG = "UserRepository";

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.userDao();
        this.enrollmentDao = db.enrollmentDao();
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
            Log.e(TAG, "Offline ou Erro API na busca: " + e.getMessage());
        }
        return userDao.findByCpf(cpf);
    }

    public UserEntity registerSimpleUser(String cpf, String email) {
        SimpleUserRequest request = new SimpleUserRequest(cpf, email);
        try {
            // Tenta cadastro online normal
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

        // Se falhar ou estiver offline, salva localmente
        UserEntity offlineUser = new UserEntity();
        offlineUser.id = UUID.randomUUID().toString(); // ID Temporário
        offlineUser.cpf = cpf;
        offlineUser.email = email;
        offlineUser.fullname = "Novo Usuário";
        offlineUser.complete = false;
        offlineUser.isSynced = false;

        userDao.upsert(offlineUser);
        return offlineUser;
    }

    // --- ATUALIZAÇÃO AQUI ---
    public void syncPendingUsers() throws Exception {
        List<UserEntity> pending = userDao.getUnsyncedUsers();
        if (pending.isEmpty()) {
            Log.d(TAG, "Nenhum usuário pendente para sincronizar.");
            return;
        }

        for (UserEntity u : pending) {
            String oldTempId = u.id;
            Log.d(TAG, "Tentando sincronizar usuário: " + u.cpf); // Log de início

            try {
                UserSyncDTO syncDto = new UserSyncDTO(u.cpf, u.email, u.fullname);
                Response<UserResponse> res = apiService.syncUser(syncDto).execute();

                if (res.isSuccessful() && res.body() != null) {
                    String realId = res.body().id;

                    // 1. Atualiza dados locais
                    u.isSynced = true;
                    u.id = realId;
                    if (res.body().fullname != null) u.fullname = res.body().fullname;

                    // 2. Migra inscrições do ID temporário para o real
                    enrollmentDao.migrateUserEnrollments(oldTempId, realId);

                    // 3. Verifica se precisa deletar o temporário antigo para evitar duplicata no Room
                    if (!oldTempId.equals(realId)) {
                        // Opcional: deletar o registro antigo se o ID mudou
                        // userDao.deleteById(oldTempId);
                    }
                    userDao.upsert(u);
                    Log.d(TAG, "SUCESSO: Usuário " + u.cpf + " sincronizado. Novo ID: " + realId);

                } else {
                    // *** AQUI ESTÁ O SEGREDO ***
                    // Isso vai mostrar no Logcat se é 404 (Rota não existe), 403 (Permissão) ou 500 (Erro no servidor)
                    String errorBody = res.errorBody() != null ? res.errorBody().string() : "Sem detalhes";
                    Log.e(TAG, "ERRO API (" + res.code() + ") ao sincronizar " + u.cpf + ": " + errorBody);
                }
            } catch (Exception e) {
                // Adicione o 'e' aqui para ver a exceção completa no Logcat
                Log.e(TAG, "EXCEÇÃO ao sincronizar " + u.cpf, e);
            }
        }
    }
    // ------------------------

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
