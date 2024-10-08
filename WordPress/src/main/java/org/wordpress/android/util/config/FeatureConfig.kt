package org.wordpress.android.util.config

/**
 * A class that represents a feature configuration which enables the feature to be remotely turned on or off.
 * @param appConfig class that loads the feature configuration
 * @param buildConfigValue is the field in the BuildConfig file. This flag overrides the remote value. Use this field
 * to enable the feature to a certain build (debug, test build) so it doesn't have to rely on remote configuration.
 * @param remoteField is the key of the feature flag in the remote config file, only set this field when there is a
 * remote flag available
 */
open class FeatureConfig(
    private val appConfig: AppConfig,
    val buildConfigValue: Boolean,
    val remoteField: String? = null
) {
    open fun isEnabled(): Boolean {
        return appConfig.isEnabled(this)
    }

    open fun name() = remoteField ?: this.javaClass.name

    fun featureState() = appConfig.featureState(this)
}
