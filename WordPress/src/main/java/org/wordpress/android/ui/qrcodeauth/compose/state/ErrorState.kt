package org.wordpress.android.ui.qrcodeauth.compose.state

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.PrimaryButtonM3
import org.wordpress.android.ui.compose.components.buttons.SecondaryButtonM3
import org.wordpress.android.ui.compose.components.text.SubtitleM3
import org.wordpress.android.ui.compose.components.text.TitleM3
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ErrorPrimaryActionButton
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.ActionButton.ErrorSecondaryActionButton

@Composable
fun ErrorState(uiState: QRCodeAuthUiState.Error): Unit = with(uiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = stringResource(R.string.qrcode_auth_flow_error_content_description),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .padding(vertical = Margin.ExtraLarge.value)
                .wrapContentSize()
        )
        TitleM3(
            text = uiStringText(title),
            textAlign = TextAlign.Center
        )
        SubtitleM3(
            text = uiStringText(subtitle),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        primaryActionButton?.let { actionButton ->
            if (actionButton.isVisible) {
                PrimaryButtonM3(
                    text = uiStringText(actionButton.label),
                    onClick = { actionButton.clickAction.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
        secondaryActionButton?.let { actionButton ->
            SecondaryButtonM3(
                text = uiStringText(actionButton.label),
                onClick = { actionButton.clickAction.invoke() },
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    AppThemeM3 {
        val state = QRCodeAuthUiState.Error.InvalidData(
            primaryActionButton = ErrorPrimaryActionButton {},
            secondaryActionButton = ErrorSecondaryActionButton {},
        )
        ErrorState(state)
    }
}
