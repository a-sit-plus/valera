package preview.ui.composables

import ui.composables.PersonAttributeDetailCard
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ui.composables.PersonalDataCategory


private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PersonAttributeDetailCard(
            personalDataCategory = PersonalDataCategory.ResidencyData,
            actionButton = {
                IconButton(
                    onClick = {
                        val a = 0;
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Actions",
                    )
                }
            },
            onDetailClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Teststraße 12/6")
                Text("8010 Graz")
            }
        }
    }
}