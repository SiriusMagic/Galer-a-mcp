# Galería MCP · Luma

**Luma** es una galería Android local, privada y sin publicidad. La meta del proyecto es que una IA (Claude,
ChatGPT…) pueda **buscar tus fotos sin verlas**: Luma guarda en el teléfono una descripción de cada imagen en forma de
**vectores** y la IA pregunta por texto («la foto del perro en la playa»), igual que Stone ofrece su música por MCP.

## Estado (0.6.1)

- **Búsqueda inteligente:** cada foto se analiza en el teléfono (ML Kit, sin internet): qué aparece y qué texto tiene.
  Se guarda como un vector local y se busca en español («perro en la playa», «recibo», «cumpleaños»).
- Fotos por día con duración de videos, selección múltiple, álbumes (Todas, Favoritos, Cámara, Videos, Capturas y
  Más álbumes), Eliminado recientemente con restaurar, visor con menú ⋮ y Detalles, y Ajustes.
- **Nuevo álbum, Mover y Copiar**, **Ocultar** en una **Bóveda** con el bloqueo del teléfono, **Exportar a PDF** y
  **Memorias** (un día como hoy, meses y temas).
- **Modo MCP:** tu IA busca en tus fotos sin verlas (`gallery_*`), con tokens LLM/Hub o **Redireccionar a Hub**.
- Temas, pellizcar por meses/años, deslizar para seleccionar, videos dentro de Luma y «Abrir con» con Stone primero.
- Solo permiso de **leer fotos y videos**; internet solo para el Modo MCP (apagado por defecto). Sin anuncios.

APK: [`android/Luma-0.6.1.apk`](android/Luma-0.6.1.apk) (`app.luma.gallery`, 64 bits, firmado como Stone y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Las capturas de las pantallas quedan en `android/app/build/screenshots/`.

## Siguiente

Ver [ROADMAP.md](ROADMAP.md): Modo MCP (tokens, permisos, analítica, «Redireccionar a Hub») y búsqueda por vectores.
