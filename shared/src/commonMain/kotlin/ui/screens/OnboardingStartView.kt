package ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.app_display_name
import compose_wallet_app.shared.generated.resources.button_label_start
import compose_wallet_app.shared.generated.resources.onboardingBackground
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal object OnboardingStartScreenTestTag {
    const val startButton = "startButton"
}

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStartView(
    onClickStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box {
        Image(
            painter = painterResource(Res.drawable.onboardingBackground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                ),
                title = {
                    Text(
                        text = stringResource(Res.string.app_display_name),
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            )
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
    ) {}
}