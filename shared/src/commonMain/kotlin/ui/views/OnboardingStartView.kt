package ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.asp
import at.asitplus.valera.resources.button_label_start
import at.asitplus.valera.resources.icon
import at.asitplus.valera.resources.text_label_powered_by
import at.asitplus.valera.resources.valera_b
import at.asitplus.valera.resources.valera_w
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal object OnboardingStartScreenTestTag {
    const val startButton = "startButton"
}

@Composable
fun OnboardingStartView(
    onClickStart: () -> Unit,
    onClickLogo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onClickStart,
                        modifier = Modifier.testTag(OnboardingStartScreenTestTag.startButton)
                    ) {
                        Text(stringResource(Res.string.button_label_start))
                    }
                }
            }
        },
        modifier = modifier,
    ) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding), verticalArrangement = Arrangement.SpaceEvenly) {
            Column(modifier = Modifier.fillMaxWidth(),horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(Res.drawable.icon),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.padding(start = 100.dp, end = 100.dp)
                )
                val logo = when(isSystemInDarkTheme()){
                    true -> Res.drawable.valera_w
                    false -> Res.drawable.valera_b
                }
                Row {
                    Spacer(Modifier.width(10.dp))
                    Image(
                        painter = painterResource(logo),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClickLogo), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(Res.string.text_label_powered_by), fontWeight = FontWeight.Medium)
                Image(
                    painter = painterResource(Res.drawable.asp),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.padding(start = 135.dp, end = 135.dp)
                )
            }


        }
    }
}