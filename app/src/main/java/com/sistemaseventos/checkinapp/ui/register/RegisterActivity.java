package com.sistemaseventos.checkinapp.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.ui.BaseActivity;
import com.sistemaseventos.checkinapp.ui.userdetail.UserDetailActivity;
import com.sistemaseventos.checkinapp.util.CpfValidator;

public class RegisterActivity extends BaseActivity {

    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        EditText cpfInput = findViewById(R.id.reg_cpf_input);
        EditText emailInput = findViewById(R.id.reg_email_input);
        Button registerButton = findViewById(R.id.register_btn);

        String cpfExtra = getIntent().getStringExtra("CPF_EXTRA");
        if (cpfExtra != null) {
            cpfInput.setText(cpfExtra);
            cpfInput.setEnabled(false); // Trava edição do CPF
        }

        registerButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            // Limpa o CPF para garantir
            String cpf = cpfInput.getText().toString().replaceAll("[^0-9]", "");

            if (email.isEmpty() || cpf.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validação extra de segurança
            if (!CpfValidator.isValid(cpf)) {
                Toast.makeText(this, "CPF Inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.registerSimpleUser(cpf, email);
        });

        viewModel.registerSuccess.observe(this, user -> {
            // VERIFICAÇÃO ADICIONADA
            if (!user.isSynced) {
                Toast.makeText(this, "Sem internet: Usuário salvo localmente e pendente de envio.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Cadastro realizado e sincronizado!", Toast.LENGTH_SHORT).show();
            }

            Intent intent = new Intent(RegisterActivity.this, UserDetailActivity.class);
            intent.putExtra("USER_ID", user.id);
            intent.putExtra("USER_NAME", user.fullname);
            intent.putExtra("USER_CPF", user.cpf);
            intent.putExtra("USER_EMAIL", user.email);
            startActivity(intent);
            finish();
        });
    }
}
