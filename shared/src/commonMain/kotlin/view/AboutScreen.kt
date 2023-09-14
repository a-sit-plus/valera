package view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import globalBack

@Composable
fun AboutScreen(){
    val openDialog = remember { mutableStateOf(false)  }
    Column(
        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Compose Wallet", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Demo App", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Button(
            modifier = Modifier
                .padding(vertical = 24.dp),
            onClick = { globalBack() }
        ) {
            Text("Close")
        }
        Button(
            modifier = Modifier
                .padding(vertical = 24.dp),
            onClick = { openDialog.value = true }
        ) {
            Text("Info")
        }

        if (openDialog.value){
            Dialog(onDismissRequest = { openDialog.value = false }) {
                Box(Modifier.padding(horizontal = 5.dp).fillMaxWidth().background(color = MaterialTheme.colorScheme.secondaryContainer).height(300.dp)){
                    Column(
                        modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Info1", color = MaterialTheme.colorScheme.secondary)
                        Text("Info2", color = MaterialTheme.colorScheme.secondary)
                        Text("Info3", color = MaterialTheme.colorScheme.secondary)
                        Button(
                            modifier = Modifier
                                .padding(vertical = 24.dp),
                            onClick = { openDialog.value = false }
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}