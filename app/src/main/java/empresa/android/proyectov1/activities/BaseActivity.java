package empresa.android.proyectov1.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BaseActivity extends AppCompatActivity {

    protected DatabaseReference mDatabaseRef;
    protected String currentUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cuando cualquier pantalla pase al frente, asegura el estado online
        if (currentUid != null) {
            mDatabaseRef.child("Usuarios").child(currentUid).child("estado").setValue("online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cuando cambias de pantalla, dejas un estado temporal en "online"
        // para que no parpadee mientras se abre la siguiente actividad.
        if (currentUid != null) {
            mDatabaseRef.child("Usuarios").child(currentUid).child("estado").setValue("online");
        }
    }

    // El truco definitivo: Usamos onDisconnect de Firebase para que el servidor
    // lo ponga "offline" automáticamente SOLO si cierra la app por completo o pierde internet.
    public void configurarDesconexionAutomatica() {
        if (currentUid != null) {
            mDatabaseRef.child("Usuarios").child(currentUid).child("estado").onDisconnect().setValue("offline");
        }
    }
}