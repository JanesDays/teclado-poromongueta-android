package com.rick.tecladoporomongueta

/** Onde um texto é guardado (arquivo criptografado, arquivo comum, memória nos testes). */
interface TextStorage {
    fun read(): String?
    fun write(text: String)
    fun delete()
}
