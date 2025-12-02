package com.sistemaseventos.checkinapp.ui.checkin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.sync.SyncManager;
import com.sistemaseventos.checkinapp.ui.BaseActivity;
import com.sistemaseventos.checkinapp.ui.register.RegisterActivity;
import com.sistemaseventos.checkinapp.ui.userdetail.UserDetailActivity;
import com.sistemaseventos.checkinapp.util.CpfValidator;

public class CheckinActivity extends BaseActivity {

    private CheckinViewModel viewModel;
    private EditText cpfInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);

        // Dispara sync inicial em background
        new SyncManager(this).syncAll(null); // Passa null no listener se não quiser feedback visual

        viewModel = new ViewModelProvider(this).get(CheckinViewModel.class);

        cpfInput = findViewById(R.id.cpf_input);
        Button searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(v -> {
            String rawCpf = cpfInput.getText().toString().replaceAll("[^0-9]", "");

            if (rawCpf.isEmpty()) {
                Toast.makeText(this, "Digite um CPF", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validação Lógica
            if (!CpfValidator.isValid(rawCpf)) {
                Toast.makeText(this, "CPF Inválido! Verifique os números.", Toast.LENGTH_LONG).show();
                return; // Interrompe a execução aqui
            }

            // Se passou, chama a busca com o CPF limpo
            viewModel.searchUser(rawCpf);
        });

        setupObservers();
    }

    private void setupObservers() {
        viewModel.userFound.observe(this, user -> {
            Intent intent = new Intent(CheckinActivity.this, UserDetailActivity.class);
            intent.putExtra("USER_ID", user.id);
            intent.putExtra("USER_NAME", user.fullname);
            intent.putExtra("USER_CPF", user.cpf);
            intent.putExtra("USER_EMAIL", user.email);
            // Passa a data de nascimento
            if (user.birthDate != null) {
                intent.putExtra("USER_BIRTHDATE", user.birthDate.getTime());
            }
            startActivity(intent);
        });

        viewModel.userNotFound.observe(this, cpf -> {
            Intent intent = new Intent(CheckinActivity.this, RegisterActivity.class);
            intent.putExtra("CPF_EXTRA", cpf);
            startActivity(intent);
        });

        viewModel.error.observe(this, msg -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}
