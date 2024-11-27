package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val GUTENBERG_KIT_THEME_STYLES_FEATURE_REMOTE_FIELD = "experimental_block_editor_theme_styles"

@Feature(GUTENBERG_KIT_THEME_STYLES_FEATURE_REMOTE_FIELD, false)
class GutenbergKitThemeStylesFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_GUTENBERG_KIT_THEME_STYLES,
    GUTENBERG_KIT_THEME_STYLES_FEATURE_REMOTE_FIELD
)
