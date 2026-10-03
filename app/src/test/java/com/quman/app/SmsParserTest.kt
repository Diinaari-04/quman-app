package com.quman.app

import com.quman.app.util.NotificationType
import com.quman.app.util.SmsTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class SmsParserTest {

    @Test
    fun testEvcPlusReceivedExactFormat() {
        val inSms = "[-EVCPLUS-] waxaad $0.5 ka heshay 0613682904, Tar: 30/09/26 20:51:26 haraagagu waa $4.21.\nLa soo deg App-ka WAAFI http://onelink.to/waafi"
        val parsed = SmsTransactionParser.parse("192", inSms)

        assertEquals("EVC Plus", parsed.provider)
        assertEquals(NotificationType.MONEY_RECEIVED, parsed.type)
        assertEquals("in", parsed.direction)
        assertEquals(0.5, parsed.amount ?: 0.0, 0.001)
        assertNull(parsed.counterpartyName) // No name in received SMS format
        assertEquals("0613682904", parsed.counterpartyPhone)
        assertEquals(4.21, parsed.balanceAfter ?: 0.0, 0.001)
        assertFalse("Real transaction must not be marked as ad", parsed.isAd)

        // Date check: 30/09/26 20:51:26
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US)
        val expectedDate = sdf.parse("30/09/26 20:51:26")
        assertNotNull(expectedDate)
        assertEquals(expectedDate!!.time, parsed.timestamp)
    }

    @Test
    fun testEvcPlusSentExactFormat() {
        val outSms = "[-EVCPLUS-] $0.15 ayaad uwareejisay hassan muqtar mohamed(615999823), Tar: 01/10/26 07:55:18, Haraagaagu waa $4.06.\nLa soo deg App-ka WAAFI http://onelink.to/waafi"
        val parsed = SmsTransactionParser.parse("192", outSms)

        assertEquals("EVC Plus", parsed.provider)
        assertEquals(NotificationType.MONEY_SENT, parsed.type)
        assertEquals("out", parsed.direction)
        assertEquals(0.15, parsed.amount ?: 0.0, 0.001)
        assertEquals("hassan muqtar mohamed", parsed.counterpartyName)
        assertEquals("615999823", parsed.counterpartyPhone)
        assertEquals(4.06, parsed.balanceAfter ?: 0.0, 0.001)
        assertFalse("Real transaction must not be marked as ad", parsed.isAd)

        // Date check: 01/10/26 07:55:18
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US)
        val expectedDate = sdf.parse("01/10/26 07:55:18")
        assertNotNull(expectedDate)
        assertEquals(expectedDate!!.time, parsed.timestamp)
    }

    @Test
    fun testEvcPlusSentWithSpaceBeforeParen() {
        val outSms = "[-EVCPLUS-] $2.50 ayaad uwareejisay Asha Ali Nuur (612345678), Tar: 01/10/26 08:30:00, Haraagaagu waa $10.00.\nLa soo deg App-ka WAAFI http://onelink.to/waafi"
        val parsed = SmsTransactionParser.parse("192", outSms)

        assertEquals("EVC Plus", parsed.provider)
        assertEquals(NotificationType.MONEY_SENT, parsed.type)
        assertEquals("out", parsed.direction)
        assertEquals(2.50, parsed.amount ?: 0.0, 0.001)
        assertEquals("Asha Ali Nuur", parsed.counterpartyName)
        assertEquals("612345678", parsed.counterpartyPhone)
        assertEquals(10.00, parsed.balanceAfter ?: 0.0, 0.001)
        assertFalse(parsed.isAd)
    }

    @Test
    fun testJeebSender898Format() {
        val jeebSms = "[-Jeeb-] waxaad $15.0 ka heshay 252681234567, Tar: 08/07/2026 13:00:51:768, Haraagaagu waa $120.00"
        val parsed = SmsTransactionParser.parse("898", jeebSms)

        assertEquals("Jeeb", parsed.provider)
        assertEquals(NotificationType.MONEY_RECEIVED, parsed.type)
        assertEquals("in", parsed.direction)
        assertEquals(15.0, parsed.amount ?: 0.0, 0.001)
        assertEquals("252681234567", parsed.counterpartyPhone)
        assertEquals(120.0, parsed.balanceAfter ?: 0.0, 0.001)
        assertFalse(parsed.isAd)
    }

    @Test
    fun testNegativeBalanceParsing() {
        val negativeSms1 = "[-EVCPLUS-] $0.50 ayaad uwareejisay Axmed (615000000), Tar: 01/10/26 12:00:00, Haraagaagu waa $-1.15"
        val parsed1 = SmsTransactionParser.parse("192", negativeSms1)
        assertEquals(-1.15, parsed1.balanceAfter ?: 0.0, 0.001)

        val negativeSms2 = "[-EVCPLUS-] $1.00 ayaad uwareejisay Cali (615111111), Tar: 01/10/26 12:01:00, Haraagaagu waa -$2.50"
        val parsed2 = SmsTransactionParser.parse("192", negativeSms2)
        assertEquals(-2.50, parsed2.balanceAfter ?: 0.0, 0.001)
    }

    @Test
    fun testPureAdSmsWithoutTransfer() {
        val adSms = "La soo deg App-ka WAAFI si aad u hesho adeegyo casri ah iyo qiimo dhimis."
        val parsed = SmsTransactionParser.parse("192", adSms)

        assertTrue(parsed.isAd)
        assertEquals(NotificationType.OTHER, parsed.type)
        assertNull(parsed.amount)
    }
}
