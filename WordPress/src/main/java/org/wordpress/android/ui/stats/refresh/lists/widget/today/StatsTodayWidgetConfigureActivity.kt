package org.wordpress.android.ui.stats.refresh.lists.widget.today

import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.StatsTodayWidgetConfigureActivityBinding
import androidx.appcompat.app.AppCompatActivity
import android.R as AndroidR

@AndroidEntryPoint
class StatsTodayWidgetConfigureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(StatsTodayWidgetConfigureActivityBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbar.toolbarMain)
        }
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
