/**
 * Provides a list view and list adapter to display a note. It will have a header view to show
 * the avatar and other details for the post.
 * <p>
 * More specialized note adapters will need to be made to provide the correct views for the type
 * of note/note template it has.
 */
package org.wordpress.android.ui.notifications;

import androidx.annotation.Nullable;

import org.wordpress.android.models.Note;

public interface NotificationFragment {
    interface OnPostClickListener {
        void onPostClicked(Note note, long remoteBlogId, int postId);
    }

    @Nullable
    Note getNote();

    void setNote(String noteId);
}
