package com.example.cardscanner.util

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.text.Text
import java.util.regex.Pattern

import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

const val ERROR_TAG = "cardScanner"

const val INFO_TAG = "cardScanner_info"

private val creditCardPattern =
    Pattern.compile("^(?:4[0-9]{12}(?:[0-9]{3})?|[25][1-7][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|(?:2131|1800|35\\d{3})\\d{11})")

private val expiryDatePattern = Pattern.compile("[0-9]{2}/[0-9]{2}")

fun Text?.setValuesFromVisionText(
    cardDetails: (cardNumber: String, expiryDate: String, cardType: String, cardIcon : Int) -> Unit,
) {

    this?.let {

        val datesList = ArrayList<String>()

        var cardNumber = ""

        textBlocks.flatMap { it.lines }.forEach { lines ->

            with(lines.text.validateString()) {

                if (contains("/") && length == 5) datesList.add(this)

                if (contains("/") && length > 5) {

                    split(" ").filter { it.length == 5 && it.contains("/") }.forEach {

                        datesList.add(it)
                    }
                }

                if (isCardNumberValid()) cardNumber = this.replace(" ", "")
            }
        }

        if (cardNumber.isValidString() && datesList.getExpiryDate().isValidString()) cardDetails(
            cardNumber,
            datesList.getExpiryDate(),
            cardNumber.getCardType().cardType,
            cardNumber.getCardType().cardIcon)
    }
}

internal fun Bitmap.scaleImage(maxDimension: Int = 640): Bitmap {

    try {

        val originalWidth = width

        val originalHeight = height

        val resizedWidth: Int

        val resizedHeight: Int

        if (originalHeight > originalWidth) {

            resizedHeight = maxDimension

            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()

        } else if (originalWidth > originalHeight) {

            resizedWidth = maxDimension

            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()

        } else {

            resizedHeight = maxDimension

            resizedWidth = maxDimension
        }

        return Bitmap.createScaledBitmap(this, resizedWidth, resizedHeight, false)

    } catch (e: Exception) {

        if (e.localizedMessage.isValidString()) Log.e(INFO_TAG, e.localizedMessage.validateString())

        Log.e(INFO_TAG, "Scale image is unsuccessful, try to resize it.")

        return this
    }
}

private fun String.isCardNumberValid(): Boolean =
    creditCardPattern.matcher(replace(" ", "")).matches()


private fun ArrayList<String>.getExpiryDate(): String {

    val maxDate = maxOfOrNull { it.substring(it.length - 2, it.length).parseInt() }

    if (maxDate == 0) return ""

    val currentDate = find { it.substring(it.length - 2, it.length).parseInt() == maxDate }

    return currentDate.validateString()
}

// 이미지 전처리: 그레이스케일 + 대비 증가
fun Bitmap.preprocessForOcr(): Bitmap {
    // 1. 그레이스케일 변환
    val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(this, 0f, 0f, paint)

    // 2. 대비 조정 (optional, 계수 1.5~2.0 내외 실험)
    val contrasted = grayscaleBitmap.adjustContrast(1.5f)
    return contrasted
}

// 대비 조정 함수
fun Bitmap.adjustContrast(contrast: Float): Bitmap {
    val contrastedBitmap = Bitmap.createBitmap(width, height, config)
    val canvas = Canvas(contrastedBitmap)
    val paint = Paint()
    val cm = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, 0f,
            0f, contrast, 0f, 0f, 0f,
            0f, 0f, contrast, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(this, 0f, 0f, paint)
    return contrastedBitmap
}
