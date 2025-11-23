package com.sistemaseventos.checkinapp.data.network;

import com.sistemaseventos.checkinapp.data.network.dto.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // --- AUTENTICAÇÃO ---
    @POST("auth/login")
    Call<String> login(@Body LoginRequest request);

    // --- USUÁRIOS ---
    @GET("users/search")
    Call<UserResponse> findUserByCpf(@Query("cpf") String cpf);

    @POST("users/sync")
    Call<UserResponse> syncOfflineUser(@Body UserSyncDTO dto);

    @POST("auth/register") // Rota de cadastro público usada pelo app
    Call<UserResponse> registerSimpleUser(@Body SimpleUserRequest request);

    // --- EVENTOS ---
    @GET("events")
    Call<List<EventResponse>> getAllEvents();

    // --- INSCRIÇÕES (ROTAS ATUALIZADAS) ---

    // Listar por usuário: GET /registrations/users/{id}
    @GET("registrations/users/{id}")
    Call<List<EnrollmentResponse>> getEnrollments(@Path("id") String userId);

    // Criar inscrição: POST /registrations
    @POST("registrations")
    Call<EnrollmentResponse> createEnrollment(@Body CreateEnrollmentRequest request);

    // Fazer Check-in: PATCH /registrations/{id}/check-in
    @PATCH("registrations/{id}/check-in")
    Call<EnrollmentResponse> performCheckIn(@Path("id") String enrollmentId);
}
