package com.rick.tecladoporomongueta

/**
 * Tabela chave -> contagem, com limite de tamanho e persistência em um [TextStorage].
 * As chaves não podem ter tabulação nem quebra de linha.
 */
class FrequencyStore(
    private val storage: TextStorage,
    private val maxEntries: Int
) {

    private val counts = LinkedHashMap<String, Int>()
    private var loaded = false

    /** Aumenta a contagem de [key]. Devolve false se a chave for inválida. */
    fun increment(key: String): Boolean {
        if (key.isEmpty() || key.any { it == '\t' || it == '\n' || it == '\r' }) return false
        ensureLoaded()
        counts[key] = (counts[key] ?: 0) + 1
        prune(keep = key)
        return true
    }

    fun get(key: String): Int {
        ensureLoaded()
        return counts[key] ?: 0
    }

    fun entries(): Map<String, Int> {
        ensureLoaded()
        return counts
    }

    fun size(): Int {
        ensureLoaded()
        return counts.size
    }

    fun clear() {
        counts.clear()
        loaded = true
        storage.delete()
    }

    fun serialize(): String =
        counts.entries.joinToString("\n") { "${it.value}\t${it.key}" }

    fun save() {
        ensureLoaded()
        storage.write(serialize())
    }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val text = storage.read() ?: return
        text.lineSequence().forEach { line ->
            val tab = line.indexOf('\t')
            if (tab > 0) {
                val count = line.substring(0, tab).toIntOrNull()
                val key = line.substring(tab + 1)
                if (count != null && count > 0 && key.isNotEmpty()) counts[key] = count
            }
        }
    }

    private fun prune(keep: String) {
        if (counts.size <= maxEntries) return
        counts.entries
            .filter { it.key != keep }
            .sortedBy { it.value }
            .take(counts.size - maxEntries)
            .map { it.key }
            .forEach { counts.remove(it) }
    }
}
