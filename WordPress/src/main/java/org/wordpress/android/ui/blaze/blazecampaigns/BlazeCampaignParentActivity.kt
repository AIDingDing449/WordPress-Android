package org.wordpress.android.ui.blaze.blazecampaigns

import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailFragment
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingFragment
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.extensions.getParcelableExtraCompat

const val ARG_EXTRA_BLAZE_CAMPAIGN_PAGE = "blaze_campaign_page"

@AndroidEntryPoint
class BlazeCampaignParentActivity : BaseAppCompatActivity() {
    private val viewModel: CampaignViewModel by viewModels()
    private var campaignPage: BlazeCampaignPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        campaignPage =
            savedInstanceState?.getParcelableCompat(ARG_EXTRA_BLAZE_CAMPAIGN_PAGE) ?: intent.getParcelableExtraCompat(
                ARG_EXTRA_BLAZE_CAMPAIGN_PAGE
            )

        setContentView(R.layout.activity_blaze_campaign)
        viewModel.start(campaignPage)
        observe()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_EXTRA_BLAZE_CAMPAIGN_PAGE, campaignPage)
    }

    private fun observe() {
        viewModel.uiState.observe(this) { uiState ->
            campaignPage = uiState

            when (uiState) {
                is BlazeCampaignPage.CampaignListingPage -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, CampaignListingFragment.newInstance(uiState.source))
                        .commitNow()
                }

                is BlazeCampaignPage.CampaignDetailsPage -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, CampaignDetailFragment.newInstance(uiState.campaignId, uiState.source))
                        .commitNow()
                }

                is BlazeCampaignPage.Done -> {
                    finish()
                }

                else -> {}
            }
        }
    }
}
