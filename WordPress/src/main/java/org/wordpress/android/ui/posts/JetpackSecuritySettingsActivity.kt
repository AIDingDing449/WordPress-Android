package org.wordpress.android.ui.posts

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import org.wordpress.android.R
import org.wordpress.android.databinding.FragmentJetpackSecuritySettingsBinding
import org.wordpress.android.ui.main.BaseAppCompatActivity
import android.R as AndroidR

class JetpackSecuritySettingsActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(FragmentJetpackSecuritySettingsBinding.inflate(layoutInflater)) {
            setContentView(root)
            setupToolbar()
        }

        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_OK, null)
            finish()
        }
    }

    private fun FragmentJetpackSecuritySettingsBinding.setupToolbar() {
        title = resources.getText(R.string.jetpack_security_setting_title)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemID = item.itemId
        if (itemID == AndroidR.id.home) {
            setResult(RESULT_OK, null)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val JETPACK_SECURITY_SETTINGS_REQUEST_CODE = 101
    }
}
