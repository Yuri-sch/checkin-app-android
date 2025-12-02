package com.sistemaseventos.checkinapp.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.sistemaseventos.checkinapp.R;
import com.sistemaseventos.checkinapp.sync.SyncManager;
import com.sistemaseventos.checkinapp.ui.checkin.CheckinActivity;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        EditText emailInput = findViewById(R.id.email_input);
        EditText passwordInput = findViewById(R.id.password_input);
        Button loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> {
            viewModel.login(emailInput.getText().toString(), passwordInput.getText().toString());
        });

        viewModel.loginSuccess.observe(this, success -> {
            if (success) {
                startActivity(new Intent(this, CheckinActivity.class));
                finish();
            }
        });

        viewModel.error.observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        );
    }
}
