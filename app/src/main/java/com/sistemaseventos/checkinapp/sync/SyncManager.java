package com.sistemaseventos.checkinapp.sync;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import com.sistemaseventos.checkinapp.data.repository.EventRepository;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final EventRepository eventRepo;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SyncManager(Context context) {
        this.context = context;
        this.enrollmentRepo = new EnrollmentRepository(context);
        this.userRepo = new UserRepository(context);
        this.eventRepo = new EventRepository(context);
    }

    public void syncAll(OnSyncListener listener) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "--- INÍCIO SINCRONIZAÇÃO MANUAL ---");

                // 1. Upload: Envia dados criados offline para o servidor
                // Se falhar aqui (sem internet), vai lançar exceção e cair no catch
                userRepo.syncPendingUsers();
                enrollmentRepo.syncPendingEnrollments();

                // 2. Download: Baixa dados frescos do servidor
                eventRepo.getAllEvents();

                Log.d(TAG, "--- FIM SINCRONIZAÇÃO ---");

                // Avisa a UI na thread principal
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onSyncComplete();
                });

            } catch (Exception e) {
                Log.e(TAG, "Erro no Sync", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    // Mostra erro real se falhar a conexão
                    Toast.makeText(context, "Erro ao sincronizar (Verifique a internet): " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (listener != null) listener.onSyncError(e.getMessage());
                });
            }
        });
    }

    public interface OnSyncListener {
        void onSyncComplete();
        void onSyncError(String error);
    }
}