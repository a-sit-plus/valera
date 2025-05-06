package ui.composables

import ExpandButtonUpDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.rqes.CredentialInfo
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.text_label_certificate
import at.asitplus.valera.resources.text_label_credential_id
import at.asitplus.valera.resources.text_label_delete_certificate
import at.asitplus.valera.resources.text_label_valid_from
import at.asitplus.valera.resources.text_label_valid_to
import org.jetbrains.compose.resources.stringResource

@Composable

fun CertificateCard(credentialInfo: CredentialInfo, isExpanded: Boolean, onChangeIsExpanded: (Boolean) -> Unit, action: () -> Unit){
    ElevatedCard(){
        Column(modifier = Modifier.padding(10.dp)) {
            CertificateCardHeading(isExpanded = isExpanded, onChangeIsExpanded = onChangeIsExpanded)
            CertificateCardContent(credentialInfo = credentialInfo, isExpanded = isExpanded, action = action)
        }
    }
}

@Composable
fun CertificateCardHeading(
    isExpanded: Boolean,
    onChangeIsExpanded: (Boolean) -> Unit){
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeadingTextIcon(
                text = "${stringResource(Res.string.text_label_certificate)[0]}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier,
            )
            Spacer(modifier = Modifier.width(15.dp))
            Text(stringResource(Res.string.text_label_certificate))
        }

        ExpandButtonUpDown(
            isExpanded = isExpanded,
            onClick = {
                onChangeIsExpanded(!isExpanded)
            },
            contentDescription = null,
        )
    }
}

@Composable
fun CertificateCardContent(
    credentialInfo: CredentialInfo,
    isExpanded: Boolean,
    action: () -> Unit) {
    LabeledText(
        label = stringResource(Res.string.text_label_credential_id),
        text = "${credentialInfo.credentialID}",
        modifier = Modifier.padding(bottom = 16.dp),
    )
    if (isExpanded) {
        LabeledText(
            label = stringResource(Res.string.text_label_valid_from),
            text = "${credentialInfo.certParameters?.validFrom}",
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LabeledText(
            label = stringResource(Res.string.text_label_valid_to),
            text = "${credentialInfo.certParameters?.validTo}",
            modifier = Modifier.padding(bottom = 16.dp),
        )
        OutlinedButton(onClick = action) {
            Text(stringResource(Res.string.text_label_delete_certificate))
        }
    }

}