package empresa.android.proyectov1.models;

public class MensajeModel {
    private String idMensaje;
    private String emisorUid;
    private String receptorUid;
    private String mensaje;
    private long timestamp;

    // Constructor vacío requerido por Firebase
    public MensajeModel() {
    }

    // Constructor completo para facilitar el envío
    public MensajeModel(String idMensaje, String emisorUid, String receptorUid, String mensaje, long timestamp) {
        this.idMensaje = idMensaje;
        this.emisorUid = emisorUid;
        this.receptorUid = receptorUid;
        this.mensaje = mensaje;
        this.timestamp = timestamp;
    }

    // Getters y Setters
    public String getIdMensaje() { return idMensaje; }
    public void setIdMensaje(String idMensaje) { this.idMensaje = idMensaje; }

    public String getEmisorUid() { return emisorUid; }
    public void setEmisorUid(String emisorUid) { this.emisorUid = emisorUid; }

    public String getReceptorUid() { return receptorUid; }
    public void setReceptorUid(String receptorUid) { this.receptorUid = receptorUid; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}