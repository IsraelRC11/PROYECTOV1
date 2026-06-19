package empresa.android.proyectov1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import empresa.android.proyectov1.R;
import empresa.android.proyectov1.models.MensajeModel;

public class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.ViewHolder> {

    private List<MensajeModel> mList;
    private static final int MSG_DERECHA = 1;
    private static final int MSG_IZQUIERDA = 0;

    public MensajeAdapter(List<MensajeModel> mList) {
        this.mList = mList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el XML según quién envió el mensaje
        if (viewType == MSG_DERECHA) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje_propio, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje_recibido, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MensajeModel m = mList.get(position);

        holder.tvMensaje.setText(m.getMensaje());

        // Convertimos el timestamp de Firebase a formato 10:30 AM
        if (m.getTimestamp() != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String horaFormateada = sdf.format(new Date(m.getTimestamp()));
            holder.tvHora.setText(horaFormateada);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // Validación para evitar errores si el UID es null
        String miUid = FirebaseAuth.getInstance().getUid();
        if (miUid != null && mList.get(position).getEmisorUid().equals(miUid)) {
            return MSG_DERECHA;
        } else {
            return MSG_IZQUIERDA;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMensaje, tvHora;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Buscamos los IDs que tú pusiste en tus XML
            // Primero intentamos con los de "propio"
            tvMensaje = itemView.findViewById(R.id.tvTextoMensajePropio);
            tvHora = itemView.findViewById(R.id.tvHoraMensajePropio);

            // Si son null, significa que estamos en el XML de "recibido"
            if (tvMensaje == null) {
                tvMensaje = itemView.findViewById(R.id.tvTextoMensajeOtro);
                tvHora = itemView.findViewById(R.id.tvHoraMensajeOtro);
            }
        }
    }
}