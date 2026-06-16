package com.example.yesmaam.export.ooxml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class XlsxWriterTest {
    private fun entries(bytes: ByteArray): Map<String, String> {
        val out = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                out[e.name] = zis.readBytes().toString(Charsets.UTF_8)
                e = zis.nextEntry
            }
        }
        return out
    }

    @Test fun `writes the required ooxml parts`() {
        val sheet = XlsxSheet("Report", listOf(listOf(XlsxCell.Text("Name", 1), XlsxCell.Number(96.0))))
        val bos = ByteArrayOutputStream()
        XlsxWriter.write(sheet, bos)
        val parts = entries(bos.toByteArray())
        assertTrue("[Content_Types].xml" in parts)
        assertTrue("_rels/.rels" in parts)
        assertTrue("xl/workbook.xml" in parts)
        assertTrue("xl/_rels/workbook.xml.rels" in parts)
        assertTrue("xl/styles.xml" in parts)
        assertTrue("xl/worksheets/sheet1.xml" in parts)
    }

    @Test fun `sheet contains cell values and styles`() {
        val sheet = XlsxSheet("Report", listOf(listOf(XlsxCell.Text("Ann & co", 1), XlsxCell.Number(75.0, 5))))
        val bos = ByteArrayOutputStream()
        XlsxWriter.write(sheet, bos)
        val xml = entries(bos.toByteArray()).getValue("xl/worksheets/sheet1.xml")
        assertTrue(xml.contains("<c r=\"A1\" t=\"inlineStr\" s=\"1\">"))
        assertTrue(xml.contains("<t>Ann &amp; co</t>"))      // escaped
        assertTrue(xml.contains("<c r=\"B1\" s=\"5\"><v>75</v></c>"))
    }

    @Test fun `column names roll over past Z`() {
        assertEquals("A", colName(0))
        assertEquals("Z", colName(25))
        assertEquals("AA", colName(26))
        assertEquals("AB", colName(27))
    }
}
