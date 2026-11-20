# Luma (Galería MCP): reglas para agentes

- Lee `ARQUITECTURA.md` y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Luma-<versión>.apk` (borra el anterior)
  y anótalo en `CHANGELOG.md`.
- Privacidad: solo leer fotos y videos; nada sale del teléfono salvo por el Modo MCP que el usuario active.
- Firma: la de depuración del entorno, igual que Stone y Gateway Hub (el hub reconoce al mismo desarrollador).
