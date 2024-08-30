package data.bletransfer.holder

import android.content.Context
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.connectionmethod.ConnectionMethodBle
import com.android.identity.mdoc.connectionmethod.ConnectionMethodNfc
import com.android.identity.mdoc.connectionmethod.ConnectionMethodWifiAware
import com.android.identity.android.mdoc.transport.DataTransportOptions
import io.github.aakira.napier.Napier
import java.util.ArrayList
import java.util.OptionalLong
import java.util.UUID

class ConnectionSetup(
    private val context: Context
) {

    fun getConnectionOptions(): DataTransportOptions {
        val builder = DataTransportOptions.Builder()
            .setBleUseL2CAP(PreferencesHelper.isBleL2capEnabled())
            .setBleClearCache(PreferencesHelper.isBleClearCacheEnabled())
        return builder.build()
    }

    fun getConnectionMethods(): List<ConnectionMethod> {
        val connectionMethods = ArrayList<ConnectionMethod>()
        if (PreferencesHelper.isBleDataRetrievalEnabled()) {
            connectionMethods.add(
                ConnectionMethodBle(
                    false,
                    true,
                    null,
                    UUID.randomUUID()
                )
            )
        }
        if (PreferencesHelper.isBleDataRetrievalPeripheralModeEnabled()) {
            connectionMethods.add(
                ConnectionMethodBle(
                    true,
                    false,
                    UUID.randomUUID(),
                    null
                )
            )
        }
        return connectionMethods
    }
}