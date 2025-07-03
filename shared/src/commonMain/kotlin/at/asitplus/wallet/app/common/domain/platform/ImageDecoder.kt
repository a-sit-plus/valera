package at.asitplus.wallet.app.common.domain.platform

import androidx.compose.ui.graphics.ImageBitmap

fun interface ImageDecoder {
    operator fun invoke(byteArray: ByteArray): Result<ImageBitmap>
}