package org.wordpress.android.ui.stats.refresh.lists.sections.insights.management

import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.InsightsManagementActivityBinding
import org.wordpress.android.ui.main.BaseAppCompatActivity
import android.R as AndroidR

@AndroidEntryPoint
class InsightsManagementActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(InsightsManagementActivityBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbarMain)
        }

        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
