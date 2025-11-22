package com.sistemaseventos.checkinapp.data.network;

import android.content.Context;
import com.sistemaseventos.checkinapp.data.manager.SessionManager;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    // IMPORTANTE: Coloque o IP correto do seu computador aqui (porta 8080)
    // Se for emulador: "http://10.0.2.2:8080/"
    // Se for dispositivo físico: "http://192.168.X.X:8080/"
    private static final String BASE_URL = "http://192.168.0.105:8080/";

    private static Retrofit retrofit;

    // Método que exige Contexto para configurar a Autenticação
    public static ApiService getApiService(Context context) {
        // Sempre recria o cliente se não existir, ou se precisarmos garantir o contexto novo
        if (retrofit == null) {
            // 1. Configura o Gerenciador de Sessão
            SessionManager sessionManager = new SessionManager(context);

            // 2. Configura o Cliente HTTP com o Interceptor (que cola o Token)
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(sessionManager))
                    .connectTimeout(30, TimeUnit.SECONDS) // Aumentei o timeout para evitar erros em redes lentas
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // 3. Constrói o Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create()) // Para ler Token como String
                    .addConverterFactory(GsonConverterFactory.create())    // Para ler JSONs normais
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
