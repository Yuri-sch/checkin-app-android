package com.sistemaseventos.checkinapp.data.network;

import com.sistemaseventos.checkinapp.data.network.dto.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // Login: Retorna String pura (Token)
    @POST("auth/login")
    Call<String> login(@Body LoginRequest request);

    // Busca por CPF: Retorna UserResponse
    // AVISO: O backend DEVE ter este endpoint implementado.
    @GET("users/search")
    Call<UserResponse> findUserByCpf(@Query("cpf") String cpf);

    // Cadastro Rápido (Sync): Usa a rota de sync do UserController
    @POST("users/sync")
    Call<UserResponse> syncOfflineUser(@Body UserSyncDTO dto);

    // --- EVENTOS ---
    @GET("events")
    Call<List<EventResponse>> getAllEvents();

    // --- INSCRIÇÕES ---
    @GET("enrollments")
    Call<List<EnrollmentResponse>> getEnrollments(@Query("userId") String userId);

    @POST("enrollments")
    Call<EnrollmentResponse> createEnrollment(@Body CreateEnrollmentRequest request);

    @POST("enrollments/{id}/checkin")
    Call<Void> performCheckIn(@Path("id") String enrollmentId);
}
