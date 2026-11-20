package app.tacit.core

import app.tacit.data.SnippetExpander
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDateTime

class HardeningTest {

    private val now = LocalDateTime.of(2026, 10, 8, 9, 5)

    @Test
    fun snippetWithABrokenDateFormatKeepsItsPlaceholder() {
        assertEquals("{date:today is dd}", SnippetExpander.expand("{date:today is dd}", now, null))
    }

    @Test
    fun snippetFormatNeedingAZoneKeepsItsPlaceholder() {
        assertEquals("{datetime:HH:mm z}", SnippetExpander.expand("{datetime:HH:mm z}", now, null))
    }

    @Test
    fun goodSnippetFormatsStillExpand() {
        assertEquals("08/10/2026 09:05", SnippetExpander.expand("{date:dd/MM/yyyy} {time}", now, null))
    }

    @Test
    fun quicklinkDateFormatNeedingAZoneFallsBackToTheIsoDate() {
        assertEquals("https://x.com/2026-10-08", QuicklinkRules.fill("https://x.com/{date:HH z}", "", "", now))
        assertFalse(QuicklinkRules.placeholdersValid("https://x.com/{date:HH z}"))
    }

    @Test
    fun folderDocumentIdsCoverPrimaryAndRemovableVolumes() {
        assertEquals("primary:Download/a", PathNames.documentId("/storage/emulated/0/Download/a"))
        assertEquals("primary:", PathNames.documentId("/storage/emulated/0"))
        assertEquals("primary:DCIM", PathNames.documentId("/sdcard/DCIM"))
        assertEquals("1A2B-3C4D:Movies/trip", PathNames.documentId("/storage/1A2B-3C4D/Movies/trip"))
        assertEquals("1A2B-3C4D:", PathNames.documentId("/storage/1A2B-3C4D"))
        assertEquals("primary:", PathNames.documentId("/data/other"))
    }
}
