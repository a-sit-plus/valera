package data.bletransfer.holder

import android.content.Context
import com.android.identity.android.mdoc.transport.DataTransportOptions
import com.android.identity.mdoc.connectionmethod.ConnectionMethod
import com.android.identity.mdoc.connectionmethod.ConnectionMethodBle
import com.android.identity.util.UUID

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
                    supportsPeripheralServerMode = false,
                    supportsCentralClientMode = true,
                    peripheralServerModeUuid = null,
                    centralClientModeUuid = UUID.randomUUID()
                )
            )
        }
        if (PreferencesHelper.isBleDataRetrievalPeripheralModeEnabled()) {
            connectionMethods.add(
                ConnectionMethodBle(
                    supportsPeripheralServerMode = true,
                    supportsCentralClientMode = false,
                    peripheralServerModeUuid = UUID.randomUUID(),
                    centralClientModeUuid = null
                )
            )
        }
        return connectionMethods
    }
}
