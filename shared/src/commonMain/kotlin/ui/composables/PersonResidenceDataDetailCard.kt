package ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.mainAddress

data class ResidenceData(
    val mainAddress: String?,
)


val List<SubjectCredentialStore.StoreEntry>.residenceData: ResidenceData
    get() = ResidenceData(
        mainAddress = firstNotNullOfOrNull { it.mainAddress },
    )

@Composable
fun PersonResidenceDataDetailCard(
    residenceData: ResidenceData,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PersonAttributeDetailCardHeading(
                avatarText = "MA",
                title = "Meldeadresse",
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                val textGap = 4.dp
                if (residenceData.mainAddress != null) {
                    Text(
                        text = residenceData.mainAddress,
                        modifier = Modifier.padding(bottom = textGap),
                    )
                }
//                if (residenceData.postalCode != null || residenceData.townName != null) {
//                    Text(
//                        listOfNotNull(
//                            residenceData.postalCode,
//                            residenceData.townName
//                        ).joinToString(" "),
//                        modifier = Modifier.padding(bottom = textGap),
//                    )
//                }
                Spacer(modifier = Modifier.height(16.dp - textGap))
            }
        }
    }
}
