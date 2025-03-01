package org.wordpress.android.ui.domains

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.DomainsDashboardActivityBinding
import org.wordpress.android.ui.main.BaseAppCompatActivity
import android.R as AndroidR

class DomainsDashboardActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(DomainsDashboardActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            setupToolbar()
        }
    }

    private fun DomainsDashboardActivityBinding.setupToolbar() {
        setSupportActionBar(toolbarDomains)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
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
