package empresa.android.proyectov1.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import empresa.android.proyectov1.R;

public class HomeProfesor extends AppCompatActivity {

    private TextView tvSaludo, tvSesiones;
    private ChipGroup cgDisponibilidad;
    private BottomNavigationView bottomNav;

    private PieChart pieChartAprobacion;
    private HorizontalBarChart barChartReunionesMensuales;

    private DatabaseReference mDatabase;
    private String uidLogueado;
    private ValueEventListener badgeValueListener; // Referencia para desenganchar el listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_profesor);

        uidLogueado = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        cargarDatosYEstadisticas();
        cargarDisponibilidadHoraria();
        configurarBadgeMensajesGlobal(); // NUEVO: Escucha global de chats con mensajes nuevos
        configurarNavegacion();
    }

    private void initViews() {
        tvSaludo = findViewById(R.id.tvSaludoHome);
        tvSesiones = findViewById(R.id.tvNumSesiones);
        bottomNav = findViewById(R.id.bottomNavigation);
        cgDisponibilidad = findViewById(R.id.cgDisponibilidad);

        pieChartAprobacion = findViewById(R.id.pieChartAprobacion);
        barChartReunionesMensuales = findViewById(R.id.barChartReunionesMensuales);

        inicializarEstructuraGraficos();
    }

    // NUEVO MÉTODO: Cuenta salas únicas con pendientes y actualiza el BottomNav reactivamente
    private void configurarBadgeMensajesGlobal() {
        if (uidLogueado == null) return;

        badgeValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int salasConMensajesNuevos = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String idSala = ds.getKey();
                    if (idSala != null && idSala.contains(uidLogueado)) {
                        Long misPendientes = ds.child("noLeidos_" + uidLogueado).getValue(Long.class);
                        if (misPendientes != null && misPendientes > 0) {
                            salasConMensajesNuevos++;
                        }
                    }
                }

                if (bottomNav != null) {
                    BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_chats);
                    if (salasConMensajesNuevos > 0) {
                        badge.setVisible(true);
                        badge.setNumber(salasConMensajesNuevos);
                        badge.setBackgroundColor(getResources().getColor(R.color.upn_yellow));
                        badge.setBadgeTextColor(getResources().getColor(R.color.upn_black));
                    } else {
                        bottomNav.removeBadge(R.id.nav_chats);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("Chats").addValueEventListener(badgeValueListener);
    }

    private void inicializarEstructuraGraficos() {
        pieChartAprobacion.setUsePercentValues(true);
        pieChartAprobacion.setDrawHoleEnabled(true);
        pieChartAprobacion.setHoleColor(Color.TRANSPARENT);
        pieChartAprobacion.getDescription().setEnabled(false);
        pieChartAprobacion.getLegend().setEnabled(false);

        barChartReunionesMensuales.getDescription().setEnabled(false);
        barChartReunionesMensuales.getLegend().setEnabled(false);
        barChartReunionesMensuales.getAxisRight().setEnabled(false);

        XAxis xAxis = barChartReunionesMensuales.getXAxis();
        String[] meses = new String[]{"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Set", "Oct", "Nov", "Dic"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(meses));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

        barChartReunionesMensuales.getAxisLeft().setTextColor(Color.WHITE);
        barChartReunionesMensuales.getAxisLeft().setDrawGridLines(true);
        barChartReunionesMensuales.getAxisLeft().setGridColor(Color.parseColor("#1A1A1A"));
    }

    private void cargarDatosYEstadisticas() {
        mDatabase.child("Usuarios").child(uidLogueado).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    tvSaludo.setText("¡HOLA, " + (nombre != null ? nombre.toUpperCase() : "PROFESOR") + "!");

                    DataSnapshot stats = snapshot.child("estadisticas");
                    if (stats.exists()) {
                        long completadas = stats.child("citasCompletadas").getValue(Long.class) != null ? stats.child("citasCompletadas").getValue(Long.class) : 0;
                        float sumaCalificaciones = stats.child("sumaCalificaciones").getValue(Float.class) != null ? stats.child("sumaCalificaciones").getValue(Float.class) : 0f;

                        float promedioEstrellas = completadas > 0 ? (sumaCalificaciones / completadas) : 0f;
                        float porcentajeAprobacion = (promedioEstrellas / 5.0f) * 100f;

                        tvSesiones.setText(String.valueOf(completadas));
                        actualizarGraficoCircular(porcentajeAprobacion);
                    } else {
                        actualizarGraficoCircular(0f);
                        tvSesiones.setText("0");
                    }

                    cargarHistorialReunionesMensuales(snapshot.child("historialMensual"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void actualizarGraficoCircular(float porcentaje) {
        ArrayList<PieEntry> entradas = new ArrayList<>();
        entradas.add(new PieEntry(porcentaje, ""));
        entradas.add(new PieEntry(100f - porcentaje, ""));

        PieDataSet dataSet = new PieDataSet(entradas, "");
        int[] colores = new int[]{Color.parseColor("#FFCC00"), Color.parseColor("#252525")};
        dataSet.setColors(colores);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        pieChartAprobacion.setData(data);

        pieChartAprobacion.setCenterText(String.format(Locale.US, "%.0f%%", porcentaje));
        pieChartAprobacion.setCenterTextColor(Color.WHITE);
        pieChartAprobacion.setCenterTextSize(16f);

        pieChartAprobacion.invalidate();
    }

    private void cargarHistorialReunionesMensuales(DataSnapshot historialSnapshot) {
        ArrayList<BarEntry> entradasBarras = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            float cantidadSesionesMes = 0f;
            if (historialSnapshot.exists() && historialSnapshot.hasChild(String.valueOf(i))) {
                Long val = historialSnapshot.child(String.valueOf(i)).getValue(Long.class);
                if (val != null) cantidadSesionesMes = val.floatValue();
            }
            entradasBarras.add(new BarEntry(i, cantidadSesionesMes));
        }

        BarDataSet barDataSet = new BarDataSet(entradasBarras, "Sesiones");
        barDataSet.setColor(Color.parseColor("#FFCC00"));
        barDataSet.setValueTextColor(Color.WHITE);
        barDataSet.setValueTextSize(10f);

        BarData data = new BarData(barDataSet);
        barChartReunionesMensuales.setData(data);
        barChartReunionesMensuales.invalidate();
    }

    private void cargarDisponibilidadHoraria() {
        mDatabase.child("Usuarios").child(uidLogueado).child("disponibilidad")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cgDisponibilidad.removeAllViews();

                        if (snapshot.exists()) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String dia = ds.getKey();
                                String rangoHora = ds.getValue(String.class);

                                Chip chip = new Chip(HomeProfesor.this);
                                chip.setText(dia + ": " + rangoHora);
                                chip.setCloseIconVisible(true);

                                chip.setChipBackgroundColorResource(R.color.upn_black);
                                chip.setTextColor(getResources().getColor(R.color.text_white));
                                chip.setCloseIconTintResource(R.color.upn_yellow);

                                chip.setOnCloseIconClickListener(v -> {
                                    mDatabase.child("Usuarios").child(uidLogueado).child("disponibilidad").child(dia).removeValue();
                                });

                                cgDisponibilidad.addView(chip);
                            }
                        }

                        Chip btnAgregar = new Chip(HomeProfesor.this);
                        btnAgregar.setText("+ CONFIGURAR HORARIO");
                        btnAgregar.setChipBackgroundColorResource(R.color.upn_yellow);
                        btnAgregar.setTextColor(getResources().getColor(R.color.pure_black));
                        btnAgregar.setOnClickListener(v -> abrirDialogoHorario());
                        cgDisponibilidad.addView(btnAgregar);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void abrirDialogoHorario() {
        CharSequence[] dias = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona el día a configurar");

        builder.setItems(dias, (dialog, index) -> {
            String diaSeleccionado = dias[index].toString();

            TimePickerDialog timePickerInicio = new TimePickerDialog(HomeProfesor.this,
                    (viewInicio, horaIn, minIn) -> {

                        String amPmIn = (horaIn >= 12) ? "PM" : "AM";
                        int horaIn12 = (horaIn > 12) ? (horaIn - 12) : (horaIn == 0 ? 12 : horaIn);
                        String horaInicioFormateada = String.format(Locale.US, "%02d:%02d %s", horaIn12, minIn, amPmIn);

                        TimePickerDialog timePickerFin = new TimePickerDialog(HomeProfesor.this,
                                (viewFin, horaFin, minFin) -> {

                                    String amPmFin = (horaFin >= 12) ? "PM" : "AM";
                                    int horaFin12 = (horaFin > 12) ? (horaFin - 12) : (horaFin == 0 ? 12 : horaFin);
                                    String horaFinFormateada = String.format(Locale.US, "%02d:%02d %s", horaFin12, minFin, amPmFin);

                                    String rangoHorarioFinal = horaInicioFormateada + " - " + horaFinFormateada;

                                    mDatabase.child("Usuarios").child(uidLogueado).child("disponibilidad")
                                            .child(diaSeleccionado).setValue(rangoHorarioFinal);
                                },
                                12, 0, false);

                        timePickerFin.setTitle("Hora de finalización para el " + diaSeleccionado);
                        timePickerFin.show();

                    },
                    8, 0, false);

            timePickerInicio.setTitle("Hora de inicio para el " + diaSeleccionado);
            timePickerInicio.show();
        });

        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDatabase.child("Usuarios").child(uidLogueado).child("estado").setValue("online");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDatabase.child("Usuarios").child(uidLogueado).child("estado").setValue("offline");
    }

    private void configurarNavegacion() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chats) {
                startActivity(new Intent(this, Mensajes.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilProfesor.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return id == R.id.nav_home;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (badgeValueListener != null) {
            mDatabase.child("Chats").removeEventListener(badgeValueListener);
        }
    }
}