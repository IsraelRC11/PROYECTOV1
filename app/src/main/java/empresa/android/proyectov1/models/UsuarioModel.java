package empresa.android.proyectov1.models;

import java.util.List;

public class UsuarioModel {
    // Datos básicos compartidos
    private String uid;
    private String nombre;
    private String apellido;
    private String codigo;
    private String correo;
    private String rol; // "estudiante" o "profesor"
    private String fotoUrl;

    // Datos específicos de perfil
    private List<String> intereses; // Para estudiantes
    private List<String> especialidades; // Para profesores

    // Objeto anidado para el Dashboard del Profesor
    private EstadisticasModel estadisticas;

    // Constructor vacío requerido por Firebase
    public UsuarioModel() {}

    // Getters y Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public List<String> getIntereses() { return intereses; }
    public void setIntereses(List<String> intereses) { this.intereses = intereses; }

    public List<String> getEspecialidades() { return especialidades; }
    public void setEspecialidades(List<String> especialidades) { this.especialidades = especialidades; }

    public EstadisticasModel getEstadisticas() { return estadisticas; }
    public void setEstadisticas(EstadisticasModel estadisticas) { this.estadisticas = estadisticas; }

    // Clase interna para mapear las estadísticas del Dashboard
    public static class EstadisticasModel {
        private long totalSesiones;
        private long totalEstudiantes;
        private long totalHoras;
        private float sumaCalificaciones;
        private long citasCompletadas;
        private long citasAgendadas;

        public EstadisticasModel() {}

        public long getTotalSesiones() { return totalSesiones; }
        public long getTotalEstudiantes() { return totalEstudiantes; }
        public long getTotalHoras() { return totalHoras; }
        public float getSumaCalificaciones() { return sumaCalificaciones; }
        public long getCitasCompletadas() { return citasCompletadas; }
        public long getCitasAgendadas() { return citasAgendadas; }
    }
}