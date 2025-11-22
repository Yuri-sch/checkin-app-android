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
    @POST("users/sync")
    Call<UserResponse> syncOfflineUser(@Body UserSyncDTO dto);

    @GET("users/search")
    Call<UserResponse> findUserByCpf(@Query("cpf") String cpf);

    @POST("users/register-simple")
    Call<UserResponse> registerSimpleUser(@Body SimpleUserRequest request);

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
