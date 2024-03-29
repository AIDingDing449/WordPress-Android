package org.wordpress.android.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.parcelize.parcelableCreator
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.editor.savedinstance.SavedInstanceDatabase.Companion.getDatabase
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.widgets.DiffView

class HistoryDetailFragment : Fragment() {
    private var mRevision: Revision? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRevision = if (getDatabase(WordPress.getContext())?.hasParcel(KEY_REVISION) == true) {
            getDatabase(WordPress.getContext())?.getParcel(KEY_REVISION, parcelableCreator())
        } else {
            getDatabase(WordPress.getContext())?.getParcel(EXTRA_REVISION, parcelableCreator())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.history_detail_fragment, container, false) as ViewGroup
        (rootView.findViewById<View>(R.id.title) as DiffView).showDiffs(mRevision!!.titleDiffs, true)
        (rootView.findViewById<View>(R.id.content) as DiffView).showDiffs(mRevision!!.contentDiffs, false)
        return rootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        getDatabase(WordPress.getContext())?.addParcel(KEY_REVISION, mRevision)
    }

    companion object {
        const val EXTRA_REVISION = "EXTRA_REVISION"
        const val KEY_REVISION = "KEY_REVISION"

        fun newInstance(revision: Revision): HistoryDetailFragment {
            getDatabase(WordPress.getContext())?.addParcel(EXTRA_REVISION, revision)
            return HistoryDetailFragment()
        }
    }
}
