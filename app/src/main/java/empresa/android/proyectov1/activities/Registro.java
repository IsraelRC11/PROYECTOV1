package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import empresa.android.proyectov1.R;

public class Registro extends AppCompatActivity {

    private EditText etNom, etApe, etCod, etPass, etConfirm;
    private ImageView ivFoto;
    private Button btnRegistrar, btnCancelar;
    private FloatingActionButton fabSubir;
    private String rolRecibido;
    private Uri imagenUri = null;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        rolRecibido = getIntent().getStringExtra("rol");

        ivFoto = findViewById(R.id.ivFotoPerfilReg);
        etNom = findViewById(R.id.etNombre);
        etApe = findViewById(R.id.etApellido);
        etCod = findViewById(R.id.etCodigoUPN);
        etPass = findViewById(R.id.etContrasena);
        etConfirm = findViewById(R.id.etConfirmarContrasena);
        btnRegistrar = findViewById(R.id.btnRegistrarse);
        btnCancelar = findViewById(R.id.btnVolverLogin);
        fabSubir = findViewById(R.id.fabSubirFoto);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imagenUri = result.getData().getData();
                        ivFoto.setImageURI(imagenUri);
                        ivFoto.setPadding(0, 0, 0, 0);
                        ivFoto.setImageTintList(null);

                        fabSubir.setImageResource(R.drawable.check);
                        fabSubir.setEnabled(false);

                        new Handler().postDelayed(() -> {
                            fabSubir.setImageResource(R.drawable.flechaarriba);
                            fabSubir.setEnabled(true);
                        }, 3000);
                    }
                }
        );

        fabSubir.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = etNom.getText().toString().trim();
        String apellido = etApe.getText().toString().trim();
        String codigo = etCod.getText().toString().trim();
        String pass = etPass.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        // VALIDACIONES PASO A PASO CON MENSAJES FINOS
        if (imagenUri == null) {
            Toast.makeText(this, "La foto de perfil es obligatoria para tu cuenta", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tus nombres", Toast.LENGTH_SHORT).show();
            return;
        }
        if (apellido.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tus apellidos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ingresa tu código institucional UPN", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.isEmpty()) {
            Toast.makeText(this, "Crea una contraseña de acceso", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirm)) {
            Toast.makeText(this, "Las contraseñas ingresadas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegistrar.setEnabled(false); // Bloqueo para evitar doble registro accidental

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(codigo + "@upn.pe", pass)
                .addOnSuccessListener(authResult -> {
                    subirFotoYDatos(authResult.getUser().getUid(), nombre, apellido, codigo);
                })
                .addOnFailureListener(e -> {
                    btnRegistrar.setEnabled(true);
                    // Controla si el código UPN ya está registrado en Firebase
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Este código UPN ya se encuentra registrado", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void subirFotoYDatos(String uid, String nom, String ape, String cod) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("FotosPerfil").child(uid + ".jpg");

        storageRef.putFile(imagenUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                guardarEnDatabase(uid, nom, ape, cod, uri.toString());
            });
        }).addOnFailureListener(e -> {
            btnRegistrar.setEnabled(true);
            Toast.makeText(this, "Error al subir la foto de perfil", Toast.LENGTH_SHORT).show();
        });
    }

    private void guardarEnDatabase(String uid, String nom, String ape, String cod, String urlFoto) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("nombre", nom);
        user.put("apellido", ape);
        user.put("codigo", cod);
        user.put("rol", rolRecibido);
        user.put("fotoUrl", urlFoto);
        user.put("estado", "offline");

        FirebaseDatabase.getInstance().getReference("Usuarios").child(uid).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Intent i = new Intent(Registro.this, Cursos.class);
                    i.putExtra("rol", rolRecibido);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegistrar.setEnabled(true);
                    Toast.makeText(this, "Error al guardar el perfil en la base de datos", Toast.LENGTH_SHORT).show();
                });
    }
}