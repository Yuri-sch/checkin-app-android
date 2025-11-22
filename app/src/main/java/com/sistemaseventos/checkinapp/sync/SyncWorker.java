package com.sistemaseventos.checkinapp.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.sistemaseventos.checkinapp.data.repository.EnrollmentRepository;
import com.sistemaseventos.checkinapp.data.repository.UserRepository;

public class SyncWorker extends Worker {

    private EnrollmentRepository enrollmentRepository;
    private UserRepository userRepository; // Se implementar sync de usuário, use aqui

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        enrollmentRepository = new EnrollmentRepository(context);
        userRepository = new UserRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Chama os métodos de sincronização dos repositórios
            enrollmentRepository.syncPendingEnrollments();
            // userRepository.syncPendingUsers(); // Se implementado

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
