package app.tacit.ui

import app.tacit.core.ItemKind
import app.tacit.core.QuicklinkType
import app.tacit.core.SearchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuicklinkMakerTest {

    private fun row(key: String, payload: String, label: String = "row") =
        SearchItem(key, ItemKind.ACTION, label, "", payload = payload)

    @Test
    fun aTypedWebAddressBecomesAUrlQuicklinkNamedAfterItsHost() {
        val draft = QuicklinkMaker.draftOf(row("url", "url|https://www.example.com/path?q=1"))
        assertNotNull(draft)
        assertEquals(QuicklinkType.URL, draft!!.type)
        assertEquals("https://www.example.com/path?q=1", draft.target)
        assertEquals("example.com", draft.name)
        assertEquals("", draft.detail)
    }

    @Test
    fun aTypedAddressWithoutASchemeGetsHttps() {
        val draft = QuicklinkMaker.draftOf(row("url", "url|example.org/docs"))
        assertNotNull(draft)
        assertEquals("https://example.org/docs", draft!!.target)
        assertEquals("example.org", draft.name)
    }

    @Test
    fun aBlockedSchemeNeverBecomesAQuicklink() {
        assertNull(QuicklinkMaker.draftOf(row("url", "url|javascript:alert(1)")))
        assertNull(QuicklinkMaker.draftOf(row("url", "url|file:///sdcard/a.txt")))
        assertNull(QuicklinkMaker.draftOf(row("url", "url|https://")))
    }

    @Test
    fun aTypedEmailBecomesAMailtoQuicklinkNamedAfterTheAddress() {
        val draft = QuicklinkMaker.draftOf(row("mail", "mail|someone@example.com"))
        assertNotNull(draft)
        assertEquals(QuicklinkType.URL, draft!!.type)
        assertEquals("mailto:someone@example.com", draft.target)
        assertEquals("someone@example.com", draft.name)
    }

    @Test
    fun aTypedNumberBecomesAContactActionForCallMessageAndWhatsapp() {
        val call = QuicklinkMaker.draftOf(row("num:call", "dial|+201001234567", "Call +201001234567"))
        val sms = QuicklinkMaker.draftOf(row("num:sms", "sms|+201001234567", "Message +201001234567"))
        val wa = QuicklinkMaker.draftOf(row("num:wa", "wa|+201001234567", "WhatsApp +201001234567"))
        assertEquals("call|+201001234567", call!!.target)
        assertEquals("sms|+201001234567", sms!!.target)
        assertEquals("wa|+201001234567", wa!!.target)
        assertEquals(QuicklinkType.CONTACT, call.type)
        assertEquals("Call +201001234567", call.name)
        assertEquals("+201001234567", call.detail)
    }

    @Test
    fun aNumberWithNoDigitsIsRefused() {
        assertNull(QuicklinkMaker.draftOf(row("num:call", "dial|+++")))
    }

    @Test
    fun otherTypedActionsStayWithoutAQuicklinkEntry() {
        assertNull(QuicklinkMaker.draftOf(row("act:timer", "timer|600")))
        assertNull(QuicklinkMaker.draftOf(row("act:uuid", "copy|abc")))
        assertNull(QuicklinkMaker.draftOf(row("crash", "crash|share")))
    }

    @Test
    fun theMenuOffersCreateQuicklinkOnlyWhereADraftExists() {
        assertTrue(QuicklinkMaker.canCreate(row("url", "url|https://example.com")))
        assertTrue(QuicklinkMaker.canCreate(row("mail", "mail|a@example.com")))
        assertTrue(QuicklinkMaker.canCreate(row("num:wa", "wa|0100123456")))
        assertFalse(QuicklinkMaker.canCreate(row("act:coin", "copy|Heads")))
    }
}
