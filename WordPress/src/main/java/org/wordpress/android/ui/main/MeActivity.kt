package org.wordpress.android.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.prefs.AppSettingsFragment.LANGUAGE_CHANGED
import android.R as AndroidR

@AndroidEntryPoint
class MeActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.me_activity)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == AndroidR.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.APP_SETTINGS -> {
                if (resultCode == LANGUAGE_CHANGED) {
                    // Refresh the app
                    val refresh = Intent(this, this.javaClass)
                    startActivity(refresh)
                    setResult(LANGUAGE_CHANGED)
                    finish()
                }
            }
        }
    }
}
