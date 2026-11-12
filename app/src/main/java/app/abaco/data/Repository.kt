package app.abaco.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

class AbacoRepository(context: Context) {
    private val dir = context.filesDir
    private val file = File(dir, "abaco.json")
    private val cache = File(dir, "abaco-cache.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun exists(): Boolean = file.exists() || cache.exists()

    fun load(): AppData {
        val source = when {
            file.exists() -> file
            cache.exists() -> cache
            else -> return seedData()
        }
        return try {
            decode(source.readText())
        } catch (_: Exception) {
            if (source != cache && cache.exists()) {
                try {
                    return decode(cache.readText())
                } catch (_: Exception) {
                    // La copia también falló: se siembra de nuevo.
                }
            }
            seedData()
        }
    }

    fun save(data: AppData) {
        val text = encode(data)
        writeAtomic(file, text)
        writeAtomic(cache, text)
    }

    fun encode(data: AppData): String = json.encodeToString(AppData.serializer(), data)

    fun decode(text: String): AppData = json.decodeFromString(AppData.serializer(), text)

    private fun writeAtomic(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.writeText(text)
            tmp.delete()
        }
    }
}
