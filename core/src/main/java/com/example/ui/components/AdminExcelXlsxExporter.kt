package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class XlsxSheetSpec(
    val sheetName: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

data class XlsxExportResult(
    val fileName: String,
    val absolutePath: String,
    val rowCount: Int,
    val sheetCount: Int,
    val sizeBytes: Long,
    val exportedAt: String
)

/**
 * Genuine OpenXML (.xlsx) Excel Workbook Exporter for Quick Delivery Admin Web.
 * Generates standards-compliant .xlsx ZIP archives containing XML worksheets so admins
 * can export Orders, Riders, Customers, 20%/80% Earnings, Payments, and Master Reports.
 */
object AdminExcelXlsxExporter {

    fun exportSingleSheetToXlsx(
        context: Context,
        filePrefix: String,
        sheetName: String,
        headers: List<String>,
        rows: List<List<String>>,
        launchShareSheet: Boolean = false
    ): XlsxExportResult {
        return exportWorkbookToXlsx(
            context = context,
            filePrefix = filePrefix,
            sheets = listOf(XlsxSheetSpec(sheetName = sheetName, headers = headers, rows = rows)),
            launchShareSheet = launchShareSheet
        )
    }

    fun exportWorkbookToXlsx(
        context: Context,
        filePrefix: String,
        sheets: List<XlsxSheetSpec>,
        launchShareSheet: Boolean = false
    ): XlsxExportResult {
        val safeSheets = if (sheets.isEmpty()) {
            listOf(XlsxSheetSpec("Sheet1", listOf("Status"), listOf(listOf("No Data"))))
        } else {
            sheets
        }

        val timestampSlug = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val humanTimestamp = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US).format(Date())
        val cleanPrefix = filePrefix.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "${cleanPrefix}_$timestampSlug.xlsx"

        val exportDir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "admin_exports").apply {
            if (!exists()) mkdirs()
        }
        val outFile = File(exportDir, fileName)

        FileOutputStream(outFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. [Content_Types].xml
                writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml(safeSheets.size))
                // 2. _rels/.rels
                writeZipEntry(zos, "_rels/.rels", buildRootRelsXml())
                // 3. xl/workbook.xml
                writeZipEntry(zos, "xl/workbook.xml", buildWorkbookXml(safeSheets))
                // 4. xl/_rels/workbook.xml.rels
                writeZipEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(safeSheets.size))
                // 5. xl/styles.xml
                writeZipEntry(zos, "xl/styles.xml", buildStylesXml())
                // 6. xl/worksheets/sheet{N}.xml
                safeSheets.forEachIndexed { idx, sheet ->
                    val sheetXml = buildWorksheetXml(sheet.headers, sheet.rows)
                    writeZipEntry(zos, "xl/worksheets/sheet${idx + 1}.xml", sheetXml)
                }
            }
        }

        // Also copy tab-separated summary to clipboard for immediate paste into spreadsheets
        runCatching {
            val firstSheet = safeSheets.first()
            val tsvText = buildString {
                appendLine(firstSheet.headers.joinToString("\t"))
                firstSheet.rows.forEach { row ->
                    appendLine(row.joinToString("\t") { it.replace("\t", " ").replace("\n", " ") })
                }
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(fileName, tsvText))

            if (launchShareSheet) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    putExtra(Intent.EXTRA_TEXT, tsvText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Export $fileName").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }

        val totalRows = safeSheets.sumOf { it.rows.size }
        return XlsxExportResult(
            fileName = fileName,
            absolutePath = outFile.absolutePath,
            rowCount = totalRows,
            sheetCount = safeSheets.size,
            sizeBytes = outFile.length(),
            exportedAt = humanTimestamp
        )
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(Charsets.UTF_8)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun escapeXml(input: String): String {
        return buildString(input.length) {
            for (ch in input) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> if (ch.code >= 32 || ch == '\n' || ch == '\r' || ch == '\t') append(ch)
                }
            }
        }
    }

    private fun columnLetter(colIndexZeroBased: Int): String {
        var index = colIndexZeroBased
        val sb = StringBuilder()
        while (index >= 0) {
            val remainder = index % 26
            sb.insert(0, ('A'.code + remainder).toChar())
            index = (index / 26) - 1
        }
        return sb.toString()
    }

    private fun buildContentTypesXml(sheetCount: Int): String {
        val sheetOverrides = (1..sheetCount).joinToString("\n") { idx ->
            """  <Override PartName="/xl/worksheets/sheet$idx.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$sheetOverrides
</Types>"""
    }

    private fun buildRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun buildWorkbookXml(sheets: List<XlsxSheetSpec>): String {
        val sheetNodes = sheets.mapIndexed { idx, spec ->
            val cleanName = escapeXml(
                spec.sheetName
                    .replace(Regex("[\\\\/?*\\[\\]:]"), " ")
                    .trim()
                    .take(31)
                    .ifBlank { "Sheet${idx + 1}" }
            )
            """    <sheet name="$cleanName" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>"""
        }.joinToString("\n")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
$sheetNodes
  </sheets>
</workbook>"""
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String {
        val sheetRels = (1..sheetCount).joinToString("\n") { idx ->
            """  <Relationship Id="rId$idx" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$idx.xml"/>"""
        }
        val stylesId = sheetCount + 1
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$sheetRels
  <Relationship Id="rId$stylesId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF063940"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""

    private fun buildWorksheetXml(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("\n")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("\n  <sheetData>\n")

        // Header Row (Row 1, style s="1")
        sb.append("""    <row r="1">""")
        headers.forEachIndexed { colIdx, headerText ->
            val ref = "${columnLetter(colIdx)}1"
            sb.append("""<c r="$ref" t="inlineStr" s="1"><is><t>${escapeXml(headerText)}</t></is></c>""")
        }
        sb.append("</row>\n")

        // Data Rows (Row 2..N)
        rows.forEachIndexed { rowIdx, rowCells ->
            val excelRowNum = rowIdx + 2
            sb.append("""    <row r="$excelRowNum">""")
            rowCells.forEachIndexed { colIdx, cellValue ->
                val ref = "${columnLetter(colIdx)}$excelRowNum"
                sb.append("""<c r="$ref" t="inlineStr"><is><t>${escapeXml(cellValue)}</t></is></c>""")
            }
            sb.append("</row>\n")
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }
}

/**
 * Reusable Searchable, Filterable, Paginated Enterprise Data Table with 1-Click Excel (.xlsx) Export,
 * Date-Range Summary, Loading State, and Empty State.
 */
@Composable
fun AdminEnterprisePaginatedTableCard(
    title: String,
    subtitle: String,
    exportFilePrefix: String,
    sheetName: String,
    headers: List<String>,
    allRows: List<List<String>>,
    filterChips: List<Pair<String, String>> = emptyList(),
    selectedFilterKey: String = "ALL",
    onFilterSelected: (String) -> Unit = {},
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String = "Search table records...",
    dateRangeLabel: String = "All Dates",
    cityFilterLabel: String = "All Cities",
    isLoading: Boolean = false,
    emptyStateTitle: String = "No matching records found",
    emptyStateSubtitle: String = "Try adjusting your search query, status filter, city, or date range.",
    columnWidth: Dp = 148.dp,
    testTagPrefix: String = "admin_table",
    onRowClicked: ((Int, List<String>) -> Unit)? = null
) {
    val context = LocalContext.current
    var currentPage by remember(allRows.size, selectedFilterKey, searchQuery) { mutableIntStateOf(0) }
    var pageSize by remember { mutableIntStateOf(5) }
    var lastExportResult by remember { mutableStateOf<XlsxExportResult?>(null) }

    val totalRecords = allRows.size
    val totalPages = remember(totalRecords, pageSize) {
        if (totalRecords == 0) 1 else ((totalRecords + pageSize - 1) / pageSize)
    }
    val safePage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val pageStartIndex = safePage * pageSize
    val pageEndIndexExclusive = (pageStartIndex + pageSize).coerceAtMost(totalRecords)
    val pageRows = remember(allRows, safePage, pageSize) {
        if (allRows.isEmpty() || pageStartIndex >= allRows.size) {
            emptyList()
        } else {
            allRows.subList(pageStartIndex, pageEndIndexExclusive)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header Row: Title + Active Filters Pill + Export .xlsx Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Color(0xFF063940),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF062C30)
                        )
                    }
                    Text(
                        text = "$subtitle • Date: $dateRangeLabel • City: $cityFilterLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                }

                Button(
                    onClick = {
                        lastExportResult = AdminExcelXlsxExporter.exportSingleSheetToXlsx(
                            context = context,
                            filePrefix = exportFilePrefix,
                            sheetName = sheetName,
                            headers = headers,
                            rows = allRows,
                            launchShareSheet = false
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("${testTagPrefix}_export_xlsx_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export Excel .xlsx",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export .xlsx ($totalRecords)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Export Confirmation Banner
            lastExportResult?.let { result ->
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_export_confirmation_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✅ Exported ${result.fileName} (${result.rowCount} rows • ${result.sizeBytes} bytes)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "Saved to: ${result.absolutePath} • Copied to Clipboard (${result.exportedAt})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF047857),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { lastExportResult = null },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss export notice",
                                tint = Color(0xFF065F46),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Search Input & Status Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    currentPage = 0
                },
                placeholder = { Text(searchPlaceholder, style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF00A896)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                currentPage = 0
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_search_input")
            )

            if (filterChips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterChips.forEach { (key, label) ->
                        val isSelected = selectedFilterKey == key
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onFilterSelected(key)
                                currentPage = 0
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF063940),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("${testTagPrefix}_filter_$key")
                        )
                    }
                }
            }

            // 3. Loading State, Empty State, or Scrollable Data Table
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00A896),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Syncing live records from Quick Delivery backend...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF475569)
                        )
                    }
                }
            } else if (allRows.isEmpty()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = emptyStateTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = emptyStateSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                val horizontalScroll = rememberScrollState()
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScroll)
                    ) {
                        // Table Header Row (Dark Teal)
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF063940))
                                .padding(vertical = 9.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5EEAD4),
                                modifier = Modifier.width(38.dp)
                            )
                            headers.forEach { header ->
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .width(columnWidth)
                                        .padding(horizontal = 6.dp)
                                )
                            }
                        }

                        // Paginated Data Rows
                        pageRows.forEachIndexed { localIdx, row ->
                            val globalRowNumber = pageStartIndex + localIdx + 1
                            val rowBg = if (localIdx % 2 == 0) Color.White else Color(0xFFF8FAFC)
                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .clickable(enabled = onRowClicked != null) {
                                        onRowClicked?.invoke(globalRowNumber - 1, row)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$globalRowNumber",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF008080),
                                    modifier = Modifier.width(38.dp)
                                )
                                row.forEach { cell ->
                                    val cellColor = when {
                                        cell.contains("DELIVERED", ignoreCase = true) ||
                                            cell.contains("VERIFIED", ignoreCase = true) ||
                                            cell.contains("ONLINE", ignoreCase = true) -> Color(0xFF065F46)
                                        cell.contains("CANCELLED", ignoreCase = true) ||
                                            cell.contains("BLOCKED", ignoreCase = true) ||
                                            cell.contains("OVER-SPEED", ignoreCase = true) -> Color(0xFFB91C1C)
                                        cell.startsWith("₹") -> Color(0xFF063940)
                                        else -> Color(0xFF1E293B)
                                    }
                                    Text(
                                        text = cell,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (cell.startsWith("₹") || cell.startsWith("#") || cell.startsWith("ORD-")) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        color = cellColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(columnWidth)
                                            .padding(horizontal = 6.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                        }
                    }
                }

                // 4. Pagination Footer Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Showing X-Y of Z + Page Size Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Showing ${pageStartIndex + 1}–$pageEndIndexExclusive of $totalRecords",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        listOf(5, 10, 25).forEach { sizeOption ->
                            val isSelectedSize = pageSize == sizeOption
                            Surface(
                                color = if (isSelectedSize) Color(0xFF063940) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable {
                                    pageSize = sizeOption
                                    currentPage = 0
                                }
                            ) {
                                Text(
                                    text = "${sizeOption}/pg",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelectedSize) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Right: Prev / Page X of Y / Next
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (safePage > 0) currentPage = safePage - 1 },
                            enabled = safePage > 0,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("${testTagPrefix}_prev_page_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Page",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Prev", style = MaterialTheme.typography.labelSmall)
                        }

                        Text(
                            text = "Page ${safePage + 1}/$totalPages",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF063940)
                        )

                        OutlinedButton(
                            onClick = { if (safePage < totalPages - 1) currentPage = safePage + 1 },
                            enabled = safePage < totalPages - 1,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("${testTagPrefix}_next_page_btn")
                        ) {
                            Text("Next", style = MaterialTheme.typography.labelSmall)
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Page",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
