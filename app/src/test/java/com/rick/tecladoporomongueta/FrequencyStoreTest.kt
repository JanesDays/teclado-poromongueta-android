package com.rick.tecladoporomongueta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencyStoreTest {

    @Test
    fun incrementCountsAndPersists() {
        val storage = MemoryStorage()
        val store = FrequencyStore(storage, 10)
        store.increment("ana")
        store.increment("ana")
        store.increment("kûekatu")
        store.save()

        val reloaded = FrequencyStore(storage, 10)
        assertEquals(2, reloaded.get("ana"))
        assertEquals(1, reloaded.get("kûekatu"))
    }

    @Test
    fun invalidKeysAreRejected() {
        val store = FrequencyStore(MemoryStorage(), 10)
        assertFalse(store.increment(""))
        assertFalse(store.increment("com\ttab"))
        assertFalse(store.increment("com\nquebra"))
        assertEquals(0, store.size())
    }

    @Test
    fun leastUsedEntriesAreDroppedAtTheLimit() {
        val store = FrequencyStore(MemoryStorage(), 2)
        repeat(3) { store.increment("a") }
        repeat(2) { store.increment("b") }
        store.increment("c")

        assertEquals(2, store.size())
        assertEquals(3, store.get("a"))
        assertEquals(1, store.get("c"))
        assertEquals(0, store.get("b"))
    }

    @Test
    fun clearDeletesStoredData() {
        val storage = MemoryStorage("3\tana")
        val store = FrequencyStore(storage, 10)
        assertEquals(3, store.get("ana"))
        store.clear()
        assertEquals(0, store.size())
        assertNull(storage.text)
    }

    @Test
    fun corruptLinesAreIgnored() {
        val store = FrequencyStore(MemoryStorage("lixo\nx\tana\n-2\tneg\n4\tok\n\n"), 10)
        assertEquals(1, store.size())
        assertTrue(store.get("ok") == 4)
    }
}
