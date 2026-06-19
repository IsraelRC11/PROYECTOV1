package empresa.android.proyectov1.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import empresa.android.proyectov1.R;

public class PerfilEstudiante extends AppCompatActivity {

    private ImageView ivPerfil, btnEditarDatos;
    private TextView tvNombre;
    private MaterialButton btnLogout, btnIntereses, btnEditPass;
    private FloatingActionButton fabActualizar;
    private BottomNavigationView bottomNav;
    private DatabaseReference mDatabase;
    private String uid;
    private ActivityResultLauncher<Intent> galleryLauncher;

    // Variables locales para almacenar y precargar datos en el diálogo
    private String nombreActual = "", apellidoActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_estudiante);

        uid = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        ivPerfil = findViewById(R.id.ivPerfilFoto);
        tvNombre = findViewById(R.id.tvPerfilNombreCompleto);
        btnEditarDatos = findViewById(R.id.btnEditarDatosPersonales); // Enlazado al lápiz amarillo
        btnEditPass = findViewById(R.id.btnEditPass);
        btnLogout = findViewById(R.id.btnLogOut);
        btnIntereses = findViewById(R.id.btnEditIntereses);
        bottomNav = findViewById(R.id.bottomNavigation);
        fabActualizar = findViewById(R.id.fabActualizarFoto);

        obtenerDatosEstudiante();

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri nuevaImagenUri = result.getData().getData();
                        ivPerfil.setImageURI(nuevaImagenUri);
                        ivPerfil.setPadding(0, 0, 0, 0);
                        ivPerfil.setImageTintList(null);
                        actualizarFotoEnFirebase(nuevaImagenUri);
                    }
                }
        );

        fabActualizar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        // Evento para abrir el diálogo de editar nombres/apellidos
        btnEditarDatos.setOnClickListener(v -> mostrarDialogoEditarPerfil());

        // Evento para abrir el diálogo de cambio de contraseña
        btnEditPass.setOnClickListener(v -> mostrarDialogoCambiarContrasena());

        btnIntereses.setOnClickListener(v -> {
            Intent i = new Intent(this, Cursos.class);
            i.putExtra("rol", "estudiante");
            startActivity(i);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, Login.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        configurarNavegacion();
    }

    private void obtenerDatosEstudiante() {
        if (uid == null) return;
        mDatabase.child("Usuarios").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    nombreActual = snapshot.child("nombre").getValue(String.class);
                    apellidoActual = snapshot.child("apellido").getValue(String.class);
                    String urlFoto = snapshot.child("fotoUrl").getValue(String.class);

                    tvNombre.setText(nombreActual + " " + apellidoActual);

                    if (urlFoto != null && !urlFoto.isEmpty()) {
                        ivPerfil.setPadding(0, 0, 0, 0);
                        ivPerfil.setImageTintList(null);
                        Glide.with(PerfilEstudiante.this).load(urlFoto).placeholder(R.drawable.usuario).into(ivPerfil);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void mostrarDialogoEditarPerfil() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_editar_perfil, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        EditText etNombre = dialogView.findViewById(R.id.etDialogNombre);
        EditText etApellido = dialogView.findViewById(R.id.etDialogApellido);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnDialogCancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnDialogGuardar);

        etNombre.setText(nombreActual);
        etApellido.setText(apellidoActual);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevoApellido = etApellido.getText().toString().trim();

            if (nuevoNombre.isEmpty() || nuevoApellido.isEmpty()) {
                Toast.makeText(PerfilEstudiante.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> actualizacionesPerfil = new HashMap<>();
            actualizacionesPerfil.put("nombre", nuevoNombre);
            actualizacionesPerfil.put("apellido", nuevoApellido);

            mDatabase.child("Usuarios").child(uid).updateChildren(actualizacionesPerfil)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(PerfilEstudiante.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(PerfilEstudiante.this, "Error al actualizar", Toast.LENGTH_SHORT).show());
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void mostrarDialogoCambiarContrasena() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_cambiar_contra, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        EditText etNuevaContra = dialogView.findViewById(R.id.etDialogNuevaContra);
        EditText etConfirmarContra = dialogView.findViewById(R.id.etDialogConfirmarContra);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnDialogContraCancelar);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnDialogContraGuardar);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String nuevaContra = etNuevaContra.getText().toString().trim();
            String confirmarContra = etConfirmarContra.getText().toString().trim();

            if (nuevaContra.isEmpty() || confirmarContra.isEmpty()) {
                Toast.makeText(PerfilEstudiante.this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (nuevaContra.length() < 6) {
                Toast.makeText(PerfilEstudiante.this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!nuevaContra.equals(confirmarContra)) {
                Toast.makeText(PerfilEstudiante.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cambiar contraseña nativa en Firebase Authentication
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                btnGuardar.setEnabled(false);
                user.updatePassword(nuevaContra).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(PerfilEstudiante.this, "Contraseña actualizada en Firebase", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        btnGuardar.setEnabled(true);
                        Toast.makeText(PerfilEstudiante.this, "Error: Vuelve a iniciar sesión para cambiarla", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void actualizarFotoEnFirebase(Uri uri) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("FotosPerfil").child(uid + ".jpg");

        fabActualizar.setEnabled(false);

        storageRef.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String nuevoLinkFoto = downloadUri.toString();
                mDatabase.child("Usuarios").child(uid).child("fotoUrl").setValue(nuevoLinkFoto)
                        .addOnSuccessListener(aVoid -> {
                            fabActualizar.setImageResource(R.drawable.check);
                            new Handler().postDelayed(() -> {
                                fabActualizar.setImageResource(R.drawable.flechaarriba);
                                fabActualizar.setEnabled(true);
                            }, 3000);
                        });
            });
        }).addOnFailureListener(e -> {
            fabActualizar.setEnabled(true);
            fabActualizar.setImageResource(R.drawable.flechaarriba);
            Toast.makeText(this, "Error al subir", Toast.LENGTH_SHORT).show();
        });
    }

    private void configurarNavegacion() {
        bottomNav.setSelectedItemId(R.id.nav_perfil);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeEstudiante.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_chats) {
                startActivity(new Intent(this, Mensajes.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return id == R.id.nav_perfil;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDatabase.child("Usuarios").child(uid).child("estado").setValue("online");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDatabase.child("Usuarios").child(uid).child("estado").setValue("offline");
    }
}