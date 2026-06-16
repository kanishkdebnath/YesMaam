package com.example.yesmaam.export.ooxml

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed interface XlsxCell {
    data class Text(val value: String, val styleId: Int = 0) : XlsxCell
    data class Number(val value: Double, val styleId: Int = 0) : XlsxCell
}

class XlsxSheet(val name: String, val rows: List<List<XlsxCell>>)

/** Excel column letters: 0 -> A, 25 -> Z, 26 -> AA. */
fun colName(index: Int): String {
    var i = index
    val sb = StringBuilder()
    while (i >= 0) {
        sb.insert(0, ('A' + (i % 26)))
        i = i / 26 - 1
    }
    return sb.toString()
}

private fun String.xmlEscape(): String = buildString {
    for (c in this@xmlEscape) when (c) {
        '&' -> append("&amp;"); '<' -> append("&lt;"); '>' -> append("&gt;")
        '"' -> append("&quot;"); '\'' -> append("&apos;"); else -> append(c)
    }
}

object XlsxWriter {
    fun write(sheet: XlsxSheet, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            zip.put("[Content_Types].xml", CONTENT_TYPES)
            zip.put("_rels/.rels", ROOT_RELS)
            zip.put("xl/workbook.xml", workbook(sheet.name))
            zip.put("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.put("xl/styles.xml", STYLES)
            zip.put("xl/worksheets/sheet1.xml", sheetXml(sheet))
        }
    }

    private fun ZipOutputStream.put(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun sheetXml(sheet: XlsxSheet): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        sheet.rows.forEachIndexed { r, row ->
            append("""<row r="${r + 1}">""")
            row.forEachIndexed { c, cell ->
                val ref = "${colName(c)}${r + 1}"
                when (cell) {
                    is XlsxCell.Text ->
                        append("""<c r="$ref" t="inlineStr" s="${cell.styleId}"><is><t>${cell.value.xmlEscape()}</t></is></c>""")
                    is XlsxCell.Number -> {
                        val v = if (cell.value == cell.value.toLong().toDouble()) cell.value.toLong().toString() else cell.value.toString()
                        append("""<c r="$ref" s="${cell.styleId}"><v>$v</v></c>""")
                    }
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun workbook(sheetName: String) =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
        """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
        """<sheets><sheet name="${sheetName.xmlEscape()}" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
        """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
        """<Default Extension="xml" ContentType="application/xml"/>""" +
        """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
        """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
        """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
        """</Types>"""

    private const val ROOT_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
        """</Relationships>"""

    private const val WORKBOOK_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
        """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
        """</Relationships>"""

    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
        """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
        """<fills count="6">""" +
        """<fill><patternFill patternType="none"/></fill>""" +
        """<fill><patternFill patternType="gray125"/></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFEAF1E7"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFFBEDED"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFFBF0E0"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFFAF6EF"/></patternFill></fill>""" +
        """</fills>""" +
        """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
        """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
        """<cellXfs count="6">""" +
        """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="5" borderId="0" xfId="0" applyFont="1" applyFill="1"/>""" +
        """<xf numFmtId="0" fontId="0" fillId="2" borderId="0" xfId="0" applyFill="1"/>""" +
        """<xf numFmtId="0" fontId="0" fillId="3" borderId="0" xfId="0" applyFill="1"/>""" +
        """<xf numFmtId="0" fontId="0" fillId="4" borderId="0" xfId="0" applyFill="1"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
        """</cellXfs></styleSheet>"""
}
