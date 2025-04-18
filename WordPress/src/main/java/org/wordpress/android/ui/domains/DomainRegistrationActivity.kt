package org.wordpress.android.ui.domains

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.DomainRegistrationActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenPlans.PlanDetails
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.FinishDomainRegistration
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationCheckout
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationDetails
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationResult
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainSuggestions
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenFreeDomainWithAnnualPlan
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject
import android.R as AndroidR

@AndroidEntryPoint
class DomainRegistrationActivity : BaseAppCompatActivity(), ScrollableViewInitializedListener {
    enum class DomainRegistrationPurpose {
        AUTOMATED_TRANSFER,
        CTA_DOMAIN_CREDIT_REDEMPTION,
        DOMAIN_PURCHASE,
        FREE_DOMAIN_WITH_ANNUAL_PLAN
    }

    companion object {
        const val RESULT_REGISTERED_DOMAIN_EMAIL = "RESULT_REGISTERED_DOMAIN_EMAIL"
        const val DOMAIN_REGISTRATION_PURPOSE_KEY = "DOMAIN_REGISTRATION_PURPOSE_KEY"
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DomainRegistrationMainViewModel
    private lateinit var binding: DomainRegistrationActivityBinding

    private val openCheckout = registerForActivityResult(DomainRegistrationCheckoutWebViewActivity.OpenCheckout()) {
        it?.let {
            viewModel.completeDomainRegistration(it)
        }
    }

    private val openPlans = registerForActivityResult(DomainRegistrationCheckoutWebViewActivity.OpenPlans()) {
        it?.let {
            viewModel.completeDomainRegistration(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(DomainRegistrationActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            binding = this

            val site = requireNotNull(intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE))
            val domainRegistrationPurpose = requireNotNull(
                intent.getSerializableExtraCompat<DomainRegistrationPurpose>(DOMAIN_REGISTRATION_PURPOSE_KEY)
            )

            setupToolbar()
            setupViewModel(site, domainRegistrationPurpose)
            setupObservers()
        }
    }

    private fun DomainRegistrationActivityBinding.setupToolbar() {
        setSupportActionBar(toolbarDomain)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViewModel(site: SiteModel, domainRegistrationPurpose: DomainRegistrationPurpose) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(DomainRegistrationMainViewModel::class.java)
        viewModel.start(site, domainRegistrationPurpose)
    }

    private fun setupObservers() {
        viewModel.onNavigation.observeEvent(this) {
            when (it) {
                is OpenDomainSuggestions -> showDomainSuggestions()
                is OpenFreeDomainWithAnnualPlan -> openFreeDomainWithAnnualPlanWebView(it.site, it.details)
                is OpenDomainRegistrationCheckout -> openDomainRegistrationCheckoutWebView(it.site, it.details)
                is OpenDomainRegistrationDetails -> showDomainRegistrationDetails(it.details)
                is OpenDomainRegistrationResult -> showDomainRegistrationResult(it.event)
                is FinishDomainRegistration -> finishDomainRegistration(it.event)
            }
        }
    }

    private fun showDomainSuggestions() {
        showFragment(DomainSuggestionsFragment.TAG, true) {
            DomainSuggestionsFragment.newInstance()
        }
    }

    private fun openFreeDomainWithAnnualPlanWebView(site: SiteModel, details: DomainProductDetails) {
        openPlans.launch(PlanDetails(site, details.domainName))
    }

    private fun openDomainRegistrationCheckoutWebView(site: SiteModel, details: DomainProductDetails) {
        openCheckout.launch(CheckoutDetails(site, details.domainName))
    }

    private fun showDomainRegistrationDetails(details: DomainProductDetails) {
        showFragment(DomainRegistrationDetailsFragment.TAG) {
            DomainRegistrationDetailsFragment.newInstance(details)
        }
    }

    private fun showDomainRegistrationResult(event: DomainRegistrationCompletedEvent) {
        showFragment(DomainRegistrationResultFragment.TAG) {
            DomainRegistrationResultFragment.newInstance(event.domainName, event.email)
        }
    }

    private fun finishDomainRegistration(event: DomainRegistrationCompletedEvent) {
        setResult(RESULT_OK, Intent().putExtra(RESULT_REGISTERED_DOMAIN_EMAIL, event.email))
        finish()
    }

    private fun showFragment(
        tag: String,
        isRootFragment: Boolean = false,
        factory: () -> Fragment
    ) = with(supportFragmentManager) {
        beginTransaction().apply {
            if (!isRootFragment) {
                setCustomAnimations(
                    R.anim.activity_slide_in_from_right,
                    R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left,
                    R.anim.activity_slide_out_to_right
                )
                addToBackStack(null)
            }
            replace(R.id.fragment_container, findFragmentByTag(tag) ?: factory(), tag)
            commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        binding.appbarMain.apply {
            if (containerId == R.id.domain_suggestions_list) {
                post {
                    isLiftOnScroll = false
                    setLifted(false)
                    elevation = 0F
                    requestLayout()
                }
            } else {
                post {
                    isLiftOnScroll = true
                    liftOnScrollTargetViewId = containerId
                    requestLayout()
                }
            }
        }
    }
}
