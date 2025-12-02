package com.sistemaseventos.checkinapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Alterado de ImageButton para Button
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.sync.SyncManager;

public abstract class BaseActivity extends AppCompatActivity {

    private ProgressBar loading;
    private Button btnSync; // Agora é um Button (texto)
    private SyncManager syncManager;

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        View baseView = getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout container = baseView.findViewById(R.id.base_content_frame);
        getLayoutInflater().inflate(layoutResID, container, true);
        super.setContentView(baseView);

        syncManager = new SyncManager(this);
        setupToolbar(baseView);
    }

    private void setupToolbar(View root) {
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Busca o botão pelo ID (agora é um Button no XML)
        btnSync = root.findViewById(R.id.btn_sync_manual);
        loading = root.findViewById(R.id.progress_sync_loading);

        if (btnSync != null) {
            btnSync.setOnClickListener(v -> requestManualSync());
        }
    }

    protected void requestManualSync() {
        if (loading != null) loading.setVisibility(View.VISIBLE);
        if (btnSync != null) btnSync.setEnabled(false);
        // btnSync.setText("Enviando..."); // Opcional: Mudar texto durante sync

        syncManager.syncAll(new SyncManager.OnSyncListener() {
            @Override
            public void onSyncComplete(int users, int enrollments) {
                if (loading != null) loading.setVisibility(View.GONE);
                if (btnSync != null) {
                    btnSync.setEnabled(true);
                    // btnSync.setText("SINCRONIZAR"); // Volta texto original
                }

                String msg;
                if (users == 0 && enrollments == 0) {
                    msg = "Nada novo a sincronizar!";
                } else {
                    msg = "Sucesso! Enviados: " + users + " usuários e " + enrollments + " inscrições.";
                }
                Toast.makeText(BaseActivity.this, msg, Toast.LENGTH_LONG).show();

                // Avisa a Activity filha para atualizar a lista
                onSyncFinished();
            }

            @Override
            public void onSyncError(String error) {
                if (loading != null) loading.setVisibility(View.GONE);
                if (btnSync != null) {
                    btnSync.setEnabled(true);
                    // btnSync.setText("SINCRONIZAR");
                }
                // O SyncManager já exibe Toast de erro
            }
        });
    }

    // Método gancho para as activities filhas recarregarem dados
    protected void onSyncFinished() {}
}
