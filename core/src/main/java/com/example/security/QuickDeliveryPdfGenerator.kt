package com.example.security

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CustomerOrder
import com.example.data.model.DeliveryProof
import com.example.data.model.OrderStatus
import com.example.data.model.PickupProof
import com.example.data.model.RiderProfile
import com.example.ui.components.AdminExcelXlsxExporter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zero-permission Android PdfDocument generator for:
 * 1. Customer Delivery Receipt & GST Tax Invoice PDF (with Official Brand Logo)
 * 2. Rider Partner Monthly Earning Statement PDF (with Official Brand Logo)
 * Files are written inside `cacheDir/pdfs`, copied to `Downloads/QuickDelivery/Invoices`, and shared safely via FileProvider.
 */
object QuickDeliveryPdfGenerator {

    private fun parseAmount(raw: String): Double =
        raw.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0

    private fun ensurePdfFile(context: Context, fileName: String): File {
        val dir = File(context.cacheDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
    }

    private fun loadCircularBrandLogoBitmap(context: Context, sizePx: Int = 56): Bitmap? = runCatching {
        val resId = context.resources.getIdentifier(
            "cute_scooter_bunny_logo_1794655841710",
            "drawable",
            context.packageName
        )
        if (resId == 0) return@runCatching null
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        val src = BitmapFactory.decodeResource(context.resources, resId, opts) ?: return@runCatching null
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FCF4E1") }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF7A1A")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val radius = sizePx / 2f
        c.save()
        val clipPath = Path().apply { addCircle(radius, radius, radius - 2f, Path.Direction.CW) }
        c.clipPath(clipPath)
        c.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)
        val cropSize = minOf(src.width, src.height)
        val srcLeft = (src.width - cropSize) / 2
        val srcTop = (src.height - cropSize) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + cropSize, srcTop + cropSize)
        val dstRect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        c.drawBitmap(src, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        c.restore()
        c.drawCircle(radius, radius, radius - 2f, borderPaint)
        src.recycle()
        out
    }.getOrNull()

    private fun sharePdfFile(context: Context, file: File, subject: String, chooserTitle: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        lineHeight: Float,
        paint: Paint
    ): Float {
        var y = startY
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return y + lineHeight
        var line = StringBuilder()
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "${line} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = StringBuilder(candidate)
            } else {
                canvas.drawText(line.toString(), x, y, paint)
                y += lineHeight
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
            y += lineHeight
        }
        return y
    }

    fun generateAndShareCustomerReceiptPdf(
        context: Context,
        order: CustomerOrder,
        pickupProof: PickupProof? = null,
        deliveryProof: DeliveryProof? = null,
        tipAmountInr: Int = 0
    ): Result<File> = runCatching {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val navyPaint = Paint().apply {
            color = Color.parseColor("#0D1B2A")
            style = Paint.Style.FILL
        }
        val tealPaint = Paint().apply {
            color = Color.parseColor("#008080")
            style = Paint.Style.FILL
        }
        val cardPaint = Paint().apply {
            color = Color.parseColor("#F4F8F9")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#FDBA74")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.parseColor("#0D1B2A")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 11f
            isAntiAlias = true
        }
        val accentAmountPaint = Paint().apply {
            color = Color.parseColor("#008080")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        // Header banner with Official Quick Delivery Brand Logo
        canvas.drawRect(0f, 0f, 595f, 92f, navyPaint)
        val customerLogo = loadCircularBrandLogoBitmap(context, 56)
        val headerTextX = if (customerLogo != null) {
            canvas.drawBitmap(customerLogo, 28f, 18f, null)
            98f
        } else {
            36f
        }
        canvas.drawText("QUICK DELIVERY • TAX INVOICE & RECEIPT", headerTextX, 44f, titlePaint)
        canvas.drawText("FAST. SIMPLE. SECURE.  •  Order #${order.orderId}", headerTextX, 68f, subTitlePaint)

        var y = 122f
        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        canvas.drawText("Invoice Date: $generatedAt", 36f, y, bodyPaint)
        canvas.drawText("Status: ${order.orderStatus.displayTitle}", 390f, y, headingPaint)
        y += 18f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 22f

        // Customer & Partner Details
        canvas.drawText("Customer Details", 36f, y, headingPaint)
        y += 16f
        canvas.drawText("Name: ${order.customerName}   |   Mobile: ${order.customerPhone}", 36f, y, bodyPaint)
        y += 16f
        if (!order.assignedRiderName.isNullOrBlank()) {
            canvas.drawText(
                "Delivery Partner: ${order.assignedRiderName} (${order.assignedRiderVehicle ?: order.vehicleType.title})",
                36f,
                y,
                bodyPaint
            )
            y += 16f
        }

        y += 8f
        canvas.drawRect(36f, y, 559f, y + 115f, cardPaint)
        var boxY = y + 20f
        canvas.drawText("Pickup Address", 48f, boxY, headingPaint)
        boxY += 15f
        boxY = drawWrappedText(canvas, order.pickupAddress.fullAddress, 48f, boxY, 495f, 14f, bodyPaint)
        boxY += 6f
        canvas.drawText("Drop Address", 48f, boxY, headingPaint)
        boxY += 15f
        drawWrappedText(canvas, order.dropAddress.fullAddress, 48f, boxY, 495f, 14f, bodyPaint)
        y += 135f

        // Parcel & Fare Summary
        canvas.drawText("Parcel & Fare Breakdown", 36f, y, headingPaint)
        y += 18f
        canvas.drawText(
            "Vehicle: ${order.vehicleType.title}  •  Package: ${order.packageDetails.packageType.title} (${order.packageDetails.weightKg} kg)",
            36f,
            y,
            bodyPaint
        )
        y += 16f
        canvas.drawText(
            String.format(Locale.US, "Route Distance: %.1f km  •  Payment: %s", order.distanceKm, order.paymentMethod.name.replace('_', ' ')),
            36f,
            y,
            bodyPaint
        )
        y += 16f
        if (order.packageDetails.description.isNotBlank()) {
            y = drawWrappedText(canvas, "Notes: ${order.packageDetails.description}", 36f, y, 520f, 14f, bodyPaint)
        }
        y += 10f

        val baseFare = parseAmount(order.totalAmount)
        val grandTotal = baseFare + tipAmountInr
        canvas.drawRect(36f, y, 559f, y + 58f, tealPaint)
        canvas.drawText("Total Amount (incl. GST & Fees)", 50f, y + 24f, subTitlePaint.apply { color = Color.WHITE })
        if (tipAmountInr > 0) {
            canvas.drawText("Includes ₹$tipAmountInr Customer Tip", 50f, y + 43f, subTitlePaint)
        } else {
            canvas.drawText("100% Transparent Fare Seal Verified", 50f, y + 43f, subTitlePaint)
        }
        canvas.drawText(
            String.format(Locale.US, "₹%.2f", grandTotal),
            445f,
            y + 37f,
            titlePaint
        )
        y += 80f

        // Proof of Pickup / Delivery Verification
        if (pickupProof != null || deliveryProof != null) {
            canvas.drawText("Digital Handover & Proof Record", 36f, y, headingPaint)
            y += 16f
            pickupProof?.let {
                y = drawWrappedText(
                    canvas,
                    "Pickup Signed By: ${it.senderName} at ${it.capturedAt.ifBlank { "Verified" }} (${it.packagePhotoSummary})",
                    36f,
                    y,
                    520f,
                    14f,
                    bodyPaint
                )
            }
            deliveryProof?.let {
                y = drawWrappedText(
                    canvas,
                    "Delivered & Signed By: ${it.recipientName} at ${it.capturedAt.ifBlank { "Verified" }} (${it.deliveryPhotoSummary})",
                    36f,
                    y,
                    520f,
                    14f,
                    bodyPaint
                )
            }
        }

        canvas.drawLine(36f, 790f, 559f, 790f, linePaint)
        canvas.drawText("Thank you for choosing quickdelivery.co.in • Support: support@quickdelivery.co.in", 36f, 810f, bodyPaint)

        doc.finishPage(page)
        val cleanId = order.orderId.replace(Regex("[^A-Za-z0-9_-]"), "")
        val outFile = ensurePdfFile(context, "QuickDelivery_Receipt_${cleanId}.pdf")
        FileOutputStream(outFile).use { fos -> doc.writeTo(fos) }
        doc.close()
        runCatching {
            AdminExcelXlsxExporter.saveBytesToPublicPhoneDownloads(
                context = context,
                subFolder = "Invoices",
                fileName = outFile.name,
                mimeType = "application/pdf",
                bytes = outFile.readBytes()
            )
        }

        sharePdfFile(
            context = context,
            file = outFile,
            subject = "Quick Delivery Receipt #${order.orderId}",
            chooserTitle = "Save or Share Delivery Receipt PDF"
        )
        outFile
    }

    fun generateAndShareRiderStatementPdf(
        context: Context,
        rider: RiderProfile,
        orders: List<CustomerOrder>,
        tipsByOrder: Map<String, Int>,
        riderSharePercent: Int
    ): Result<File> = runCatching {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val navyPaint = Paint().apply {
            color = Color.parseColor("#0D1B2A")
            style = Paint.Style.FILL
        }
        val boxPaint = Paint().apply {
            color = Color.parseColor("#ECFDF5")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val orangeSubPaint = Paint().apply {
            color = Color.parseColor("#FF7A1A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.parseColor("#0D1B2A")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 10.5f
            isAntiAlias = true
        }
        val emeraldPaint = Paint().apply {
            color = Color.parseColor("#047857")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
        val generatedAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())

        canvas.drawRect(0f, 0f, 595f, 90f, navyPaint)
        val riderLogo = loadCircularBrandLogoBitmap(context, 54)
        val riderHeaderX = if (riderLogo != null) {
            canvas.drawBitmap(riderLogo, 28f, 18f, null)
            96f
        } else {
            36f
        }
        canvas.drawText("PARTNER QUICK DELIVERY • EARNING STATEMENT", riderHeaderX, 42f, titlePaint)
        canvas.drawText("$monthLabel Statement  •  Generated $generatedAt", riderHeaderX, 65f, orangeSubPaint)

        var y = 116f
        canvas.drawText("Partner Name: ${rider.name} (${rider.riderId})", 36f, y, headingPaint)
        y += 16f
        canvas.drawText(
            "Mobile: ${rider.mobile}  •  Vehicle: ${rider.vehicleType.title} (${rider.vehicleNumber})",
            36f,
            y,
            bodyPaint
        )
        y += 20f

        val deliveredOrders = orders.filter { it.orderStatus == OrderStatus.DELIVERED }
        val totalDistance = deliveredOrders.sumOf { it.distanceKm.coerceAtLeast(0.0) }
        val basePayout = deliveredOrders.sumOf { parseAmount(it.riderEarning) }
        val totalTips = deliveredOrders.sumOf { tipsByOrder[it.orderId] ?: 0 }
        val netEarnings = basePayout + totalTips

        canvas.drawRect(36f, y, 559f, y + 72f, boxPaint)
        canvas.drawText("Completed Trips: ${deliveredOrders.size}", 50f, y + 22f, headingPaint)
        canvas.drawText(String.format(Locale.US, "Total Distance: %.1f km", totalDistance), 230f, y + 22f, headingPaint)
        canvas.drawText("Partner Share: $riderSharePercent%", 420f, y + 22f, headingPaint)
        canvas.drawText(
            String.format(Locale.US, "Trip Payout: ₹%.2f   |   100%% Tips: ₹%d", basePayout, totalTips),
            50f,
            y + 48f,
            bodyPaint
        )
        canvas.drawText(String.format(Locale.US, "Net: ₹%.2f", netEarnings), 415f, y + 52f, emeraldPaint)
        y += 94f

        canvas.drawText("Trip-by-Trip Earnings Ledger (Latest ${minOf(deliveredOrders.size, 20)})", 36f, y, headingPaint)
        y += 16f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 16f
        canvas.drawText("Order ID", 36f, y, headingPaint)
        canvas.drawText("Vehicle / Mode", 130f, y, headingPaint)
        canvas.drawText("Distance", 275f, y, headingPaint)
        canvas.drawText("Date", 345f, y, headingPaint)
        canvas.drawText("Partner + Tip", 460f, y, headingPaint)
        y += 8f
        canvas.drawLine(36f, y, 559f, y, linePaint)
        y += 16f

        if (deliveredOrders.isEmpty()) {
            canvas.drawText("No completed trips recorded in this period yet.", 36f, y, bodyPaint)
        } else {
            deliveredOrders.take(20).forEach { ord ->
                if (y > 760f) return@forEach
                val tip = tipsByOrder[ord.orderId] ?: 0
                val earning = parseAmount(ord.riderEarning)
                canvas.drawText("#${ord.orderId}", 36f, y, bodyPaint)
                canvas.drawText("${ord.vehicleType.title} (${ord.paymentMethod.shortBadge})", 130f, y, bodyPaint)
                canvas.drawText(String.format(Locale.US, "%.1f km", ord.distanceKm), 275f, y, bodyPaint)
                canvas.drawText(ord.createdAt.take(16).ifBlank { "Today" }, 345f, y, bodyPaint)
                canvas.drawText(
                    if (tip > 0) String.format(Locale.US, "₹%.0f + ₹%d", earning, tip)
                    else String.format(Locale.US, "₹%.2f", earning),
                    460f,
                    y,
                    bodyPaint
                )
                y += 18f
            }
        }

        canvas.drawLine(36f, 790f, 559f, 790f, linePaint)
        canvas.drawText("Partner Quick Delivery • Official Digital Earnings Statement", 36f, 810f, bodyPaint)

        doc.finishPage(page)
        val outFile = ensurePdfFile(context, "Partner_Earnings_Statement_${rider.riderId}.pdf")
        FileOutputStream(outFile).use { fos -> doc.writeTo(fos) }
        doc.close()
        runCatching {
            AdminExcelXlsxExporter.saveBytesToPublicPhoneDownloads(
                context = context,
                subFolder = "Invoices",
                fileName = outFile.name,
                mimeType = "application/pdf",
                bytes = outFile.readBytes()
            )
        }

        sharePdfFile(
            context = context,
            file = outFile,
            subject = "Partner Quick Delivery Earning Statement ($monthLabel)",
            chooserTitle = "Save or Share Monthly Earning Statement PDF"
        )
        outFile
    }
}
