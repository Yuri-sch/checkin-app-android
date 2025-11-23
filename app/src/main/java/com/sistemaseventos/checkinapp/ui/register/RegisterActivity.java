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
            String cpf = cpfInput.getText().toString();
            if (!email.isEmpty() && !cpf.isEmpty()) {
                viewModel.registerSimpleUser(cpf, email);
            }
        });

        viewModel.registerSuccess.observe(this, user -> {
            Toast.makeText(this, "Cadastro realizado!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, UserDetailActivity.class);
            intent.putExtra("USER_ID", user.id);
            intent.putExtra("USER_NAME", user.fullname);
            intent.putExtra("USER_CPF", user.cpf);     // Adicione isso
            intent.putExtra("USER_EMAIL", user.email); // Adicione isso
            startActivity(intent);
            finish();
        });
    }
}
