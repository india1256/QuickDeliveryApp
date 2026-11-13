package com.example.data.cloud

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class KycDocFieldType(val label: String) {
    AADHAAR("Aadhaar Card"),
    PAN("PAN Card"),
    DRIVING_LICENCE("Driving Licence"),
    VEHICLE_RC("Vehicle RC"),
    LIVE_SELFIE("Live Selfie")
}

data class KycPhotoVerificationResult(
    val docType: KycDocFieldType,
    val isValid: Boolean,
    val clarityScorePercent: Int,
    val isBlurry: Boolean = false,
    val isTooDarkOrBright: Boolean = false,
    val isDuplicatePhoto: Boolean = false,
    val structurePassed: Boolean = false,
    val perceptualHash: Long = 0L,
    val message: String = ""
)

/**
 * Automatic on-device KYC Image Clarity & Document Structure Verifier.
 * Checks sharpness (Laplacian variance + Sobel edge energy), lighting exposure,
 * printed card text/detail structure vs blur/blank wall, and perceptual duplicate detection.
 */
object RiderKycImageQualityVerifier {

    fun computePerceptualDHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val lumLeft = luminance(scaled.getPixel(x, y))
                val lumRight = luminance(scaled.getPixel(x + 1, y))
                if (lumLeft > lumRight) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    fun analyzeBitmap(
        docType: KycDocFieldType,
        bitmap: Bitmap,
        otherDocHashes: Map<KycDocFieldType, Long> = emptyMap()
    ): KycPhotoVerificationResult {
        if (bitmap.width < 120 || bitmap.height < 120) {
            return KycPhotoVerificationResult(
                docType = docType,
                isValid = false,
                clarityScorePercent = 10,
                message = "${docType.label} photo resolution is too small (${bitmap.width}x${bitmap.height}). Capture a clear, closer photo."
            )
        }

        // Preserve native crispness for small thumbnails; cap large photos at 320px longest side
        val maxSide = max(bitmap.width, bitmap.height)
        val scale = if (maxSide > 320) 320f / maxSide else 1f
        val w = max(64, (bitmap.width * scale).toInt())
        val h = max(64, (bitmap.height * scale).toInt())
        val sample = if (scale < 0.99f) Bitmap.createScaledBitmap(bitmap, w, h, true) else bitmap

        val pixels = IntArray(w * h)
        sample.getPixels(pixels, 0, w, 0, 0, w, h)
        val lum = FloatArray(w * h)

        var sum = 0.0
        for (i in pixels.indices) {
            val l = luminance(pixels[i])
            lum[i] = l
            sum += l
        }
        val meanLum = sum / pixels.size

        var sqDiffSum = 0.0
        for (l in lum) {
            val d = l - meanLum
            sqDiffSum += d * d
        }
        val stdDevLum = sqrt(sqDiffSum / pixels.size)

        if (meanLum < 32.0) {
            return KycPhotoVerificationResult(
                docType = docType,
                isValid = false,
                clarityScorePercent = 18,
                isTooDarkOrBright = true,
                message = "${docType.label} photo is too dark. Turn on light / flash and capture clearly."
            )
        }
        if (meanLum > 243.0) {
            return KycPhotoVerificationResult(
                docType = docType,
                isValid = false,
                clarityScorePercent = 18,
                isTooDarkOrBright = true,
                message = "${docType.label} photo is overexposed or blank white. Please capture clearly."
            )
        }
        if (stdDevLum < 13.5) {
            return KycPhotoVerificationResult(
                docType = docType,
                isValid = false,
                clarityScorePercent = 15,
                isBlurry = true,
                message = "${docType.label} photo appears blank or flat. Upload a clear photo of the real document."
            )
        }

        // Laplacian variance & Sobel-style edge ratio on interior pixels
        var lapSum = 0.0
        var lapSqSum = 0.0
        var edgePixels = 0
        var count = 0

        for (y in 1 until (h - 1)) {
            val rowOff = y * w
            for (x in 1 until (w - 1)) {
                val c = lum[rowOff + x]
                val l = lum[rowOff + x - 1]
                val r = lum[rowOff + x + 1]
                val u = lum[rowOff - w + x]
                val d = lum[rowOff + w + x]

                val lap = -4.0 * c + l + r + u + d
                lapSum += lap
                lapSqSum += lap * lap

                val grad = abs(r - l) + abs(d - u)
                if (grad > 30f) {
                    edgePixels++
                }
                count++
            }
        }

        val safeCount = max(1, count)
        val lapMean = lapSum / safeCount
        val lapVariance = (lapSqSum / safeCount) - (lapMean * lapMean)
        val edgeRatio = edgePixels.toDouble() / safeCount

        // Check block-level micro-contrast across 8x8 tiles (printed text / card details vs blurry smear)
        val tileSize = 8
        val cols = w / tileSize
        val rows = h / tileSize
        var detailTiles = 0
        val totalTiles = max(1, cols * rows)
        for (ty in 0 until rows) {
            for (tx in 0 until cols) {
                var tileMin = 255f
                var tileMax = 0f
                for (dy in 0 until tileSize) {
                    val idxBase = (ty * tileSize + dy) * w + tx * tileSize
                    for (dx in 0 until tileSize) {
                        val v = lum[idxBase + dx]
                        if (v < tileMin) tileMin = v
                        if (v > tileMax) tileMax = v
                    }
                }
                if ((tileMax - tileMin) >= 24f) {
                    detailTiles++
                }
            }
        }
        val detailTileRatio = detailTiles.toDouble() / totalTiles

        val dHash = computePerceptualDHash(sample)
        val isDuplicate = otherDocHashes.any { (otherType, otherHash) ->
            otherType != docType && otherHash != 0L && hammingDistance(dHash, otherHash) <= 5
        }
        if (isDuplicate) {
            return KycPhotoVerificationResult(
                docType = docType,
                isValid = false,
                clarityScorePercent = 40,
                isDuplicatePhoto = true,
                perceptualHash = dHash,
                message = "Duplicate photo detected! Please upload the actual ${docType.label} photo instead of reusing another document image."
            )
        }

        val isBlurry = lapVariance < 48.0 || (lapVariance < 75.0 && edgeRatio < 0.032)
        val structurePassed = if (docType == KycDocFieldType.LIVE_SELFIE) {
            detailTileRatio >= 0.10 && stdDevLum >= 16.0
        } else {
            detailTileRatio >= 0.14 && edgeRatio >= 0.028
        }

        // Convert sharpness & detail into a 0..99 Clarity Score
        val lapScore = ((lapVariance / 320.0) * 65.0).coerceIn(0.0, 65.0)
        val edgeScore = ((edgeRatio / 0.16) * 20.0).coerceIn(0.0, 20.0)
        val detailScore = ((detailTileRatio / 0.55) * 14.0).coerceIn(0.0, 14.0)
        val rawScore = (lapScore + edgeScore + detailScore).toInt().coerceIn(12, 99)

        if (isBlurry) {
            return KycPhotoVerificationResult(
                docType = docType,
                isValid = false,
                clarityScorePercent = min(rawScore, 52),
                isBlurry = true,
                structurePassed = structurePassed,
                perceptualHash = dHash,
                message = "${docType.label} photo is blurry ($rawScore% clarity). Please capture or upload a sharp, clear photo."
            )
        }

        if (!structurePassed) {
            return KycPhotoVerificationResult(
                docType = docType,
                isValid = false,
                clarityScorePercent = min(rawScore, 58),
                isBlurry = false,
                structurePassed = false,
                perceptualHash = dHash,
                message = if (docType == KycDocFieldType.LIVE_SELFIE) {
                    "Selfie is not clear enough. Ensure your face is well-lit and clearly visible."
                } else {
                    "${docType.label} text/details are not readable. Hold the full card clearly in frame."
                }
            )
        }

        return KycPhotoVerificationResult(
            docType = docType,
            isValid = true,
            clarityScorePercent = max(rawScore, 72),
            isBlurry = false,
            isTooDarkOrBright = false,
            isDuplicatePhoto = false,
            structurePassed = true,
            perceptualHash = dHash,
            message = "Verified Clear (${max(rawScore, 72)}%)"
        )
    }

    fun validateAadhaarNumber(input: String): Boolean {
        val digits = input.filter(Char::isDigit)
        if (digits.length != 12) return false
        return digits.toSet().size > 1
    }

    fun validatePanNumber(input: String): Boolean {
        val clean = input.trim().uppercase()
        return Regex("^[A-Z]{5}[0-9]{4}[A-Z]$").matches(clean)
    }

    fun validateDlNumber(input: String): Boolean {
        val clean = input.trim().uppercase().filter(Char::isLetterOrDigit)
        return clean.length >= 8 && clean.any(Char::isLetter) && clean.any(Char::isDigit)
    }

    fun validateVehicleRcNumber(input: String): Boolean {
        val clean = input.trim().uppercase().filter(Char::isLetterOrDigit)
        return clean.length >= 6 && clean.any(Char::isLetter) && clean.any(Char::isDigit)
    }

    private fun luminance(pixel: Int): Float {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
