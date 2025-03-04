package org.wordpress.android.ui.posts.social.compose

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun PostSocialMessageItem(
    message: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .clickable(enabled, onClick = onClick)
            .padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.MediumLarge.value)
    ) {
        Text(
            text = stringResource(R.string.social_item_message_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
                .copy(alpha = if (enabled) 0.74f else 0.38f),
        )
        Text(
            text = message,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
                .copy(alpha = if (enabled) 0.74f else 0.38f),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PostSocialMessageItemPreview() {
    val messages = listOf(
        "5 Chicken Recipes that you have to try on the grill this summer",
        "Small message sample",
        "Message to be shared to the social network when I publish this post"
    )
    var messageId by remember { mutableIntStateOf(0) }

    val updateMessage = {
        messageId = (messageId + 1) % messages.size
    }

    AppThemeM3 {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PostSocialMessageItem(
                message = messages[messageId],
                onClick = updateMessage,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            PostSocialMessageItem(
                message = messages[0],
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}
