# Reproductor MP3 Android

Este es un proyecto de una aplicación de Android que implementa un reproductor de MP3 con soporte de notificaciones, transmisión de canciones a través de un servicio, y control de reproducción multimedia (play, pause, next, previous). La aplicación se conecta a una API para obtener una lista de canciones.

## Funcionalidades

- **Reproducción de música en segundo plano:** La clase `MusicService` gestiona la reproducción de las canciones, permitiendo que la música continúe incluso cuando la app no está en primer plano.
- **Control multimedia a través de notificaciones:** Permite controlar la reproducción desde las notificaciones, incluyendo acciones como `play`, `pause`, `next`, y `previous`.
- **Manejo de la conectividad:** Si la conexión a internet se pierde, la reproducción se pausa y se reanuda automáticamente cuando la conexión es restablecida.
- **Interfaz de usuario simple:** La clase `MainActivity` ofrece botones para controlar la reproducción (play, pause, stop, siguiente, anterior) y muestra el título y autor de la canción actual.
- **API de canciones:** La aplicación se conecta a una API mediante Retrofit para descargar una lista de reproducción.

## Estructura del Proyecto

- `MainActivity`: Actividad principal que muestra la interfaz de usuario para controlar la reproducción de música.
- `MusicService`: Servicio que gestiona la reproducción de música, las notificaciones y las acciones multimedia.
- `Song`: Modelo de datos que representa una canción con título, autor y URL.
- `ApiService`: Interfaz para realizar solicitudes HTTP a la API de la lista de reproducción.
- `ApiClient`: Cliente Retrofit para interactuar con la API.

## Requisitos Previos

- Android Studio 4.0 o superior.
- SDK de Android API nivel 21 o superior.
- Conexión a internet para obtener la lista de canciones de la API.

## Instalación

1. Clona este repositorio:
   ```bash
   git clone https://github.com/usuario/reproductor-mp3.git
2. Abre el proyecto en Android Studio.
3. Configura las dependencias en el archivo build.gradle si es necesario (Retrofit, MediaPlayer).
4. Ejecuta la aplicación en un emulador o dispositivo físico.

## Uso
1. Abre la aplicación.
2. La aplicación descargará automáticamente una lista de canciones desde la API.
3. Controla la reproducción usando los botones de Play, Pause, Next, y Previous.
4. La reproducción también puede ser controlada desde las notificaciones mientras la música está en segundo plano.

## Clases Principales

## MusicService
El servicio MusicService se encarga de:

- Reproducir, pausar y detener canciones.
- Controlar las acciones desde las notificaciones (play/pause, next/previous).
- Manejar la conectividad a internet y ajustar la reproducción en consecuencia.

## MainActivity
La MainActivity contiene:

- Interfaz gráfica con botones para controlar la reproducción.
- Muestra el título y el autor de la canción actual.
- Envía comandos al MusicService para controlar la reproducción de música.
