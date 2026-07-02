package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.*;
import java.util.Calendar;
import empresa.android.proyectov1.R;

public class Calificar extends BaseActivity {
    private RatingBar rbEstrellas;
    private MaterialButton btnEnviar, btnOmitir;
    private TextView tvNombre;
    private ImageView ivFoto;
    private String idProfesor, idChatAsesoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calificar);

        // Variables mDatabaseRef y currentUid ya inicializadas por BaseActivity

        idProfesor = getIntent().getStringExtra("idProfesor");
        idChatAsesoria = getIntent().getStringExtra("idChatAsesoria");

        tvNombre = findViewById(R.id.tvNombreProfCalificar);
        ivFoto = findViewById(R.id.ivFotoCalificar);
        rbEstrellas = findViewById(R.id.rbCalificacion);
        btnEnviar = findViewById(R.id.btnEnviarCalificacion);
        btnOmitir = findViewById(R.id.btnOmitir);

        // OBLIGATORIO: Ocultamos el botón omitir para forzar la calificación
        if (btnOmitir != null) {
            btnOmitir.setVisibility(View.GONE);
        }

        // SOLUCIÓN MODERNA PARA BLOQUEAR EL BOTÓN FÍSICO DE ATRÁS
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(Calificar.this, "Debes calificar la asesoría para continuar", Toast.LENGTH_SHORT).show();
            }
        });

        cargarDatosProfesor();

        btnEnviar.setOnClickListener(v -> enviarNota());
    }

    private void cargarDatosProfesor() {
        mDatabaseRef.child("Usuarios").child(idProfesor).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    String apellido = snapshot.child("apellido").getValue(String.class);
                    String foto = snapshot.child("fotoUrl").getValue(String.class);

                    tvNombre.setText("Ing. " + nombre + " " + apellido);
                    if (foto != null && !foto.isEmpty()) {
                        ivFoto.setImageTintList(null);
                        Glide.with(Calificar.this).load(foto).into(ivFoto);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void enviarNota() {
        float nota = rbEstrellas.getRating();
        if (nota == 0) {
            Toast.makeText(this, "Por favor, selecciona al menos una estrella", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEnviar.setEnabled(false);

        int mesActual = Calendar.getInstance().get(Calendar.MONTH);
        final String stringMes = String.valueOf(mesActual);

        DatabaseReference refProfesor = mDatabaseRef.child("Usuarios").child(idProfesor);

        refProfesor.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                MutableData stats = currentData.child("estadisticas");
                long completadas = stats.child("citasCompletadas").getValue(Long.class) != null ?
                        stats.child("citasCompletadas").getValue(Long.class) : 0;
                float suma = stats.child("sumaCalificaciones").getValue(Float.class) != null ?
                        stats.child("sumaCalificaciones").getValue(Float.class) : 0f;

                stats.child("citasCompletadas").setValue(completadas + 1);
                stats.child("sumaCalificaciones").setValue(suma + nota);

                MutableData historialMensual = currentData.child("historialMensual");
                long sesionesDelMes = historialMensual.child(stringMes).getValue(Long.class) != null ?
                        historialMensual.child(stringMes).getValue(Long.class) : 0;

                historialMensual.child(stringMes).setValue(sesionesDelMes + 1);

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError e, boolean b, DataSnapshot s) {
                if (b) {
                    Toast.makeText(Calificar.this, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show();
                    finalizarYBorrarAlerta();
                } else {
                    btnEnviar.setEnabled(true);
                    Toast.makeText(Calificar.this, "Error al guardar calificación", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void finalizarYBorrarAlerta() {
        if (idChatAsesoria == null || idChatAsesoria.isEmpty()) {
            irAlHome();
            return;
        }

        // 1. Cambiamos el estado del chat a "leido" para que los listeners no vuelvan a disparar la calificación
        mDatabaseRef.child("Chats").child(idChatAsesoria).child("estado").setValue("leido")
                .addOnCompleteListener(task -> {
                    // 2. Tras asegurar que el chat ya no está "finalizado", borramos la alerta de pendientes
                    mDatabaseRef.child("CalificacionesPendientes")
                            .child(currentUid)
                            .child(idChatAsesoria)
                            .removeValue()
                            .addOnCompleteListener(task1 -> {
                                // 3. Redirigimos al Home de forma segura
                                irAlHome();
                            });
                });
    }

    private void irAlHome() {
        Intent intent = new Intent(Calificar.this, HomeEstudiante.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}