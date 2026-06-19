package empresa.android.proyectov1;

import java.util.List;

public class Profesor {
    private String uid;
    private String nombre;
    private String apellido;
    private String codigo; // Coherencia con Estudiante
    private String rol;
    private String fotoUrl;
    private List<String> especialidades; // Cambiado a Lista para Firebase
    private float rating;
    private int sesiones;

    public Profesor() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public List<String> getEspecialidades() { return especialidades; }
    public void setEspecialidades(List<String> especialidades) { this.especialidades = especialidades; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public int getSesiones() { return sesiones; }
    public void setSesiones(int sesiones) { this.sesiones = sesiones; }
}