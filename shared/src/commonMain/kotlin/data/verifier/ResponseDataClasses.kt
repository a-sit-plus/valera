package data.verifier

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.section_heading_selected_namespace
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


interface EntryValueItem {
    @Composable
    fun showData()
}

class VehicleRegistration(
    private val issueDate: String,
    private val expiryDate: String?,
    private val vehicleCategory: String
): EntryValueItem {
    @Composable
    override fun showData() {
        Text("Vehicle category:")
        Text(vehicleCategory ,fontWeight = FontWeight.Bold)
        Text("Date of issue:")
        Text(issueDate, fontWeight = FontWeight.Bold)
        if (expiryDate != null) {
            Text("Date of expiry:")
            Text(expiryDate, fontWeight = FontWeight.Bold)
        }
    }
}

interface EntryValue{
    @Composable
    fun showData()
}
class StringEntry(private val value: String): EntryValue {
    @Composable
    override fun showData() {
        Text(value, fontWeight = FontWeight.Bold)
    }
}

class BooleanEntry(private val value: Boolean?): EntryValue {
    @Composable
    override fun showData() {
        Text(if (value== null) "null" else if(value) "yes" else  "no", fontWeight = FontWeight.Bold)
    }
}

class IntEntry(private val value: Number): EntryValue {
    @Composable
    override fun showData() {
        Text("$value", fontWeight = FontWeight.Bold)
    }
}

class ImageEntry(private val imageBitmap: ImageBitmap): EntryValue {
    @Composable
    override fun showData() {
        Image(bitmap = imageBitmap, contentDescription = null)
    }
}

class ImageArray(private val items: List<EntryValueItem>): EntryValue {
    @Composable
    override fun showData() {
        Column {
            for (item in items) {
                Box(modifier = Modifier.padding(8.dp)) {
                    Column {
                        item.showData()
                    }
                }
            }
        }

    }
}

class Entry(
    val entryName: String,
    private val displayName: StringResource,
    private val value: EntryValue?
) {
    @Composable
    fun show() {
        Text(
            text = "${stringResource(displayName)}:",
            style = MaterialTheme.typography.titleMedium,
        )
        Box(modifier = Modifier.padding(16.dp)) {
            value?.showData()
        }
    }
}
