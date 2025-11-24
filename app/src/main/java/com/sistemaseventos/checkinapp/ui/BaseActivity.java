package com.sistemaseventos.checkinapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.sync.SyncManager;

public abstract class BaseActivity extends AppCompatActivity {

    private ProgressBar loading;
    private ImageButton btnSync;
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
        btnSync = root.findViewById(R.id.btn_sync_manual);
        loading = root.findViewById(R.id.progress_sync_loading);

        if (btnSync != null) {
            btnSync.setOnClickListener(v -> requestManualSync());
        }
    }

    protected void requestManualSync() {
        if (loading != null) loading.setVisibility(View.VISIBLE);
        if (btnSync != null) btnSync.setEnabled(false);

        syncManager.syncAll(new SyncManager.OnSyncListener() {
            @Override
            public void onSyncComplete() {
                if (loading != null) loading.setVisibility(View.GONE);
                if (btnSync != null) btnSync.setEnabled(true);
                onSyncFinished(); // Hook para subclasses atualizarem a tela
            }

            @Override
            public void onSyncError(String error) {
                if (loading != null) loading.setVisibility(View.GONE);
                if (btnSync != null) btnSync.setEnabled(true);
            }
        });
    }

    // Método vazio que as activities filhas podem sobrescrever para atualizar a lista após o sync
    protected void onSyncFinished() {}
}
