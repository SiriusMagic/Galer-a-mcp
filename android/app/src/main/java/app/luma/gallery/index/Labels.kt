package app.luma.gallery.index

/**
 * ML Kit labels are English. This maps them to Spanish (to show) and Spanish words back to them (to search), for the
 * labels people search most; any other label still matches when it is typed in English.
 */
object Labels {
    private val ES = mapOf(
        "person" to "persona", "selfie" to "selfie", "smile" to "sonrisa", "baby" to "bebé", "child" to "niño",
        "crowd" to "multitud", "team" to "equipo", "beard" to "barba", "hair" to "pelo", "eyelash" to "pestañas",
        "dog" to "perro", "cat" to "gato", "bird" to "pájaro", "horse" to "caballo", "fish" to "pez", "insect" to "insecto",
        "butterfly" to "mariposa", "cattle" to "vaca", "pet" to "mascota", "duck" to "pato", "bear" to "oso",
        "food" to "comida", "cake" to "pastel", "bread" to "pan", "pizza" to "pizza", "fruit" to "fruta", "vegetable" to "verdura",
        "coffee" to "café", "cup" to "taza", "drink" to "bebida", "wine" to "vino", "beer" to "cerveza", "dessert" to "postre",
        "pasta" to "pasta", "meal" to "plato", "cookie" to "galleta", "candy" to "dulce", "egg" to "huevo", "juice" to "jugo",
        "sky" to "cielo", "cloud" to "nube", "sunset" to "atardecer", "beach" to "playa", "sea" to "mar", "lake" to "lago",
        "river" to "río", "mountain" to "montaña", "waterfall" to "cascada", "forest" to "bosque", "tree" to "árbol",
        "flower" to "flor", "plant" to "planta", "garden" to "jardín", "grass" to "césped", "snow" to "nieve", "rainbow" to "arcoíris",
        "desert" to "desierto", "rock" to "roca", "sand" to "arena", "moon" to "luna", "star" to "estrella", "fireworks" to "fuegos artificiales",
        "car" to "carro", "bicycle" to "bicicleta", "motorcycle" to "moto", "bus" to "bus", "airplane" to "avión", "boat" to "barco",
        "train" to "tren", "wheel" to "rueda", "road" to "carretera", "bridge" to "puente", "building" to "edificio",
        "skyscraper" to "rascacielos", "church" to "iglesia", "temple" to "templo", "castle" to "castillo", "house" to "casa",
        "room" to "habitación", "kitchen" to "cocina", "bedroom" to "dormitorio", "couch" to "sofá", "bed" to "cama",
        "chair" to "silla", "table" to "mesa", "desk" to "escritorio", "door" to "puerta", "window" to "ventana", "stairs" to "escaleras",
        "screenshot" to "captura", "poster" to "póster", "text" to "texto", "paper" to "papel", "receipt" to "recibo",
        "menu" to "menú", "newspaper" to "periódico", "comics" to "cómic", "cartoon" to "dibujo", "handwriting" to "escritura",
        "computer" to "computadora", "laptop" to "portátil", "mobile phone" to "celular", "television" to "televisor",
        "monitor" to "monitor", "keyboard" to "teclado", "musical instrument" to "instrumento", "guitar" to "guitarra",
        "piano" to "piano", "toy" to "juguete", "balloon" to "globo", "bag" to "bolso", "glasses" to "gafas", "sunglasses" to "lentes de sol",
        "hat" to "sombrero", "shoe" to "zapato", "jacket" to "chaqueta", "dress" to "vestido", "jeans" to "jeans", "tattoo" to "tatuaje",
        "party" to "fiesta", "concert" to "concierto", "wedding" to "boda", "christmas" to "navidad", "sports" to "deporte",
        "soccer" to "fútbol", "swimming" to "natación", "pool" to "piscina", "running" to "correr", "stadium" to "estadio",
        "money" to "dinero", "flag" to "bandera", "statue" to "estatua", "painting" to "pintura", "sketch" to "boceto", "museum" to "museo",
        "night" to "noche", "fun" to "diversión", "event" to "evento", "vehicle" to "vehículo", "tool" to "herramienta", "cable" to "cable",
    )
    /** Spanish word (and a few synonyms) → the English labels it means. */
    private val EN: Map<String, Set<String>> = buildMap<String, MutableSet<String>> {
        ES.forEach { (en, es) -> ContentSearch.fold(es).split(' ').forEach { getOrPut(it) { mutableSetOf() } += en } }
        mapOf("auto" to "car", "coche" to "car", "telefono" to "mobile phone", "movil" to "mobile phone", "torta" to "cake",
            "nino" to "child", "nina" to "child", "ninos" to "child", "perros" to "dog", "gatos" to "cat", "flores" to "flower",
            "ticket" to "receipt", "factura" to "receipt", "cumpleanos" to "cake", "comida" to "meal", "gente" to "person")
            .forEach { (es, en) -> getOrPut(es) { mutableSetOf() } += en }
    }

    fun spanish(label: String) = ES[label] ?: label
    fun toEnglish(word: String): Set<String> = (EN[word] ?: emptySet()) + word
}
