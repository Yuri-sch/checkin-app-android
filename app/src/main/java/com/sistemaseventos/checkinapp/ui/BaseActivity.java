package com.sistemaseventos.checkinapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.sync.SyncWorker;

// Classe abstrata para servir de base para as outras
public abstract class BaseActivity extends AppCompatActivity {

    private ProgressBar loading;
    private ImageButton btnSync;

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        // 1. Pega o layout base (que tem o banner)
        View baseView = getLayoutInflater().inflate(R.layout.activity_base, null);

        // 2. Pega o container onde vai o conteúdo da tela específica
        FrameLayout container = baseView.findViewById(R.id.base_content_frame);

        // 3. Infla o layout da Activity filha (ex: activity_checkin) DENTRO do container
        getLayoutInflater().inflate(layoutResID, container, true);

        // 4. Define esse layout combinado como a tela
        super.setContentView(baseView);

        // 5. Configura o botão do banner
        setupToolbar(baseView);
    }

    private void setupToolbar(View root) {
        btnSync = root.findViewById(R.id.btn_sync_manual);
        loading = root.findViewById(R.id.progress_sync_loading);

        if (btnSync != null) {
            btnSync.setOnClickListener(v -> requestSync());
        }

        // Monitora se o Worker está rodando para mostrar o loading
        try {
            WorkManager.getInstance(this).getWorkInfosByTagLiveData("sync_work")
                    .observe(this, workInfos -> {
                        if (workInfos != null && !workInfos.isEmpty()) {
                            WorkInfo.State state = workInfos.get(0).getState();
                            boolean isRunning = state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED;

                            if (loading != null) loading.setVisibility(isRunning ? View.VISIBLE : View.GONE);
                            if (btnSync != null) btnSync.setEnabled(!isRunning);
                        }
                    });
        } catch (Exception e) {
            // Evita crash se WorkManager não estiver inicializado
        }
    }

    // O método que o seu CheckinActivity estava procurando!
    protected void requestSync() {
        Toast.makeText(this, "Sincronizando dados...", Toast.LENGTH_SHORT).show();

        // Inicia o Worker imediatamente
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .addTag("sync_work")
                .build();

        WorkManager.getInstance(this).enqueue(syncRequest);
    }
}
