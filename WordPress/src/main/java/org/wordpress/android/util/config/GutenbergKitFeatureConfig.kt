package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig
import org.wordpress.android.annotation.Feature
import javax.inject.Inject

private const val GUTENBERG_KIT_FEATURE_REMOTE_FIELD = "experimental_block_editor"

@Feature(GUTENBERG_KIT_FEATURE_REMOTE_FIELD, false)
class GutenbergKitFeatureConfig @Inject constructor(
    appConfig: AppConfig
) : FeatureConfig(
    appConfig,
    BuildConfig.ENABLE_GUTENBERG_KIT,
    GUTENBERG_KIT_FEATURE_REMOTE_FIELD
)
