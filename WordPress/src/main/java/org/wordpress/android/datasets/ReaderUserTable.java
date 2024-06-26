package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.util.WPAvatarUtils;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;

/**
 * stores info about the current user and liking users
 */
public class ReaderUserTable {
    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_users ("
                   + " user_id INTEGER PRIMARY KEY,"
                   + " blog_id INTEGER DEFAULT 0,"
                   + " user_name TEXT,"
                   + " display_name TEXT COLLATE NOCASE,"
                   + " url TEXT,"
                   + " profile_url TEXT,"
                   + " avatar_url TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_users");
    }

    private static final String COLUMN_NAMES =
            " user_id," // 1
            + " blog_id," // 2
            + " user_name," // 3
            + " display_name," // 4
            + " url," // 5
            + " profile_url," // 6
            + " avatar_url"; // 7

    public static void addOrUpdateUsers(ReaderUserList users) {
        if (users == null || users.size() == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement(
                "INSERT OR REPLACE INTO tbl_users (" + COLUMN_NAMES + ") VALUES (?1,?2,?3,?4,?5,?6,?7)");
        try {
            for (ReaderUser user : users) {
                stmt.bindLong(1, user.userId);
                stmt.bindLong(2, user.blogId);
                stmt.bindString(3, user.getUserName());
                stmt.bindString(4, user.getDisplayName());
                stmt.bindString(5, user.getUrl());
                stmt.bindString(6, user.getProfileUrl());
                stmt.bindString(7, user.getAvatarUrl());
                stmt.execute();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    /*
     * returns avatar urls for the passed user ids - used by post detail to show avatars for liking users
     */
    public static ArrayList<String> getAvatarUrls(ReaderUserIdList userIds, int max, int avatarSz, long wpComUserId) {
        ArrayList<String> avatars = new ArrayList<String>();
        if (userIds == null || userIds.size() == 0) {
            return avatars;
        }

        StringBuilder sb = new StringBuilder("SELECT user_id, avatar_url FROM tbl_users WHERE user_id IN (");

        // make sure current user's avatar is returned if the passed list contains them - this is
        // important since it may not otherwise be returned when a "max" is passed, and we want
        // the current user to appear first in post detail when they like a post
        boolean containsCurrentUser = userIds.contains(wpComUserId);
        if (containsCurrentUser) {
            sb.append(wpComUserId);
        }

        int numAdded = (containsCurrentUser ? 1 : 0);
        for (Long id : userIds) {
            // skip current user since we added them already
            if (id != wpComUserId) {
                if (numAdded > 0) {
                    sb.append(",");
                }
                sb.append(id);
                numAdded++;
                if (max > 0 && numAdded >= max) {
                    break;
                }
            }
        }
        sb.append(")");

        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sb.toString(), null);
        try {
            if (c.moveToFirst()) {
                do {
                    long userId = c.getLong(0);
                    String url = WPAvatarUtils.rewriteAvatarUrl(c.getString(1), avatarSz);
                    // add current user to the top
                    if (userId == wpComUserId) {
                        avatars.add(0, url);
                    } else {
                        avatars.add(url);
                    }
                } while (c.moveToNext());
            }
            return avatars;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static ReaderUser getCurrentUser(final long wpComUserId) {
        return getUser(wpComUserId);
    }

    private static ReaderUser getUser(long userId) {
        String[] args = {Long.toString(userId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_users WHERE user_id=?", args);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            return getUserFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static ReaderUserList getUsersWhoLikePost(long blogId, long postId, int max) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        String sql = "SELECT * from tbl_users WHERE user_id IN "
                     + "(SELECT user_id FROM tbl_post_likes WHERE blog_id=? AND post_id=?) ORDER BY display_name";
        if (max > 0) {
            sql += " LIMIT " + Integer.toString(max);
        }

        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            ReaderUserList users = new ReaderUserList();
            if (c.moveToFirst()) {
                do {
                    users.add(getUserFromCursor(c));
                } while (c.moveToNext());
            }
            return users;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static ReaderUserList getUsersWhoLikeComment(long blogId, long commentId, int max) {
        String[] args = {Long.toString(blogId),
                Long.toString(commentId)};
        String sql = "SELECT * from tbl_users WHERE user_id IN"
                     + " (SELECT user_id FROM tbl_comment_likes WHERE blog_id=? AND comment_id=?)"
                     + " ORDER BY display_name";
        if (max > 0) {
            sql += " LIMIT " + Integer.toString(max);
        }

        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            ReaderUserList users = new ReaderUserList();
            if (c.moveToFirst()) {
                do {
                    users.add(getUserFromCursor(c));
                } while (c.moveToNext());
            }
            return users;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    private static ReaderUser getUserFromCursor(Cursor c) {
        ReaderUser user = new ReaderUser();

        user.userId = c.getLong(c.getColumnIndexOrThrow("user_id"));
        user.blogId = c.getLong(c.getColumnIndexOrThrow("blog_id"));
        user.setUserName(c.getString(c.getColumnIndexOrThrow("user_name")));
        user.setDisplayName(c.getString(c.getColumnIndexOrThrow("display_name")));
        user.setUrl(c.getString(c.getColumnIndexOrThrow("url")));
        user.setProfileUrl(c.getString(c.getColumnIndexOrThrow("profile_url")));
        user.setAvatarUrl(c.getString(c.getColumnIndexOrThrow("avatar_url")));

        return user;
    }
}
