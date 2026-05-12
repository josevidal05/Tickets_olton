package com.example.tickets_android;

public class Conexiones {
    // Aquí puedes cambiar la URL base una sola vez para toda la aplicación
    public static final String BASE_URL = "http://192.168.0.151:8000";

    // Endpoints específicos para cada funcionalidad

    // iniciar sesion
    public static final String LOGIN_URL = BASE_URL + "/android/login/";

    // registro de usuarios
    public static final String REGISTRO_URL = BASE_URL + "/android/registrar_usuario/";

    // publicar tickets
    public static final String TICKETS_URL = BASE_URL + "/android/tickets/";

    // cerrar sesión
    public static final String LOGOUT = BASE_URL + "/android/logout/";

    // obtener tickets por ID
    public static final String TICKETS_ID = BASE_URL + "/android/tickets/<int:ticket_id>/";

    // obtener tickets por usuario
    public static final String TICKET_USUARIO = BASE_URL + "/android/tickets_usuario/";

    //
    //public static final String  = BASE_URL + "/android/tickets/";




}
