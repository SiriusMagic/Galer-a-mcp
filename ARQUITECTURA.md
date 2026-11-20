# Arquitectura de Luma

Carpeta = package (`android/app/src/main/java/app/luma/gallery/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | `Media` y `Album` (modelos), `Library` (reglas puras: álbumes, búsqueda, favoritas; con pruebas), `MediaStoreSource` (lee MediaStore fuera del hilo principal), `Favorites` (preferencias locales). |
| `ui/` | `GalleryState` (único dueño del estado: página, álbum, búsqueda, visor) y `LumaApp` (barra superior, barra inferior, navegación hacia atrás). |
| `ui/screens/` | `MediaGrid`, `AlbumsScreen`, `SearchScreen`, `Viewer` (pantalla completa) y `PermissionScreen`. Solo pintan y avisan. |
| `ui/theme/` | Colores del concepto, claro y oscuro. |
| `MainActivity.kt` | Permisos, carga, compartir, editar, reproducir y borrar (con la confirmación del sistema). |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
- Nada sale del teléfono: sin permiso de internet hasta la fase MCP, y entonces solo hacia el relay.
