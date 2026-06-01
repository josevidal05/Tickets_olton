# Tickets Olton - Sistema Multiplataforma de Gestión de Incidencias

Este proyecto consiste en una aplicación multiplataforma (Web y Móvil Android) diseñada a medida para la empresa **Sistemas Olton**, destinada a registrar, clasificar y realizar el seguimiento de incidencias técnicas en maquinaria, localizadores (trackers) y otros dispositivos.

El sistema utiliza un único servidor centralizado basado en **Django** que ofrece una interfaz de administración web y expone una **API REST (JSON)** para ser consumida por el cliente móvil desarrollado de forma nativa en **Android (Java)**.

---

## Estructura del Proyecto

El repositorio está organizado en las siguientes carpetas principales:

*   **`proyecto/`**: Contiene el código fuente del servidor backend desarrollado con el framework Django (Python).
    *   `app/`: Módulo de la aplicación que contiene los modelos (`models.py`), vistas web y API (`views.py`, `views_ad.py`) y plantillas HTML.
    *   `media/`: Carpeta donde se almacenan las imágenes adjuntas a los tickets.
    *   `db.sqlite3`: Base de datos relacional SQLite con la estructura y datos del proyecto.
*   **`tickets_android/`**: Proyecto nativo de Android Studio desarrollado en Java y estructurado mediante layouts XML.
*   **`MEMORIA_PROYECTO_DAM.md`**: Memoria técnica completa y documentada bajo los estándares del ciclo formativo de DAM.

---

## Requisitos de Ejecución

### 1. Servidor Backend (Django)
*   **Python 3.10** o superior.
*   **Pip** (gestor de paquetes de Python).
*   Librerías requeridas: **Django** (versión 6.x o compatible) y **Bcrypt** (para cifrado de contraseñas).

### 2. Cliente Móvil (Android)
*   **Android Studio** (versión Narwhal 2025.1.1 o superior).
*   **Java Development Kit (JDK 17)**.
*   Dispositivo físico Android o Emulador (mínimo Android 5.0 Lollipop - API 21).

---

## Instrucciones de Configuración e Inicio

### Paso 1: Puesta en marcha del Servidor Backend (Django)

1.  Abre una terminal de comandos en la carpeta raíz del servidor backend:
    ```bash
    cd "proyecto"
    ```

2.  Es altamente recomendable crear un entorno virtual para no interferir con otras instalaciones de Python:
    ```bash
    # Crear el entorno virtual
    python -m venv venv

    # Activar el entorno virtual (Windows)
    .\venv\Scripts\activate

    # Activar el entorno virtual (Linux/macOS)
    source venv/bin/activate
    ```

3.  Instala las dependencias necesarias:
    ```bash
    pip install django bcrypt
    ```

4.  Realiza las migraciones del modelo de base de datos para asegurar que SQLite está sincronizado (opcional si ya dispones de `db.sqlite3` configurado):
    ```bash
    python manage.py makemigrations app
    python manage.py migrate
    ```

5.  *(Opcional)* Si deseas acceder al panel de administración integrado de Django (`/admin`), crea un superusuario administrador:
    ```bash
    python manage.py createsuperuser
    ```

6.  Inicia el servidor de desarrollo en modo escucha local o externa:
    ```bash
    # Ejecución básica (local)
    python manage.py runserver

    # Ejecución para conexión móvil (importante: expone el servidor a tu red local)
    python manage.py runserver 0.0.0.0:8000
    ```
    *Nota: Si ejecutas con `0.0.0.0`, asegúrate de conocer la IP local de tu ordenador (por ejemplo, `192.168.1.50`) para enlazar el cliente móvil.*

---

### Paso 2: Puesta en marcha de la App de Android

1.  Abre **Android Studio**.
2.  Importa el proyecto seleccionando la carpeta **`tickets_android/`**.
3.  Espera a que Gradle sincronice las dependencias del proyecto (descargará la librería de peticiones HTTP **Volley** y dependencias del sistema).
4.  **Configuración de la URL de conexión:**
    *   Abre el archivo de configuración de red en `app/src/main/java/com/example/tickets_android/Conexiones.java`.
    *   Modifica el valor de `BASE_URL` dependiendo de tu entorno de ejecución:
        ```java
        // Si utilizas el Emulador interno de Android Studio:
        public static final String BASE_URL = "http://10.0.2.2:8000";

        // Si utilizas un Teléfono móvil físico conectado a la misma red WiFi:
        public static final String BASE_URL = "http://<TU_IP_LOCAL>:8000"; // Ejemplo: "http://192.168.1.50:8000"
        ```
5.  Compila y ejecuta la aplicación en tu emulador o dispositivo móvil pulsando el botón **Run** (flecha verde) en Android Studio.

---

##  Cuentas y Acceso de Prueba

*   **Registro de Usuarios:** Puedes crear nuevas cuentas de acceso directamente desde la interfaz web accediendo a `http://localhost:8000/registro/` o bien desde el botón "Registrarse" de la pantalla de bienvenida en la app móvil.
*   Al registrar un usuario, si la **empresa** que introduces no existe, el servidor la creará automáticamente en la tabla correspondiente, asignando al nuevo usuario como su encargado.
*   Para iniciar sesión, introduce el nombre de usuario y contraseña creados.

---

## Listado de Endpoints API REST (Android)

Todos los endpoints REST utilizados por el cliente móvil devuelven y aceptan objetos en formato JSON (salvo el envío de adjuntos, que utiliza `multipart/form-data`) y requieren la cabecera `Session` con el token de sesión tras loguearse:

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| **POST** | `/android/login/` | Inicia sesión y devuelve el token de sesión. |
| **POST** | `/android/registrar_usuario/` | Registra una nueva cuenta de usuario y empresa asociada. |
| **GET** | `/android/tickets/` | Obtiene el listado de todos los tickets. |
| **POST** | `/android/tickets/` | Crea una nueva incidencia (acepta imagen adjunta en campo `archivo`). |
| **GET** | `/android/tickets/<int:id>/` | Obtiene el detalle técnico de un ticket por ID. |
| **POST** | `/android/tickets/<int:id>/` | Modifica un ticket usando un formulario `multipart/form-data` (Android). |
| **PUT** | `/android/tickets/<int:id>/` | Modifica un ticket enviando datos en formato JSON raw. |
| **DELETE** | `/android/tickets/<int:id>/` | Elimina permanentemente un ticket del sistema. |
| **GET** | `/android/tickets_usuario/` | Obtiene los tickets creados exclusivamente por el usuario activo. |
| **GET** | `/android/perfil/` | Obtiene los datos del perfil del usuario autenticado. |
| **PUT** | `/android/perfil/` | Permite actualizar datos del perfil (nombre, correo, contraseña, etc.). |
| **POST** | `/android/logout/` | Destruye el token de sesión del usuario en el servidor. |
