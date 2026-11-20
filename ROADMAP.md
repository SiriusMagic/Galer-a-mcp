# Hoja de ruta de Luma

## Fase 2 · Modo MCP (como Stone)
- Ajustes › **Modo MCP**: Activar MCP, Crear token (LLM o Hub, uso, validez, modo y herramientas), Analítica y
  **Redireccionar a Hub** (el hub administra todo).
- Herramientas `gallery_*`: buscar, listar álbumes, ver datos de una foto (fecha, tamaño, carpeta, descripción),
  favoritas, crear álbum, mover a la papelera (pide confirmación en el teléfono).
- Llega a la IA por el mismo relay de Stone (otro identificador de teléfono) y en el hub con el prefijo `gallery_`.

## Fase 3 · Buscar sin ver (vectores) — hecho en 0.3.0, pendiente lo marcado
- En el teléfono, sin internet: etiquetas de lo que aparece (perro, playa, comida…), texto dentro de la imagen
  (capturas, tickets) y fecha/lugar, guardados como **vectores** en una base local.
- La IA busca por texto («recibos de octubre», «fotos con mi perro») y recibe ids, descripción y datos, **nunca la
  imagen**, salvo que el token lo permita.
- Se indexa poco a poco, con el teléfono cargando, y se puede borrar el índice.
- Pendiente: caras (agrupar personas), lugar (GPS de la foto) y vectores densos para frases largas.

## Pendiente
- Paginación para bibliotecas muy grandes; presentación (pase de fotos) en Memorias.
