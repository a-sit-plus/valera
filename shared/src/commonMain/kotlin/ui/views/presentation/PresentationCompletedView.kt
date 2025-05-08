package ui.views.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.icon_presentation_error
import at.asitplus.valera.resources.icon_presentation_success
import at.asitplus.valera.resources.presentation_canceled
import at.asitplus.valera.resources.presentation_error
import at.asitplus.valera.resources.presentation_success
import at.asitplus.valera.resources.presentation_timeout
import at.asitplus.wallet.app.common.presentation.PresentmentCanceled
import at.asitplus.wallet.app.common.presentation.PresentmentTimeout
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationCompletedView(error: Throwable?) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Result",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            )
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (error) {
                null -> showPresentationSuccess()
                else -> showPresentationFailure(error)
            }
        }
    }
}

@Composable
fun showPresentationSuccess() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        showImageAndText(
            painterResource(Res.drawable.icon_presentation_success),
            stringResource(Res.string.presentation_success)
        )
    }
}

@Composable
fun showPresentationFailure(error: Throwable) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val text = when (error) {
            is PresentmentCanceled -> stringResource(Res.string.presentation_canceled)
            is PresentmentTimeout -> stringResource(Res.string.presentation_timeout)
            else -> stringResource(Res.string.presentation_error)
        }
        showImageAndText(painterResource(Res.drawable.icon_presentation_error), text)
    }
}

@Composable
fun showImageAndText(painter: Painter, text: String) {
    Image(
        modifier = Modifier.size(200.dp).fillMaxSize().padding(10.dp),
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Normal
    )
}
