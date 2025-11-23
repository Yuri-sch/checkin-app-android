package com.sistemaseventos.checkinapp.data.network;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sistemaseventos.checkinapp.data.manager.SessionManager;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://192.168.0.105:8080/"; // CONFIRA SEU IP

    private static Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context);

            // CORREÇÃO DA DATA: Adicionado .SSSX para ler milissegundos e fuso (ex: .822Z)
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .setLenient()
                    .create();

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(sessionManager))
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
