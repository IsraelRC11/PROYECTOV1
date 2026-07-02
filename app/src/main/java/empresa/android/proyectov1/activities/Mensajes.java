package empresa.android.proyectov1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import empresa.android.proyectov1.R;
import empresa.android.proyectov1.adapters.ChatAdapter;
import empresa.android.proyectov1.models.ChatModel;

public class Mensajes extends BaseActivity {

    private BottomNavigationView bottomNav;
    private RecyclerView rvChats;
    private EditText etBuscar;
    private ChatAdapter chatAdapter;
    private List<ChatModel> listaChats;
    private List<ChatModel> listaChatsFiltrada;

    private DatabaseReference mDatabase;
    private String uidLogueado, rolUsuario;
    private ValueEventListener chatsValueListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mensajes);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        uidLogueado = FirebaseAuth.getInstance().getUid();

        initViews();
        obtenerRolYContenido();
        configurarNavegacion();
        configurarBuscador();
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottomNavigation);
        etBuscar = findViewById(R.id.etBuscarConversacion);

        rvChats = findViewById(R.id.rvConversaciones);
        rvChats.setLayoutManager(new LinearLayoutManager(this));

        listaChats = new ArrayList<>();
        listaChatsFiltrada = new ArrayList<>();

        chatAdapter = new ChatAdapter(listaChatsFiltrada, rolUsuario);
        rvChats.setAdapter(chatAdapter);
    }

    private void obtenerRolYContenido() {
        if (uidLogueado == null) return;

        mDatabase.child("Usuarios").child(uidLogueado).child("role")
                .get().addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult().exists()){
                        rolUsuario = task.getResult().getValue(String.class);
                        actualizarAdapter();
                        cargarListaChats();
                        return;
                    }

                    mDatabase.child("Usuarios").child(uidLogueado).child("rol")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    rolUsuario = snapshot.getValue(String.class);
                                    actualizarAdapter();
                                    cargarListaChats();
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                });
    }

    private void actualizarAdapter() {
        chatAdapter = new ChatAdapter(listaChatsFiltrada, rolUsuario);
        rvChats.setAdapter(chatAdapter);
    }

    private void cargarListaChats() {
        chatsValueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaChats.clear();
                if (!snapshot.exists()) {
                    listaChatsFiltrada.clear();
                    chatAdapter.notifyDataSetChanged();
                    actualizarBadgeGlobal(); // Limpiar badge si no hay chats
                    return;
                }

                final long totalChats = snapshot.getChildrenCount();
                final int[] chatsProcesados = {0};

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String idSala = ds.getKey();

                    if (idSala != null && idSala.contains(uidLogueado)) {
                        String idReceptor = idSala.replace(uidLogueado, "").replace("_", "");

                        ChatModel chat = ds.getValue(ChatModel.class);
                        if (chat != null) {
                            chat.setIdChat(idSala);
                            chat.setIdReceptor(idReceptor);

                            Long misPendientes = ds.child("noLeidos_" + uidLogueado).getValue(Long.class);
                            chat.setMensajesNoLeidos(misPendientes != null ? misPendientes.intValue() : 0);

                            mDatabase.child("Usuarios").child(idReceptor)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                            if (userSnapshot.exists()) {
                                                String nombre = userSnapshot.child("nombre").getValue(String.class);
                                                String apellido = userSnapshot.child("apellido").getValue(String.class);
                                                chat.setNombreReceptor(nombre + " " + apellido);
                                                chat.setFotoReceptor(userSnapshot.child("fotoUrl").getValue(String.class));

                                                listaChats.remove(chat);
                                                listaChats.add(chat);
                                            }
                                            chatsProcesados[0]++;
                                            if (chatsProcesados[0] >= totalChats) {
                                                realizarFiltradoYOrdenamiento(etBuscar.getText().toString());
                                                actualizarBadgeGlobal(); // Refrescar el contador de la barra inferior
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError e) {
                                            chatsProcesados[0]++;
                                        }
                                    });
                        } else {
                            chatsProcesados[0]++;
                        }
                    } else {
                        chatsProcesados[0]++;
                    }
                }

                if (totalChats == 0 || chatsProcesados[0] >= totalChats) {
                    realizarFiltradoYOrdenamiento(etBuscar.getText().toString());
                    actualizarBadgeGlobal();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("Chats").addValueEventListener(chatsValueListener);
    }

    // CORREGIDO: Cuenta salas/chats únicos con mensajes nuevos en vez de acumular el total de textos
    private void actualizarBadgeGlobal() {
        int salasConMensajesNuevos = 0;
        for (ChatModel chat : listaChats) {
            if (chat.getMensajesNoLeidos() > 0) {
                salasConMensajesNuevos++;
            }
        }

        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_chats);

        if (salasConMensajesNuevos > 0) {
            badge.setVisible(true);
            badge.setNumber(salasConMensajesNuevos);
            // Estética acoplada a tu paleta Starboy/UPN
            badge.setBackgroundColor(getResources().getColor(R.color.upn_yellow));
            badge.setBadgeTextColor(getResources().getColor(R.color.upn_black));
        } else {
            bottomNav.removeBadge(R.id.nav_chats);
        }
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                realizarFiltradoYOrdenamiento(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void realizarFiltradoYOrdenamiento(String textoABuscar) {
        listaChatsFiltrada.clear();
        String query = textoABuscar.toLowerCase().trim();

        for (ChatModel chat : listaChats) {
            if (query.isEmpty() || (chat.getNombreReceptor() != null && chat.getNombreReceptor().toLowerCase().contains(query))) {
                listaChatsFiltrada.add(chat);
            }
        }

        Collections.sort(listaChatsFiltrada, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
        chatAdapter.notifyDataSetChanged();
    }

    private void configurarNavegacion() {
        bottomNav.setSelectedItemId(R.id.nav_chats);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                redireccionarAlHomeCorrecto();
                return true;
            } else if (id == R.id.nav_perfil) {
                redireccionarAlPerfilCorrecto();
                return true;
            }
            return id == R.id.nav_chats;
        });
    }

    private void redireccionarAlHomeCorrecto() {
        Intent intent = (rolUsuario != null && rolUsuario.equals("profesor"))
                ? new Intent(this, HomeProfesor.class)
                : new Intent(this, HomeEstudiante.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void redireccionarAlPerfilCorrecto() {
        Intent intent = (rolUsuario != null && rolUsuario.equals("profesor"))
                ? new Intent(this, PerfilProfesor.class)
                : new Intent(this, PerfilEstudiante.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsValueListener != null) {
            mDatabase.child("Chats").removeEventListener(chatsValueListener);
        }
    }
}