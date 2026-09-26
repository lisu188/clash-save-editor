package com.lis.clash.desktop

import com.lis.clash.editor.*
import kotlin.test.*

class MapViewportTest {
    @Test fun `rectangular map hit testing keeps physical 100 tile row stride`() {
        val viewport = MapViewport(10f, 20f, 30f)
        assertEquals(207, viewport.tileAt(95f, 55f, rows = 20, columns = 40))
        assertNull(viewport.tileAt(19f, 35f))
        assertNull(viewport.tileAt(421f, 35f, rows = 20, columns = 40))
        assertNull(viewport.tileAt(21f, 231f, rows = 20, columns = 40))
    }

    @Test fun `zoom keeps tile under pointer and pan translates hit test`() {
        val viewport = MapViewport(10f, 20f, 30f)
        val slot = viewport.tileAt(95f, 55f)
        val zoomed = viewport.zoom(2f, 95f, 55f)
        assertEquals(slot, zoomed.tileAt(95f, 55f))
        assertEquals(slot, zoomed.pan(200f, -20f).tileAt(295f, 35f))
    }

    @Test fun `fit is centered and zoom bounds are enforced`() {
        val viewport = MapViewport.fit(1000f, 600f, 20, 40)
        assertEquals(500f, viewport.x + 20 * viewport.cellSize, .01f)
        assertEquals(300f, viewport.y + 10 * viewport.cellSize, .01f)
        assertEquals(72f, viewport.zoom(1000f, 0f, 0f).cellSize)
        assertEquals(2f, viewport.zoom(.0001f, 0f, 0f).cellSize)
    }

    @Test fun `selection uses sparse physical id instead of displayed list row`() {
        val selected = RecordId(RecordKind.ARMY, 37)
        val entity = EntitySnapshot(selected, "Sparse army", 2, 7, 0, 0, emptyList())
        val snapshot = DocumentSnapshot("Test", 100, 100, emptyList(), emptyList(), listOf(entity), 0, false, emptyList())
        assertTrue(physicalSelectionExists(selected, snapshot))
        assertFalse(physicalSelectionExists(RecordId(RecordKind.ARMY, 0), snapshot))
        assertEquals(147190 + 37 * 725, recordByteRange(selected).first)
        assertFalse(physicalSelectionExists(selected, snapshot.copy(entities = emptyList())))
        assertTrue(physicalSelectionExists(selected, snapshot.copy(revision = 2)))
    }
}
