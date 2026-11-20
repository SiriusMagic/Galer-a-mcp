# Galería MCP · Luma

**Luma** es una galería Android local, privada y sin publicidad. La meta del proyecto es que una IA (Claude,
ChatGPT…) pueda **buscar tus fotos sin verlas**: Luma guarda en el teléfono una descripción de cada imagen en forma de
**vectores** y la IA pregunta por texto («la foto del perro en la playa»), igual que Stone ofrece su música por MCP.

## Estado (0.2.0)

- Fotos (cuadrícula de 3), Álbumes (por carpeta, con «Favoritas» primero) y Buscar (por nombre o álbum).
- Visor a pantalla completa: deslizar entre fotos, favorita, compartir, editar con otra app y **borrar con la
  confirmación de Android**. Los videos se abren en el reproductor del teléfono.
- Solo permiso de **leer fotos y videos** (en Android 14+ puede ser solo una selección). Sin internet, sin anuncios,
  sin copias en la nube.
- Español e inglés; tema claro y oscuro con los colores del concepto (`design/concepto.png`).

APK: [`android/Luma-0.2.0.apk`](android/Luma-0.2.0.apk) (`app.luma.gallery`, firmado como Stone y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Las capturas de las pantallas quedan en `android/app/build/screenshots/`.

## Siguiente

Ver [ROADMAP.md](ROADMAP.md): Modo MCP (tokens, permisos, analítica, «Redireccionar a Hub») y búsqueda por vectores.
