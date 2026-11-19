package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.domain.AttendXDataSnapshot
import com.example.domain.AttendanceCalculator
import com.example.domain.IsoDate
import java.io.File
import java.io.FileOutputStream

object ReportExporter {

    fun generateCsvContent(snapshot: AttendXDataSnapshot): String {
        val sb = StringBuilder()
        sb.appendLine("Subject Report — Generated on ${IsoDate.today().toIsoString()}")
        sb.appendLine("Code,Subject,Attended,Conducted,Absent,Current %,Required %,Safe Bunks,To Recover,Status")
        snapshot.subjects.forEach { s ->
            val m = AttendanceCalculator.calculateBunkMetrics(s)
            val safeName = "\"${s.name.replace("\"", "\"\"")}\""
            sb.appendLine(
                "${s.code},$safeName,${s.attended},${s.conducted},${s.absent}," +
                    "${AttendanceCalculator.formatPercent(m.currentPercent)},${s.requiredPercent}%," +
                    "${m.safeBunks},${m.classesToRecover},${m.healthStatus.label}"
            )
        }
        sb.appendLine()
        sb.appendLine("Attendance History")
        sb.appendLine("Date,Subject Code,Subject Name,Period,Periods Count,Status,Note")
        val subMap = snapshot.subjects.associateBy { it.id }
        snapshot.attendanceRecords.forEach { r ->
            val sub = subMap[r.subjectId]
            val safeName = "\"${(sub?.name ?: r.subjectId).replace("\"", "\"\"")}\""
            val safeNote = "\"${r.note.replace("\"", "\"\"")}\""
            sb.appendLine("${r.dateIso},${sub?.code ?: ""},$safeName,${r.periodIndex},${r.periodsCount},${r.status},$safeNote")
        }
        return sb.toString()
    }

    fun generatePdfFile(context: Context, snapshot: AttendXDataSnapshot): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "AttendX_Report_${IsoDate.today().toIsoString()}.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(17, 24, 54)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(91, 33, 182)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10.5f
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        val summary = AttendanceCalculator.calculateDashboardSummary(
            snapshot.subjects,
            snapshot.profile.defaultRequiredPercent
        )

        var y = 48f
        canvas.drawText("AttendX — Attendance & Bunk Report", 36f, y, titlePaint)
        y += 20f
        canvas.drawText("Generated: ${IsoDate.today().toIsoString()}  |  Overall: ${AttendanceCalculator.formatPercent(summary.overallPercent)} (${summary.overallStatus.label})", 36f, y, subtitlePaint)
        y += 18f
        canvas.drawText(
            "Conducted: ${summary.totalConducted}   Attended: ${summary.totalAttended}   Absent: ${summary.totalAbsent}   Total Safe Bunks: ${summary.totalSafeBunksAcrossSubjects}",
            36f,
            y,
            bodyPaint
        )
        y += 14f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 22f

        canvas.drawText("Code", 36f, y, boldPaint)
        canvas.drawText("Subject", 92f, y, boldPaint)
        canvas.drawText("Att/Cond", 295f, y, boldPaint)
        canvas.drawText("Current", 365f, y, boldPaint)
        canvas.drawText("Target", 425f, y, boldPaint)
        canvas.drawText("Safe/Need", 480f, y, boldPaint)
        y += 8f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 18f

        snapshot.subjects.forEach { s ->
            if (y > 780f) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, doc.pages.size + 1).create())
                canvas = page.canvas
                y = 48f
            }
            val m = AttendanceCalculator.calculateBunkMetrics(s)
            canvas.drawText(s.code.take(8), 36f, y, bodyPaint)
            canvas.drawText(s.name.take(30), 92f, y, bodyPaint)
            canvas.drawText("${s.attended}/${s.conducted}", 295f, y, bodyPaint)
            canvas.drawText(AttendanceCalculator.formatPercentShort(m.currentPercent), 365f, y, boldPaint)
            canvas.drawText("${s.requiredPercent.toInt()}%", 425f, y, bodyPaint)
            val actionText = if (m.classesToRecover > 0) "Attend ${m.classesToRecover}" else "${m.safeBunks} bunks"
            canvas.drawText(actionText, 480f, y, bodyPaint)
            y += 18f
        }

        y += 12f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 22f
        canvas.drawText("Recent Attendance History (Latest 20)", 36f, y, boldPaint)
        y += 18f

        val subMap = snapshot.subjects.associateBy { it.id }
        snapshot.attendanceRecords.take(20).forEach { r ->
            if (y > 790f) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, doc.pages.size + 1).create())
                canvas = page.canvas
                y = 48f
            }
            val subName = subMap[r.subjectId]?.name?.take(26) ?: r.subjectId
            canvas.drawText("${r.dateIso}  |  $subName (P${r.periodIndex})  —  ${r.status}", 36f, y, bodyPaint)
            y += 15f
        }

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun writeCsvToCacheAndShare(context: Context, snapshot: AttendXDataSnapshot) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "AttendX_Report_${IsoDate.today().toIsoString()}.csv")
        file.writeText(generateCsvContent(snapshot))
        shareFile(context, file, "text/csv", "Export Attendance CSV")
    }

    fun printPdfReport(context: Context, snapshot: AttendXDataSnapshot) {
        val pdfFile = generatePdfFile(context, snapshot)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "AttendX Report ${IsoDate.today().toIsoString()}"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    pdfFile.inputStream().use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
