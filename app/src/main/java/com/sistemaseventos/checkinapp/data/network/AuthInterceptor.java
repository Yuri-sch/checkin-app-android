package com.sistemaseventos.checkinapp.data.network;

import com.sistemaseventos.checkinapp.data.manager.SessionManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = sessionManager.getToken();

        if (token != null && !token.isEmpty()) {
            // Adiciona o cabeçalho Authorization: Bearer <token>
            Request modified = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(modified);
        }
        return chain.proceed(original);
    }
}
