package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.FirebaseDatabase;

import empresa.android.proyectov1.R;

public class Login extends AppCompatActivity {
    private EditText etCodigo, etPass;
    private Button btnLogin, btnGoReg;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        etCodigo = findViewById(R.id.etCodigoLogin);
        etPass = findViewById(R.id.etPasswordLogin);
        btnLogin = findViewById(R.id.btnIniciarSesion);
        btnGoReg = findViewById(R.id.btnIrRegistro);

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoReg.setOnClickListener(v -> startActivity(new Intent(this, SeleccionRol.class)));
    }

    private void loginUser() {
        String codigo = etCodigo.getText().toString().trim();
        String pass = etPass.getText().toString().trim();

        // VALIDACIÓN LOCAL PREMIUM: Indica exactamente qué falta rellenar
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu código UPN", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        String mail = codigo + "@upn.pe";

        mAuth.signInWithEmailAndPassword(mail, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                checkUserRole();
            } else {
                // CAPTURA DE ERRORES REALES DE FIREBASE
                if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                    Toast.makeText(this, "Nombre de usuario y/o contraseña incorrectos", Toast.LENGTH_LONG).show();
                } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(this, "Nombre de usuario y/o contraseña incorrectos", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Error de conexión. Revisa tu internet", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkUserRole() {
        String uid = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Usuarios").child(uid).child("rol")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getValue() != null) {
                        String rol = String.valueOf(task.getResult().getValue());
                        if ("profesor".equals(rol)) {
                            startActivity(new Intent(this, HomeProfesor.class));
                        } else {
                            startActivity(new Intent(this, HomeEstudiante.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(this, "Error al recuperar el perfil de usuario", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}