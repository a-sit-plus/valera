package view

import Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

@Composable
fun PayloadScreen(text: String, onContinueClick: () -> Unit){
    Column {
        Text("QR Payload:", fontSize = 25.sp)
        Text(text, fontSize = 18.sp)

        Button(onClick = {
            onContinueClick()
        }) {
            Text(Resources.BUTTON_CONTINUE)
        }
    }

}