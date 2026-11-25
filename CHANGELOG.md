# Luma 0.6.4

- **Se llama «Galería»** en la pantalla de inicio y en los textos de la app (el paquete no cambia: se instala encima).
- **Icono de la app**: se quita «Original» (la foto). Quedan Claro (el de siempre a partir de ahora), Dorado, Azul y
  Verde. Si tenías «Original», pasa solo a Claro; si tenías Claro, se mantiene.

# Luma 0.6.3

- **Conectar todas** (Gateway Hub 0.0.21): con un toque en el Hub se activa aquí «Redireccionar a Hub» y la app queda
  vinculada, sin entrar en ella. Solo lo puede pedir una app firmada por el mismo desarrollador.
- **Lucía** (la voz del Hub) usa las herramientas de esta app en el teléfono, **sin internet**, con las mismas reglas que
  por MCP. Solo mientras «Redireccionar a Hub» esté activado.
- **Icono de la app**: 5 iconos para elegir (Original, Claro, Dorado, Azul y Verde) en Ajustes › Icono de la app. Tras esta
  actualización quizá tengas que volver a poner el icono en tu pantalla de inicio (una sola vez).

# Luma 0.6.2

- **Buscar:** una escoba al final de la caja de búsqueda borra lo escrito de un toque (aparece solo cuando hay texto).
- 24 pruebas.

# Luma 0.6.1

- **Ícono nuevo:** fotos apiladas con un atardecer sobre las montañas, en un marco luminoso morado y azul. El marco sigue
  la forma del lanzador (redonda o cuadrada); con los íconos temáticos de Android 13+ se usa la versión de un color.
- 23 pruebas.

# Luma 0.6.0

- **Los videos se ven:** las miniaturas de video ya se dibujan (faltaba el decodificador de fotogramas) y el ▶ del visor
  los reproduce **dentro de Luma**, con pausa y barra de tiempo.
- **Abrir con…** muestra primero **Stone** (recomendado, mismo desarrollador; necesita Stone 1.7.23) y después los demás
  reproductores o visores instalados, por nombre.
- MCP: nueva herramienta `gallery_by_date` (entre dos fechas) y `gallery_recent` acepta orden (más nuevas o más viejas)
  y tipo (fotos o videos).
- 23 pruebas.

# Luma 0.5.0

- **Modo MCP** (Ajustes › Modo MCP), como en Stone: Activar MCP, **Crear token** (LLM o Hub, uso, solo lectura o
  lectura y escritura, validez), pausar, revocar y **Redireccionar a Hub** («Ir al Hub»; Gateway Hub vincula Luma sola).
  Tu IA busca en tus fotos sin verlas: herramientas `gallery_search`, `gallery_recent`, `gallery_albums`, `gallery_info`,
  `gallery_stats` y `gallery_favorite`. Nunca recibe la imagen, ni puede borrar o mover.
- Llega por el mismo relay que Stone (enlace `…/d/<luma>/mcp?token=lum_…`); en el hub aparece como `gallery_`.
- Internet solo para el Modo MCP (apagado por defecto); las estadísticas de uso de ML Kit quedan bloqueadas.
- **Íconos y textos blancos en modo oscuro** (salían negros).
- **Temas:** Sistema, Claro u Oscuro y cinco colores.
- **Pellizcar** en Fotos agrupa por meses y otra vez por años; abrir los dedos vuelve.
- **Deslizar para seleccionar:** mantén pulsada una foto y pasa el dedo por otras.
- 21 pruebas (incluye el MCP: tokens, herramientas, errores, límites y el enlace al hub).

# Luma 0.4.0

- **Nuevo álbum (+)** en Álbumes: le pones nombre, eliges las fotos y decides **Mover** o **Copiar**.
- **Mover o copiar a un álbum** desde la selección o desde el visor (⋮). Mover pide la confirmación de Android.
- **Ocultar / Bóveda:** lo que ocultas sale de la galería y queda dentro de Luma (nunca en copias de seguridad). La
  Bóveda (en Álbumes) se abre solo con el bloqueo del teléfono (huella, cara, PIN…) y se cierra al salir; desde ahí,
  restaurar a su álbum o borrar para siempre.
- **Exportar a PDF** (una foto por página, A4) en Documentos/Luma, desde la selección o el visor.
- **Memorias** (pestaña nueva): «Un día como hoy», los últimos meses y temas que aparecen mucho (perros, playa…).
- 14 pruebas (memorias, bóveda, nombres de álbum, nuevo álbum, elegir álbum y capturas).

# Luma 0.3.0

- **Búsqueda inteligente (buscar sin ver):** Luma analiza tus fotos dentro del teléfono con ML Kit (sin internet) y
  guarda lo que aparece (perro, playa, comida…) y el texto de la imagen. Buscas en español («perro en la playa»,
  «supermercado», «cumpleaños») y salen ordenadas por parecido. Sugerencias con lo que más aparece.
- El análisis se hace solo con el teléfono **cargando y la pantalla apagada**; en Ajustes: «Analizar ahora», «Detener»
  y «Borrar análisis».
- **Fotos por día** («Hoy», «Ayer», fecha) en 4 columnas, con la **duración de los videos**.
- **Selección múltiple** (mantener pulsado o «Seleccionar todo»): compartir, favoritas y borrar varias a la vez.
- **Álbumes:** Todas, **Favoritos** (con su corazón), Cámara, Videos y Capturas; debajo «Más álbumes».
- **Eliminado recientemente** (Android 11+): borrar mueve a la papelera de Android (30 días); desde ahí, restaurar o
  borrar para siempre. Android pide confirmación siempre.
- **Visor:** fecha y hora arriba; abajo compartir, corazón, editar, borrar y **⋮** con Detalles (incluye lo que Luma
  ve en la foto), Usar como fondo, Copiar al portapapeles y Abrir con.
- **Ajustes** nuevos, barra inferior flotante y botón de buscar aparte, como la galería del teléfono.
- Sin permiso de internet (se quita el que traería ML Kit). APK para teléfonos de 64 bits (unos 30 MB).
- 9 pruebas (biblioteca, selección, papelera, índice, búsqueda en español y capturas).

# Luma 0.2.0

- Base reorganizada por carpetas (datos, estado y pantallas) con las mismas herramientas que Stone.
- Las fotos se cargan fuera del hilo principal y se actualizan al volver a la app.
- Visor a pantalla completa como en el concepto: deslizar, favorita, compartir, editar y borrar (con la confirmación
  de Android). Los videos se abren en el reproductor.
- «Favoritas» muestra de portada una favorita de verdad y solo aparece si hay alguna.
- La búsqueda pide que estén todas las palabras (nombre o álbum).
- Tema oscuro, inglés, icono propio, sin copias de seguridad en la nube.
- 7 pruebas (reglas de la biblioteca, navegación hacia atrás y capturas).

# Luma 0.1.0

- Primera versión de trabajo (MVP).
