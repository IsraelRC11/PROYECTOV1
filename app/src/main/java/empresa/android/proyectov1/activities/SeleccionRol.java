package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import empresa.android.proyectov1.R;

public class SeleccionRol extends AppCompatActivity {

    private LinearLayout btnEstudiante, btnProfesor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion_rol);

        // Vinculamos los contenedores por ID
        btnEstudiante = findViewById(R.id.btnRolEstudiante);
        btnProfesor = findViewById(R.id.btnRolProfesor);

        // Acción para Estudiante
        btnEstudiante.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irARegistro("estudiante");
            }
        });

        // Acción para Profesor
        btnProfesor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irARegistro("profesor");
            }
        });
    }

    private void irARegistro(String rol) {
        Intent intent = new Intent(SeleccionRol.this, Registro.class);
        intent.putExtra("rol", rol); // Pasamos el rol para que Registro sepa qué guardar
        startActivity(intent);
    }
}