package org.wordpress.android.ui.jetpack.scan.details

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString

sealed class ThreatDetailsNavigationEvents {
    class OpenThreatActionDialog(
        val title: UiString,
        val message: UiString,
        val okButtonAction: () -> Unit
    ) : ThreatDetailsNavigationEvents() {
        @StringRes
        val positiveButtonLabel: Int = R.string.dialog_button_ok

        @StringRes
        val negativeButtonLabel: Int = R.string.dialog_button_cancel
    }

    data class ShowUpdatedScanStateWithMessage(@StringRes val messageRes: Int) : ThreatDetailsNavigationEvents()

    data class ShowUpdatedFixState(val threatId: Long) : ThreatDetailsNavigationEvents()

    data class ShowGetFreeEstimate(val url: String) : ThreatDetailsNavigationEvents()

    data class ShowJetpackSettings(val url: String) : ThreatDetailsNavigationEvents()
}
