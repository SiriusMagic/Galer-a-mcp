# Arquitectura de Luma

Carpeta = package (`android/app/src/main/java/app/luma/gallery/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | `Media` y `Album` (modelos), `Library` (reglas puras: álbumes, búsqueda, favoritas; con pruebas), `MediaStoreSource` (lee MediaStore fuera del hilo principal), `Favorites` (preferencias locales), `MediaWriter` (copiar, mover, PDF), `Vault` (archivos ocultos privados) y `Memories` (reglas de las memorias, con pruebas). |
| `index/` | Buscar sin ver: `Facts` (etiquetas + texto por foto), `SqlFacts` (SQLite local, consultas con parámetros), `MlKitAnalyzer` (ML Kit dentro de la app), `Indexer` (nuevas, borradas, errores), `IndexWorker` (cargando y con la pantalla apagada, o «Analizar ahora»), `ContentSearch` y `Labels` (búsqueda en español). |
| `edit/` | El editor de fotos. Reglas puras con pruebas: `EditModel` (`Tool`, `Brush`, trazos, textos, marco de recorte, borrador, flechas) y `ColorMath` (ajustes, filtros y «Mejorar» como matrices de color; `Pixels`: histograma y nitidez). `Render` dibuja con el Canvas de Android lo mismo en pantalla y al guardar (recorte, giro, colores, trazos, mosaico, texto). |
| `ui/edit/` | `EditorState` (único dueño del editor: foto actual, deshacer, borrador de cada apartado; ✓ lo pinta, ✕ lo descarta), `EditorScreen` (barra superior, foto y apartados), `EditCanvas` (foto ajustada, lo que se dibuja encima y los gestos), `DrawPanels` (Dibujar, Mosaico, grosor y colores) y `ToolPanels` (Estudio IA, Recortar, Ajustes, Filtros, Texto). |
| `mcp/` | Modo MCP: `Tokens` (LLM/Hub/Redirect, cifrados con `SecretBox`), `GalleryTools` (las herramientas `gallery_*`), `McpHandler` (JSON-RPC, tokens, límites), `RelayLink` (WebSocket al relay de Stone), `McpMode` (estado e interruptores) y `HubLink` (enlace para Gateway Hub con firma). |
| `ui/` | `GalleryState` (único dueño del estado: página, álbum, búsqueda, visor) y `LumaApp` (barra superior, barra inferior, navegación hacia atrás). |
| `ui/` (cont.) | `Chrome` (título grande, píldora de acciones, barra flotante y barra de selección) y `Actions` (lo que necesita Android). |
| `ui/screens/` | `MediaGrid` (por días), `AlbumsScreen`, `AlbumPicker`, `MemoriesScreen`, `VaultScreen`, `SearchScreen`, `SettingsScreen`, `Viewer`, `DetailsDialog` y `PermissionScreen`. Solo pintan y avisan. |
| `ui/theme/` | Colores del concepto, claro y oscuro. |
| `VaultLock.kt` | Pide el bloqueo del teléfono (BiometricPrompt) antes de abrir la Bóveda. |
| `MainActivity.kt` | Permisos, carga, compartir, editar, reproducir y borrar (con la confirmación del sistema). |

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
- «Editar» nunca toca la foto original: «Guardar copia» crea un JPEG nuevo («… editada») en la misma carpeta. Estudio IA se
  calcula en el teléfono con la propia foto (sin internet ni modelos externos).
- Nada sale del teléfono salvo por el Modo MCP que el usuario active, y solo hacia el relay. El manifiesto quita el
  destino de las estadísticas de ML Kit.
