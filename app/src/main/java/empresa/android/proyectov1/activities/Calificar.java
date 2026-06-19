package empresa.android.proyectov1.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.Calendar;
import empresa.android.proyectov1.R;

public class Calificar extends AppCompatActivity {
    private RatingBar rbEstrellas;
    private MaterialButton btnEnviar, btnOmitir;
    private TextView tvNombre;
    private ImageView ivFoto;
    private String idProfesor, idEstudiante;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calificar);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        idEstudiante = FirebaseAuth.getInstance().getUid();
        idProfesor = getIntent().getStringExtra("idProfesor");

        tvNombre = findViewById(R.id.tvNombreProfCalificar);
        ivFoto = findViewById(R.id.ivFotoCalificar);
        rbEstrellas = findViewById(R.id.rbCalificacion);
        btnEnviar = findViewById(R.id.btnEnviarCalificacion);
        btnOmitir = findViewById(R.id.btnOmitir);

        cargarDatosProfesor();

        btnEnviar.setOnClickListener(v -> enviarNota());
        btnOmitir.setOnClickListener(v -> finalizarYBorrarAlerta());
    }

    private void cargarDatosProfesor() {
        mDatabase.child("Usuarios").child(idProfesor).addListenerForSingleValueEvent(new ValueEventListener() {
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

        DatabaseReference refProfesor = mDatabase.child("Usuarios").child(idProfesor);

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
        // CORREGIDO: Aseguramos borrar usando el ID exacto del estudiante logueado (idEstudiante)
        mDatabase.child("CalificacionesPendientes").child(idEstudiante).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Solo cuando Firebase confirme el borrado físico del nodo, se cierra la pantalla
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnEnviar.setEnabled(true);
                    finish();
                });
    }
}