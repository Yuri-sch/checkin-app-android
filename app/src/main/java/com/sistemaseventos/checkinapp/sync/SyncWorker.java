package com.sistemaseventos.checkinapp.sync;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import com.sistemaseventos.checkinapp.data.repository.EventRepository;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;

public class SyncWorker extends Worker {

    private EnrollmentRepository enrollmentRepo;
    private UserRepository userRepo;
    private EventRepository eventRepo;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        enrollmentRepo = new EnrollmentRepository(context);
        userRepo = new UserRepository(context);
        eventRepo = new EventRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SyncWorker", "Iniciando sincronização completa...");
        try {
            // 1. Upload: Envia dados pendentes criados offline
            userRepo.syncPendingUsers();
            enrollmentRepo.syncPendingEnrollments();

            // 2. Download: Baixa dados atualizados para uso offline futuro
            Log.d("SyncWorker", "Baixando eventos...");
            eventRepo.getAllEvents(); // Isso popula o cache de eventos

            return Result.success();
        } catch (Exception e) {
            Log.e("SyncWorker", "Erro na sincronização", e);
            return Result.retry();
        }
    }
}
