package com.rick.tecladoporomongueta

/** [TextStorage] em memória, para os testes. */
class MemoryStorage(var text: String? = null) : TextStorage {
    var writes = 0

    override fun read(): String? = text

    override fun write(text: String) {
        this.text = text
        writes++
    }

    override fun delete() {
        text = null
    }
}
