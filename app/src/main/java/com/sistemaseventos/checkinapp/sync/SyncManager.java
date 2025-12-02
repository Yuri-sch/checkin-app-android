package com.sistemaseventos.checkinapp.sync;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import com.sistemaseventos.checkinapp.data.repository.EventRepository;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final EventRepository eventRepo;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final AtomicBoolean isSyncing = new AtomicBoolean(false);

    public SyncManager(Context context) {
        this.context = context;
        this.enrollmentRepo = new EnrollmentRepository(context);
        this.userRepo = new UserRepository(context);
        this.eventRepo = new EventRepository(context);
    }

    public void syncAll(OnSyncListener listener) {
        // Se já estiver rodando, aborta esta chamada
        if (isSyncing.get()) {
            Log.d(TAG, "Sincronização já em andamento. Ignorando nova solicitação.");
            if (listener != null) listener.onSyncError("Sincronização já em andamento.");
            return;
        }

        isSyncing.set(true); // Bloqueia

        executor.execute(() -> {
            try {
                Log.d(TAG, "--- INÍCIO SINCRONIZAÇÃO MANUAL ---");

                // 1. Upload: Envia dados criados offline para o servidor
                // Se falhar aqui (sem internet), vai lançar exceção e cair no catch
                int usersCount = userRepo.syncPendingUsers();
                int enrollmentsCount = enrollmentRepo.syncPendingEnrollments();

                // 2. Download: Baixa dados frescos do servidor
                eventRepo.getAllEvents();

                Log.d(TAG, "--- FIM SINCRONIZAÇÃO ---");

                // Avisa a UI na thread principal
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onSyncComplete(usersCount, enrollmentsCount);
                });

            } catch (Exception e) {
                Log.e(TAG, "Erro no Sync", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    // Mostra erro real se falhar a conexão
                    Toast.makeText(context, "Erro ao sincronizar (Verifique a internet): " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (listener != null) listener.onSyncError(e.getMessage());
                });
            } finally {
                isSyncing.set(false); // Libera o bloqueio SEMPRE no finally
            }
        });
    }

    public interface OnSyncListener {
        void onSyncComplete(int usersSynced, int enrollmentsSynced);
        void onSyncError(String error);
    }
}