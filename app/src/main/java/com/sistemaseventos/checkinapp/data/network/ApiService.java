package com.sistemaseventos.checkinapp.data.network;

import com.sistemaseventos.checkinapp.data.network.dto.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("auth/login")
    Call<String> login(@Body LoginRequest request);

    @GET("users/search")
    Call<UserResponse> findUserByCpf(@Query("cpf") String cpf);

    @POST("auth/register")
    Call<UserResponse> registerSimpleUser(@Body SimpleUserRequest request);

    @POST("users/sync")
    Call<UserResponse> syncUser(@Body UserSyncDTO request);

    @GET("events")
    Call<List<EventResponse>> getAllEvents();

    // --- MUDANÇA PARA V3: /registrations ---

    @GET("registrations/users/{id}")
    Call<List<EnrollmentResponse>> getEnrollments(@Path("id") String userId);

    @POST("registrations")
    Call<EnrollmentResponse> createEnrollment(@Body CreateEnrollmentRequest request);

    @PATCH("registrations/{id}/check-in")
    Call<EnrollmentResponse> performCheckIn(@Path("id") String enrollmentId);

    @PATCH("registrations/{id}/cancel")
    Call<EnrollmentResponse> cancelRegistration(@Path("id") String enrollmentId);
}
