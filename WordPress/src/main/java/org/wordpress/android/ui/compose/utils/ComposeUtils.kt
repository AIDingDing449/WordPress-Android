package org.wordpress.android.ui.compose.utils

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.wordpress.android.util.extensions.isRtl
import org.wordpress.android.util.extensions.primaryLocale
import java.util.Locale

/**
 * Utility function that wraps the [content] inside a [CompositionLocalProvider] overriding the [LocalContext]
 * configuration with the specified [locale] when the specified language should apply.
 * Useful to apply a custom language to Compose UIs that do not respond correctly to app language changes.
 * @param locale The locale to be used in the [LocalContext] configuration override.
 * @param onLocaleChange Callback to be invoked when the locale is overridden, useful to update other app components.
 * @param content The Composable function to be rendered with the overridden locale.
 */

@Composable
@Suppress("DEPRECATION")
@SuppressLint("AppBundleLocaleChanges")
fun LocaleAwareComposable(
    locale: Locale = Locale.getDefault(),
    onLocaleChange: (Locale) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val currentLocale = context.primaryLocale
    if (currentLocale != locale) {
        val newConfiguration = Configuration(configuration).apply {
            setLocale(locale)
        }

        val newContext = context.createConfigurationContext(newConfiguration)
        val newLayoutDirection = if (newConfiguration.isRtl()) LayoutDirection.Rtl else LayoutDirection.Ltr
        onLocaleChange(locale)
        CompositionLocalProvider(
            LocalContext provides newContext,
            LocalLayoutDirection provides newLayoutDirection,
        ) {
            content()
        }
    } else {
        content()
    }
}

/**
 * Indicates whether the currently selected theme is light.
 * @return true if the current theme is light
 */
@Composable
fun isLightTheme(): Boolean = isSystemInDarkTheme().not()
