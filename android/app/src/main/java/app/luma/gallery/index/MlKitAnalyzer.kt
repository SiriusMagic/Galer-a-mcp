package app.luma.gallery.index

import android.content.Context
import android.net.Uri
import android.util.Size
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * ML Kit with its models inside the app: no internet, nothing leaves the phone. Works on a 640 px thumbnail (videos:
 * a frame), so it is light enough to run while charging.
 */
class MlKitAnalyzer(context: Context) : Analyzer {
    private val resolver = context.contentResolver
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(MIN_CONFIDENCE).build())
    private val reader = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyze(uri: Uri): Pair<Map<String, Float>, String> {
        val bitmap = resolver.loadThumbnail(uri, Size(640, 640), null)
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val labels = labeler.process(image).await().associate { it.text.lowercase() to it.confidence }
            val text = reader.process(image).await().text.replace(Regex("\\s+"), " ").trim().take(MAX_TEXT)
            return labels to text
        } finally { bitmap.recycle() }
    }

    fun close() { labeler.close(); reader.close() }

    private companion object { const val MIN_CONFIDENCE = 0.6f; const val MAX_TEXT = 2_000 }
}
