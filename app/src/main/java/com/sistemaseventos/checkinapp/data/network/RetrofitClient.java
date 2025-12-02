package com.sistemaseventos.checkinapp.data.network;

import android.content.Context;
import android.util.Log; // Importante
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sistemaseventos.checkinapp.data.manager.SessionManager;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor; // Importante
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://177.44.248.49:8080/";
    private static Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context);

            // 1. Configura o Logger
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message ->
                    Log.d("API_LOG", message) // Vamos usar a tag API_LOG para filtrar fácil
            );
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); // Mostra tudo (Headers + Body)

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .setLenient()
                    .create();

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(sessionManager))
                    .addInterceptor(logging) // 2. Adiciona o interceptor AQUI
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
