package org.wordpress.android.ui.debug.cookies

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.databinding.DebugCookiesActivityBinding
import org.wordpress.android.ui.main.BaseAppCompatActivity
import android.R as AndroidR

class DebugCookiesActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(DebugCookiesActivityBinding.inflate(layoutInflater).root)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        AndroidR.id.home -> {
            onBackPressedDispatcher.onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
