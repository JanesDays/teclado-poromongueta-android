package com.rick.tecladoporomongueta

import android.content.Context
import java.io.File

/** Arquivo de texto comum no armazenamento privado do app (para dados que não são sensíveis). */
class PlainTextStorage(context: Context, fileName: String) : TextStorage {

    private val file = File(context.applicationContext.filesDir, fileName)

    override fun read(): String? = try {
        if (file.exists()) file.readText(Charsets.UTF_8) else null
    } catch (e: Exception) {
        null
    }

    override fun write(text: String) {
        try {
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(text, Charsets.UTF_8)
            if (!temp.renameTo(file)) {
                file.writeText(text, Charsets.UTF_8)
                temp.delete()
            }
        } catch (e: Exception) {
            // sem espaço ou sem permissão: mantém só em memória
        }
    }

    override fun delete() {
        file.delete()
    }
}
