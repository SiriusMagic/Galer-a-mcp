# Arquitectura de Luma

Carpeta = package (`android/app/src/main/java/app/luma/gallery/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | `Media` y `Album` (modelos), `Library` (reglas puras: álbumes, búsqueda, favoritas; con pruebas), `MediaStoreSource` (lee MediaStore fuera del hilo principal), `Favorites` (preferencias locales), `MediaWriter` (copiar, mover, PDF), `Vault` (archivos ocultos privados) y `Memories` (reglas de las memorias, con pruebas). |
| `index/` | Buscar sin ver: `Facts` (etiquetas + texto por foto), `SqlFacts` (SQLite local, consultas con parámetros), `MlKitAnalyzer` (ML Kit dentro de la app), `Indexer` (nuevas, borradas, errores), `IndexWorker` (cargando y con la pantalla apagada, o «Analizar ahora»), `ContentSearch` y `Labels` (búsqueda en español). |
| `ui/` | `GalleryState` (único dueño del estado: página, álbum, búsqueda, visor) y `LumaApp` (barra superior, barra inferior, navegación hacia atrás). |
| `ui/` (cont.) | `Chrome` (título grande, píldora de acciones, barra flotante y barra de selección) y `Actions` (lo que necesita Android). |
| `ui/screens/` | `MediaGrid` (por días), `AlbumsScreen`, `AlbumPicker`, `MemoriesScreen`, `VaultScreen`, `SearchScreen`, `SettingsScreen`, `Viewer`, `DetailsDialog` y `PermissionScreen`. Solo pintan y avisan. |
| `ui/theme/` | Colores del concepto, claro y oscuro. |
| `VaultLock.kt` | Pide el bloqueo del teléfono (BiometricPrompt) antes de abrir la Bóveda. |
| `MainActivity.kt` | Permisos, carga, compartir, editar, reproducir y borrar (con la confirmación del sistema). |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
- Nada sale del teléfono: sin permiso de internet (el manifiesto quita el que añade ML Kit) hasta la fase MCP, y entonces
  solo hacia el relay.
