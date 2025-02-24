package data.bletransfer.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.section_heading_date_of_expiry
import at.asitplus.valera.resources.section_heading_date_of_issue
import at.asitplus.valera.resources.attribute_friendly_age_above
import at.asitplus.valera.resources.attribute_friendly_age_below
import at.asitplus.valera.resources.error_missing_value
import at.asitplus.valera.resources.section_heading_vehicle_category
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.DrivingPrivilegeCode
import org.jetbrains.compose.resources.stringResource

interface EntryValue{
    @Composable
    fun showData()
}

class StringEntry(private val value: String): EntryValue {
    @Composable
    override fun showData() {
        BoldText(value)
    }
}

class BooleanEntry(private val value: Boolean?): EntryValue {
    @Composable
    override fun showData() {
        val textRes = when (value) {
            true -> Res.string.attribute_friendly_age_above
            false -> Res.string.attribute_friendly_age_below
            else -> Res.string.error_missing_value
        }
        BoldText(text = stringResource(textRes))
    }
}

class IntEntry(private val value: Number): EntryValue {
    @Composable
    override fun showData() {
        BoldText("$value")
    }
}

class ImageEntry(private val imageBitmap: ImageBitmap): EntryValue {
    @Composable
    override fun showData() {
        Image(bitmap = imageBitmap, contentDescription = null)
    }
}

class DrivingPrivilegesEntry(private val drivingPrivileges: Array<DrivingPrivilege>) : EntryValue {
    @Composable
    override fun showData() {
        Column {
            drivingPrivileges.forEach { drivingPrivilege ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ShowDrivingPrivilegeData(drivingPrivilege)
                    }
                }
            }
        }
    }
}

@Composable
fun ShowDrivingPrivilegeData(drivingPrivilege: DrivingPrivilege) {
    Column {
        SectionTitle(stringResource(Res.string.section_heading_vehicle_category))
        BoldText(drivingPrivilege.vehicleCategoryCode)

        drivingPrivilege.issueDate?.let {
            SectionTitle(stringResource(Res.string.section_heading_date_of_issue))
            BoldText(it.toString())
        }

        drivingPrivilege.expiryDate?.let {
            SectionTitle(stringResource(Res.string.section_heading_date_of_expiry))
            BoldText(it.toString())
        }

        drivingPrivilege.codes?.takeIf { it.isNotEmpty() }?.let { codes ->
            SectionTitle("Codes")
            codes.forEach { code ->
                CodeItem(code)
            }
        }
    }
}

@Composable
fun CodeItem(code: DrivingPrivilegeCode) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            SectionTitle("Code")
            BoldText(code.code)

            code.sign?.let {
                SectionTitle("Sign")
                BoldText(it)
            }

            code.value?.let {
                SectionTitle("Value")
                BoldText(it)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun BoldText(text: String) {
    Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
}
