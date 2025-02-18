package org.wordpress.android.ui.domains.management

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.domains.management.DomainManagementViewModel.ActionEvent
import org.wordpress.android.ui.domains.management.details.DomainManagementDetailsActivity
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

@AndroidEntryPoint
class DomainManagementActivity : BaseAppCompatActivity() {
    private val viewModel: DomainManagementViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiStateFlow.collectAsState()

                MyDomainsScreen(
                    uiState = uiState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onDomainTapped = viewModel::onDomainTapped,
                    onAddDomainTapped = viewModel::onAddDomainClicked,
                    onFindDomainTapped = viewModel::onAddDomainClicked,
                    onBackTapped = viewModel::onBackTapped,
                    onRefresh = viewModel::onRefresh,
                )
            }
        }

        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: ActionEvent) {
        when (actionEvent) {
            is ActionEvent.DomainTapped -> {
                startActivity(
                    DomainManagementDetailsActivity.createIntent(
                        this,
                        actionEvent.domain,
                        actionEvent.detailUrl
                    )
                )
            }
            is ActionEvent.AddDomainTapped -> ActivityLauncher.openNewDomainSearch(this)
            is ActionEvent.NavigateBackTapped -> onBackPressedDispatcher.onBackPressed()
        }
    }
}
