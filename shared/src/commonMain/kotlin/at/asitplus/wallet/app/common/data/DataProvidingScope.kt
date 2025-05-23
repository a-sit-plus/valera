package at.asitplus.wallet.app.common.data

import kotlinx.coroutines.CoroutineScope
import kotlin.jvm.JvmInline

@JvmInline
value class DataProvidingScope(
    val coroutineScope: CoroutineScope
)