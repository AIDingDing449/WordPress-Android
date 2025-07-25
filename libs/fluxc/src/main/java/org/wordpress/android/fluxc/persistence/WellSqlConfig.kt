@file:Suppress("UnusedImports")

package org.wordpress.android.fluxc.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.preference.PreferenceManager
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringDef
import com.yarolegovich.wellsql.DefaultWellConfig
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.WellTableManager
import org.wordpress.android.fluxc.BuildConfig
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Suppress("LargeClass")
open class WellSqlConfig : DefaultWellConfig {
    companion object {
        const val ADDON_WOOCOMMERCE = "WC"

        // The maximum value of a host parameter number is SQLITE_MAX_VARIABLE_NUMBER, which defaults to 999 for
        // SQLite versions prior to 3.32.0 (2020-05-22) or 32766 for SQLite versions after 3.32.0.
        // @see https://www.sqlite.org/limits.html
        const val SQLITE_MAX_VARIABLE_NUMBER = 999
    }

    constructor(context: Context) : super(context)

    @Suppress("SpreadOperator")
    constructor(context: Context, @AddOn vararg addOns: String) : super(context, mutableSetOf(*addOns))

    @Retention(SOURCE)
    @StringDef(ADDON_WOOCOMMERCE)
    @Target(VALUE_PARAMETER)
    annotation class AddOn

    override fun getDbVersion(): Int {
        return 209
    }

    override fun getDbName(): String {
        return "wp-fluxc"
    }

    override fun onCreate(db: SQLiteDatabase, helper: WellTableManager) {
        mTables.forEach { table -> helper.createTable(table) }
    }

    @Suppress("CheckStyle", "LongMethod", "ComplexMethod", "MagicNumber")
    override fun onUpgrade(db: SQLiteDatabase, helper: WellTableManager, oldVersion: Int, newVersion: Int) {
        AppLog.d(T.DB, "Upgrading database from version $oldVersion to $newVersion")

        db.beginTransaction()
        for (version in oldVersion..newVersion) {
            when (version) {
                1 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS SiteModel")
                    db.execSQL(
                            "CREATE TABLE SiteModel (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "SITE_ID INTEGER," +
                                "URL TEXT," +
                                "ADMIN_URL TEXT," +
                                "LOGIN_URL TEXT," +
                                "NAME TEXT," +
                                "DESCRIPTION TEXT," +
                                "IS_WPCOM INTEGER," +
                                "IS_FEATURED_IMAGE_SUPPORTED INTEGER," +
                                "DEFAULT_COMMENT_STATUS TEXT," +
                                "TIMEZONE TEXT," +
                                "SELF_HOSTED_SITE_ID INTEGER," +
                                "USERNAME TEXT," +
                                "PASSWORD TEXT," +
                                "XMLRPC_URL TEXT," +
                                "SOFTWARE_VERSION TEXT," +
                                "IS_SELF_HOSTED_ADMIN INTEGER," +
                                "IS_JETPACK_INSTALLED INTEGER," +
                                "IS_JETPACK_CONNECTED INTEGER," +
                                "IS_AUTOMATED_TRANSFER INTEGER," +
                                "IS_VISIBLE INTEGER," +
                                "IS_PRIVATE INTEGER," +
                                "IS_VIDEO_PRESS_SUPPORTED INTEGER," +
                                "PLAN_ID INTEGER," +
                                "PLAN_SHORT_NAME TEXT," +
                                "ICON_URL TEXT," +
                                "HAS_CAPABILITY_EDIT_PAGES INTEGER," +
                                "HAS_CAPABILITY_EDIT_POSTS INTEGER," +
                                "HAS_CAPABILITY_EDIT_OTHERS_POSTS INTEGER," +
                                "HAS_CAPABILITY_EDIT_OTHERS_PAGES INTEGER," +
                                "HAS_CAPABILITY_DELETE_POSTS INTEGER," +
                                "HAS_CAPABILITY_DELETE_OTHERS_POSTS INTEGER," +
                                "HAS_CAPABILITY_EDIT_THEME_OPTIONS INTEGER," +
                                "HAS_CAPABILITY_EDIT_USERS INTEGER," +
                                "HAS_CAPABILITY_LIST_USERS INTEGER," +
                                "HAS_CAPABILITY_MANAGE_CATEGORIES INTEGER," +
                                "HAS_CAPABILITY_MANAGE_OPTIONS INTEGER," +
                                "HAS_CAPABILITY_ACTIVATE_WORDADS INTEGER," +
                                "HAS_CAPABILITY_PROMOTE_USERS INTEGER," +
                                "HAS_CAPABILITY_PUBLISH_POSTS INTEGER," +
                                "HAS_CAPABILITY_UPLOAD_FILES INTEGER," +
                                "HAS_CAPABILITY_DELETE_USER INTEGER," +
                                "HAS_CAPABILITY_REMOVE_USERS INTEGER," +
                                "HAS_CAPABILITY_VIEW_STATS INTEGER," +
                                "UNIQUE (SITE_ID, URL))"
                    )
                    db.execSQL("DROP TABLE IF EXISTS AccountModel")
                    db.execSQL(
                            "CREATE TABLE AccountModel (" +
                                "_id INTEGER PRIMARY KEY," +
                                "USER_NAME TEXT," +
                                "USER_ID INTEGER," +
                                "DISPLAY_NAME TEXT," +
                                "PROFILE_URL TEXT," +
                                "AVATAR_URL TEXT," +
                                "EMAIL TEXT," +
                                "PRIMARY_SITE_ID INTEGER," +
                                "SITE_COUNT INTEGER," +
                                "VISIBLE_SITE_COUNT INTEGER," +
                                "HAS_UNSEEN_NOTES INTEGER," +
                                "FIRST_NAME TEXT," +
                                "LAST_NAME TEXT," +
                                "ABOUT_ME TEXT," +
                                "DATE TEXT," +
                                "NEW_EMAIL TEXT," +
                                "PENDING_EMAIL_CHANGE INTEGER," +
                                "WEB_ADDRESS TEXT)"
                    )
                    db.execSQL("DROP TABLE IF EXISTS HTTPAuthModel")
                    db.execSQL(
                            "CREATE TABLE HTTPAuthModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,ROOT_URL TEXT," +
                                    "REALM TEXT,USERNAME TEXT,PASSWORD TEXT,UNIQUE (ROOT_URL))"
                    )
                    db.execSQL("DROP TABLE IF EXISTS PostFormatModel")
                    db.execSQL(
                            "CREATE TABLE PostFormatModel (SITE_ID INTEGER,SLUG TEXT,DISPLAY_NAME TEXT," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT)"
                    )
                    db.execSQL("DROP TABLE IF EXISTS PostModel")
                    db.execSQL(
                            "CREATE TABLE PostModel (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER," +
                                "REMOTE_SITE_ID INTEGER," +
                                "REMOTE_POST_ID INTEGER," +
                                "TITLE TEXT,CONTENT TEXT," +
                                "DATE_CREATED TEXT," +
                                "CATEGORY_IDS TEXT," +
                                "CUSTOM_FIELDS TEXT," +
                                "LINK TEXT,EXCERPT TEXT," +
                                "TAG_NAMES TEXT," +
                                "STATUS TEXT," +
                                "PASSWORD TEXT," +
                                "FEATURED_IMAGE_ID INTEGER," +
                                "POST_FORMAT TEXT,SLUG TEXT," +
                                "LATITUDE REAL," +
                                "LONGITUDE REAL," +
                                "IS_PAGE INTEGER,PARENT_ID INTEGER," +
                                "PARENT_TITLE TEXT," +
                                "IS_LOCAL_DRAFT INTEGER," +
                                "IS_LOCALLY_CHANGED INTEGER," +
                                "DATE_LOCALLY_CHANGED TEXT," +
                                "LAST_KNOWN_REMOTE_FEATURED_IMAGE_ID INTEGER," +
                                "HAS_CAPABILITY_PUBLISH_POST INTEGER," +
                                "HAS_CAPABILITY_EDIT_POST INTEGER," +
                                "HAS_CAPABILITY_DELETE_POST INTEGER)"
                    )
                    db.execSQL("DROP TABLE IF EXISTS MediaModel")
                    db.execSQL(
                            "CREATE TABLE MediaModel (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER," +
                                "MEDIA_ID INTEGER," +
                                "POST_ID INTEGER," +
                                "AUTHOR_ID INTEGER," +
                                "GUID TEXT," +
                                "UPLOAD_DATE TEXT," +
                                "URL TEXT," +
                                "THUMBNAIL_URL TEXT," +
                                "FILE_NAME TEXT," +
                                "FILE_PATH TEXT," +
                                "FILE_EXTENSION TEXT," +
                                "MIME_TYPE TEXT," +
                                "TITLE TEXT," +
                                "CAPTION TEXT," +
                                "DESCRIPTION TEXT," +
                                "ALT TEXT," +
                                "WIDTH INTEGER," +
                                "HEIGHT INTEGER," +
                                "LENGTH INTEGER," +
                                "VIDEO_PRESS_GUID TEXT," +
                                "VIDEO_PRESS_PROCESSING_DONE INTEGER," +
                                "UPLOAD_STATE TEXT," +
                                "HORIZONTAL_ALIGNMENT INTEGER," +
                                "VERTICAL_ALIGNMENT INTEGER," +
                                "FEATURED INTEGER," +
                                "FEATURED_IN_POST INTEGER)"
                    )
                    db.execSQL("DROP TABLE IF EXISTS TermModel")
                    db.execSQL(
                            "CREATE TABLE TermModel (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER," +
                                "REMOTE_TERM_ID INTEGER," +
                                "TAXONOMY TEXT," +
                                "NAME TEXT," +
                                "SLUG TEXT," +
                                "DESCRIPTION TEXT," +
                                "PARENT_REMOTE_ID INTEGER)"
                    )
                }
                2 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD FRAME_NONCE TEXT")
                }
                3 -> migrate(version) {
                    db.execSQL("ALTER TABLE AccountModel ADD EMAIL_VERIFIED BOOLEAN")
                }
                4 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD ORIGIN INTEGER")
                }
                5 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD HAS_FREE_PLAN BOOLEAN")
                }
                6 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD UNMAPPED_URL TEXT")
                }
                7 -> migrate(version) {
                    db.execSQL("ALTER TABLE MediaModel ADD LOCAL_POST_ID INTEGER")
                }
                8 -> migrate(version) {
                    db.execSQL("ALTER TABLE MediaModel ADD FILE_URL_MEDIUM_SIZE TEXT")
                    db.execSQL("ALTER TABLE MediaModel ADD FILE_URL_MEDIUM_LARGE_SIZE TEXT")
                    db.execSQL("ALTER TABLE MediaModel ADD FILE_URL_LARGE_SIZE TEXT")
                }
                9 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD MAX_UPLOAD_SIZE INTEGER")
                }
                10 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD MEMORY_LIMIT INTEGER")
                }
                11 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE RoleModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,SITE_ID INTEGER," +
                                "NAME TEXT,DISPLAY_NAME TEXT)"
                    )
                }
                12 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE PluginModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "NAME TEXT,DISPLAY_NAME TEXT,PLUGIN_URL TEXT,VERSION TEXT,SLUG TEXT,DESCRIPTION TEXT," +
                                "AUTHOR_NAME TEXT,AUTHOR_URL TEXT,IS_ACTIVE INTEGER,IS_AUTO_UPDATE_ENABLED INTEGER)"
                    )
                }
                13 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE PluginInfoModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "NAME TEXT,SLUG TEXT,VERSION TEXT,RATING TEXT,ICON TEXT)"
                    )
                }
                14 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE MediaUploadModel (_id INTEGER PRIMARY KEY,UPLOAD_STATE INTEGER," +
                                "PROGRESS REAL,ERROR_TYPE TEXT,ERROR_MESSAGE TEXT,FOREIGN KEY(_id) REFERENCES " +
                                "MediaModel(_id) ON DELETE CASCADE)"
                    )
                    db.execSQL(
                            "CREATE TABLE PostUploadModel (_id INTEGER PRIMARY KEY,UPLOAD_STATE INTEGER," +
                                "ASSOCIATED_MEDIA_IDS TEXT,ERROR_TYPE TEXT,ERROR_MESSAGE TEXT," +
                                "FOREIGN KEY(_id) REFERENCES PostModel(_id) ON DELETE CASCADE)"
                    )
                }
                15 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE ThemeModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "THEME_ID TEXT,NAME TEXT,DESCRIPTION TEXT,SLUG TEXT,VERSION TEXT,AUTHOR_NAME TEXT," +
                                "AUTHOR_URL TEXT,THEME_URL TEXT,SCREENSHOT_URL TEXT,DEMO_URL TEXT,DOWNLOAD_URL TEXT," +
                                "STYLESHEET TEXT,CURRENCY TEXT,PRICE REAL,ACTIVE INTEGER,AUTO_UPDATE INTEGER," +
                                "AUTO_UPDATE_TRANSLATION INTEGER,IS_WP_COM_THEME INTEGER)"
                    )
                }
                16 -> migrate(version) {
                    db.execSQL("ALTER TABLE ThemeModel ADD FREE INTEGER")
                    db.execSQL("ALTER TABLE ThemeModel ADD PRICE_TEXT INTEGER")
                }
                17 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD EMAIL TEXT")
                    db.execSQL("ALTER TABLE SiteModel ADD DISPLAY_NAME TEXT")
                }
                18 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD JETPACK_VERSION TEXT")
                }
                19 -> migrate(version) {
                    db.execSQL("ALTER TABLE TermModel ADD POST_COUNT INTEGER")
                }
                20 -> migrate(version) {
                    db.execSQL("ALTER TABLE PluginModel rename to SitePluginModel")
                    db.execSQL("ALTER TABLE PluginInfoModel rename to WPOrgPluginModel")
                }
                21 -> migrate(version) {
                    db.execSQL("ALTER TABLE SitePluginModel ADD SETTINGS_URL TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD AUTHOR_AS_HTML TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD BANNER TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD DESCRIPTION_AS_HTML TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD FAQ_AS_HTML TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD HOMEPAGE_URL TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD INSTALLATION_INSTRUCTIONS_AS_HTML TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD LAST_UPDATED TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD REQUIRED_WORD_PRESS_VERSION TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD WHATS_NEW_AS_HTML TEXT")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD DOWNLOAD_COUNT INTEGER")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD NUMBER_OF_RATINGS INTEGER")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD NUMBER_OF_RATINGS_OF_ONE INTEGER")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD NUMBER_OF_RATINGS_OF_TWO INTEGER")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD NUMBER_OF_RATINGS_OF_THREE INTEGER")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD NUMBER_OF_RATINGS_OF_FOUR INTEGER")
                    db.execSQL("ALTER TABLE WPOrgPluginModel ADD NUMBER_OF_RATINGS_OF_FIVE INTEGER")
                }
                22 -> migrate(version) {
                    db.execSQL("ALTER TABLE ThemeModel ADD MOBILE_FRIENDLY_CATEGORY_SLUG TEXT")
                }
                23 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE PluginDirectoryModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "SLUG TEXT,DIRECTORY_TYPE TEXT,PAGE INTEGER)"
                    )
                }
                24 -> migrate(version) {
                    /**
                     * Start with a clean slate for Plugins. This migration adds unique constraints for
                     * [SitePluginModel] and [WPOrgPluginModel] tables. Adds `authorName` column and renames `name`
                     * column to `displayName` in [WPOrgPluginModel] table. Since these records are only used as cache
                     * and would & should be refreshed often, there is no real harm to do this other than a slightly
                     * longer loading time for the first usage after the migration. This migration would be much more
                     * complicated otherwise.
                     */
                    db.execSQL("DELETE FROM PluginDirectoryModel")
                    db.execSQL("DROP TABLE IF EXISTS SitePluginModel")
                    db.execSQL("DROP TABLE IF EXISTS WPOrgPluginModel")
                    db.execSQL(
                            "CREATE TABLE SitePluginModel (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "NAME TEXT,DISPLAY_NAME TEXT,PLUGIN_URL TEXT,VERSION TEXT,SLUG TEXT,DESCRIPTION TEXT," +
                                "AUTHOR_NAME TEXT,AUTHOR_URL TEXT,SETTINGS_URL TEXT,IS_ACTIVE INTEGER," +
                                "IS_AUTO_UPDATE_ENABLED INTEGER,UNIQUE (SLUG, LOCAL_SITE_ID))"
                    )
                    db.execSQL(
                            "CREATE TABLE WPOrgPluginModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "AUTHOR_AS_HTML TEXT,AUTHOR_NAME TEXT,BANNER TEXT,DESCRIPTION_AS_HTML TEXT," +
                                "DISPLAY_NAME TEXT,FAQ_AS_HTML TEXT,HOMEPAGE_URL TEXT,ICON TEXT," +
                                "INSTALLATION_INSTRUCTIONS_AS_HTML TEXT,LAST_UPDATED TEXT,RATING TEXT," +
                                "REQUIRED_WORD_PRESS_VERSION TEXT,SLUG TEXT,VERSION TEXT,WHATS_NEW_AS_HTML TEXT," +
                                "DOWNLOAD_COUNT INTEGER,NUMBER_OF_RATINGS INTEGER,NUMBER_OF_RATINGS_OF_ONE INTEGER," +
                                "NUMBER_OF_RATINGS_OF_TWO INTEGER,NUMBER_OF_RATINGS_OF_THREE INTEGER," +
                                "NUMBER_OF_RATINGS_OF_FOUR INTEGER,NUMBER_OF_RATINGS_OF_FIVE INTEGER,UNIQUE (SLUG))"
                    )
                }
                25 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD SPACE_AVAILABLE INTEGER")
                    db.execSQL("ALTER TABLE SiteModel ADD SPACE_ALLOWED INTEGER")
                    db.execSQL("ALTER TABLE SiteModel ADD SPACE_USED INTEGER")
                    db.execSQL("ALTER TABLE SiteModel ADD SPACE_PERCENT_USED REAL")
                }
                26 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD IS_WP_COM_STORE INTEGER")
                    db.execSQL("ALTER TABLE SiteModel ADD HAS_WOO_COMMERCE INTEGER")
                }
                27 -> migrate(version) {
                    db.execSQL("ALTER TABLE AccountModel ADD TRACKS_OPT_OUT BOOLEAN")
                }
                28 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE ActivityLogModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER,ACTIVITY_ID TEXT NOT NULL," +
                                "SUMMARY TEXT NOT NULL,TEXT TEXT NOT NULL,NAME TEXT,TYPE TEXT,GRIDICON TEXT," +
                                "STATUS TEXT,REWINDABLE INTEGER,REWIND_ID TEXT,PUBLISHED TEXT NOT NULL," +
                                "DISCARDED INTEGER,DISPLAY_NAME TEXT,ACTOR_TYPE TEXT,WPCOM_USER_ID INTEGER," +
                                "AVATAR_URL TEXT,ROLE TEXT)"
                    )
                    db.execSQL(
                            "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "REMOTE_SITE_ID INTEGER,REWIND_STATE TEXT,REASON TEXT,RESTORE_ID TEXT," +
                                "RESTORE_STATE TEXT,RESTORE_PROGRESS INTEGER,RESTORE_MESSAGE TEXT," +
                                "RESTORE_ERROR_CODE TEXT,RESTORE_FAILURE_REASON TEXT)"
                    )
                }
                29 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE SubscriptionModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "SUBSCRIPTION_ID TEXT,BLOG_ID TEXT,BLOG_NAME TEXT,FEED_ID TEXT,URL TEXT," +
                                "SHOULD_NOTIFY_POSTS INTEGER,SHOULD_EMAIL_POSTS INTEGER," +
                                "EMAIL_POSTS_FREQUENCY TEXT,SHOULD_EMAIL_COMMENTS INTEGER)"
                    )
                }
                30 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS ActivityLogModel")
                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS ActivityLog (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER,ACTIVITY_ID TEXT NOT NULL," +
                                "SUMMARY TEXT NOT NULL,TEXT TEXT NOT NULL,NAME TEXT,TYPE TEXT,GRIDICON TEXT," +
                                "STATUS TEXT,REWINDABLE INTEGER,REWIND_ID TEXT,PUBLISHED INTEGER,DISCARDED INTEGER," +
                                "DISPLAY_NAME TEXT,ACTOR_TYPE TEXT,WPCOM_USER_ID INTEGER,AVATAR_URL TEXT,ROLE TEXT)"
                    )
                }
                31 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCOrderModel")
                    db.execSQL("DROP TABLE IF EXISTS WCOrderNoteModel")
                    db.execSQL(
                            "CREATE TABLE WCOrderModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_ORDER_ID INTEGER,NUMBER TEXT NOT NULL," +
                                "STATUS TEXT NOT NULL,CURRENCY TEXT NOT NULL,DATE_CREATED TEXT NOT NULL," +
                                "TOTAL TEXT NOT NULL,TOTAL_TAX TEXT NOT NULL,SHIPPING_TOTAL TEXT NOT NULL," +
                                "PAYMENT_METHOD TEXT NOT NULL,PAYMENT_METHOD_TITLE TEXT NOT NULL," +
                                "PRICES_INCLUDE_TAX INTEGER,CUSTOMER_NOTE TEXT NOT NULL,DISCOUNT_TOTAL TEXT NOT NULL," +
                                "DISCOUNT_CODES TEXT NOT NULL,REFUND_TOTAL REAL,BILLING_FIRST_NAME TEXT NOT NULL," +
                                "BILLING_LAST_NAME TEXT NOT NULL,BILLING_COMPANY TEXT NOT NULL," +
                                "BILLING_ADDRESS1 TEXT NOT NULL,BILLING_ADDRESS2 TEXT NOT NULL," +
                                "BILLING_CITY TEXT NOT NULL,BILLING_STATE TEXT NOT NULL," +
                                "BILLING_POSTCODE TEXT NOT NULL,BILLING_COUNTRY TEXT NOT NULL," +
                                "BILLING_EMAIL TEXT NOT NULL,BILLING_PHONE TEXT NOT NULL," +
                                "SHIPPING_FIRST_NAME TEXT NOT NULL,SHIPPING_LAST_NAME TEXT NOT NULL," +
                                "SHIPPING_COMPANY TEXT NOT NULL,SHIPPING_ADDRESS1 TEXT NOT NULL," +
                                "SHIPPING_ADDRESS2 TEXT NOT NULL,SHIPPING_CITY TEXT NOT NULL," +
                                "SHIPPING_STATE TEXT NOT NULL,SHIPPING_POSTCODE TEXT NOT NULL," +
                                "SHIPPING_COUNTRY TEXT NOT NULL,LINE_ITEMS TEXT NOT NULL)"
                    )
                    db.execSQL(
                            "CREATE TABLE WCOrderNoteModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,LOCAL_ORDER_ID INTEGER,REMOTE_NOTE_ID INTEGER," +
                                "DATE_CREATED TEXT NOT NULL,NOTE TEXT NOT NULL,IS_CUSTOMER_NOTE INTEGER)")
                }
                32 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS RewindStatus")
                    db.execSQL(
                            "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER,STATE TEXT NOT NULL," +
                                "LAST_UPDATED INTEGER,REASON TEXT,CAN_AUTOCONFIGURE INTEGER,REWIND_ID TEXT," +
                                "REWIND_STATUS TEXT,REWIND_STARTED_AT INTEGER,REWIND_PROGRESS INTEGER," +
                                "REWIND_REASON TEXT)"
                    )
                    db.execSQL(
                            "CREATE TABLE RewindStatusCredentials (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "REWIND_STATE_ID INTEGER,TYPE TEXT NOT NULL,ROLE TEXT NOT NULL,STILL_VALID INTEGER," +
                                "HOST TEXT,PORT INTEGER)"
                    )
                }
                33 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS RewindStatusCredentials")
                    db.execSQL(
                            "CREATE TABLE RewindStatusCredentials (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER,REWIND_STATE_ID INTEGER," +
                                "TYPE TEXT NOT NULL,ROLE TEXT NOT NULL,STILL_VALID INTEGER,HOST TEXT,PORT INTEGER)"
                    )
                }
                34 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS RewindStatus")
                    db.execSQL(
                            "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER,STATE TEXT NOT NULL," +
                                "LAST_UPDATED INTEGER,REASON TEXT,CAN_AUTOCONFIGURE INTEGER,REWIND_ID TEXT," +
                                "REWIND_STATUS TEXT,REWIND_PROGRESS INTEGER,REWIND_REASON TEXT)"
                    )
                }
                35 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCOrderStatsModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,UNIT TEXT NOT NULL,FIELDS TEXT NOT NULL,DATA TEXT NOT NULL)")
                }
                36 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS RewindStatus")
                    db.execSQL(
                            "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER,STATE TEXT NOT NULL," +
                                "LAST_UPDATED INTEGER,REASON TEXT,CAN_AUTOCONFIGURE INTEGER,REWIND_ID TEXT," +
                                "RESTORE_ID INTEGER,REWIND_STATUS TEXT,REWIND_PROGRESS INTEGER,REWIND_REASON TEXT)"
                    )
                }
                37 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS QuickStartModel")
                    db.execSQL(
                            "CREATE TABLE QuickStartTaskModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "SITE_ID INTEGER,TASK_NAME TEXT,IS_DONE INTEGER,IS_SHOWN INTEGER)"
                    )
                    db.execSQL(
                            "CREATE TABLE QuickStartStatusModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "SITE_ID INTEGER,IS_COMPLETED INTEGER,IS_NOTIFICATION_RECEIVED INTEGER)"
                    )
                }
                38 -> migrate(version) {
                    val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                    defaultSharedPrefs.getString("ACCOUNT_TOKEN_PREF_KEY", "")?.let { token ->
                        if (token.isNotEmpty()) {
                            AppLog.d(T.DB, "Migrating token to fluxc-preferences")
                            val fluxCPreferences = context.getSharedPreferences(
                                    context.packageName + "_fluxc-preferences",
                                    Context.MODE_PRIVATE
                            )
                            fluxCPreferences.edit().putString("ACCOUNT_TOKEN_PREF_KEY", token).apply()
                            defaultSharedPrefs.edit().remove("ACCOUNT_TOKEN_PREF_KEY").apply()
                        }
                    }
                }
                39 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS QuickStartModel")
                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS QuickStartTaskModel (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT,SITE_ID INTEGER,TASK_NAME TEXT," +
                                "IS_DONE INTEGER,IS_SHOWN INTEGER)"
                    )
                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS QuickStartStatusModel (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT,SITE_ID INTEGER,IS_COMPLETED INTEGER," +
                                "IS_NOTIFICATION_RECEIVED INTEGER)"
                    )
                }
                40 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS ActivityLog")
                    db.execSQL(
                            "CREATE TABLE ActivityLog (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "REMOTE_SITE_ID INTEGER,ACTIVITY_ID TEXT NOT NULL,SUMMARY TEXT NOT NULL," +
                                "FORMATTABLE_CONTENT TEXT NOT NULL,NAME TEXT,TYPE TEXT,GRIDICON TEXT,STATUS TEXT," +
                                "REWINDABLE INTEGER,REWIND_ID TEXT,PUBLISHED INTEGER,DISCARDED INTEGER," +
                                "DISPLAY_NAME TEXT,ACTOR_TYPE TEXT,WPCOM_USER_ID INTEGER,AVATAR_URL TEXT,ROLE TEXT)"
                    )
                }
                41 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE LocalDiffModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "REVISION_ID INTEGER,POST_ID INTEGER,SITE_ID INTEGER,OPERATION TEXT,VALUE TEXT," +
                                "DIFF_TYPE TEXT)"
                    )
                    db.execSQL(
                            "CREATE TABLE LocalRevisionModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "REVISION_ID INTEGER,POST_ID INTEGER,SITE_ID INTEGER,DIFF_FROM_VERSION INTEGER," +
                                "TOTAL_ADDITIONS INTEGER,TOTAL_DELETIONS INTEGER,POST_CONTENT TEXT," +
                                "POST_EXCERPT TEXT,POST_TITLE TEXT,POST_DATE_GMT TEXT,POST_MODIFIED_GMT TEXT," +
                                "POST_AUTHOR_ID TEXT)"
                    )
                }
                42 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,TYPE TEXT NOT NULL,JSON TEXT NOT NULL)"
                    )
                }
                43 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE ListModel (LAST_MODIFIED TEXT," +
                                "DESCRIPTOR_UNIQUE_IDENTIFIER_DB_VALUE INTEGER," +
                                "DESCRIPTOR_TYPE_IDENTIFIER_DB_VALUE INTEGER,STATE_DB_VALUE INTEGER," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT)"
                    )
                    db.execSQL(
                            "CREATE TABLE ListItemModel (LIST_ID INTEGER,REMOTE_ITEM_ID INTEGER," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "FOREIGN KEY(LIST_ID) REFERENCES ListModel(_id) ON DELETE CASCADE," +
                                "UNIQUE(LIST_ID, REMOTE_ITEM_ID) ON CONFLICT IGNORE)"
                    )
                    db.execSQL("ALTER TABLE PostModel ADD LAST_MODIFIED TEXT")
                }
                44 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS StatsBlock")
                    db.execSQL(
                            "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,JSON TEXT NOT NULL)"
                    )
                }
                45 -> {
                    migrateAddOn(ADDON_WOOCOMMERCE, version) {
                        db.execSQL("ALTER TABLE WCOrderNoteModel ADD IS_SYSTEM_NOTE INTEGER")
                    }
                    migrate(version) {
                        db.execSQL(
                            "CREATE TABLE NotificationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "REMOTE_NOTE_ID INTEGER,LOCAL_SITE_ID INTEGER,NOTE_HASH INTEGER,TYPE TEXT," +
                                "SUBTYPE TEXT,READ INTEGER,ICON TEXT,NOTICON TEXT,TIMESTAMP TEXT,URL TEXT," +
                                "TITLE TEXT,FORMATTABLE_BODY TEXT,FORMATTABLE_SUBJECT TEXT,FORMATTABLE_META TEXT)"
                        )
                    }
                }
                46 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS StatsBlock")
                    db.execSQL(
                            "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT NOT NULL," +
                                "JSON TEXT NOT NULL)"
                    )
                }
                47 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS StatsBlock")
                    db.execSQL(
                            "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT NOT NULL," +
                                "JSON TEXT NOT NULL)"
                    )
                }
                48 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD REMOTE_LAST_MODIFIED TEXT")
                }
                49 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCSettingsModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,CURRENCY_CODE TEXT NOT NULL,CURRENCY_POSITION TEXT NOT NULL," +
                                "CURRENCY_THOUSAND_SEPARATOR TEXT NOT NULL,CURRENCY_DECIMAL_SEPARATOR TEXT NOT NULL," +
                                "CURRENCY_DECIMAL_NUMBER INTEGER)"
                    )
                }
                50 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE PlanOffers (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "INTERNAL_PLAN_ID INTEGER,NAME TEXT,SHORT_NAME TEXT,TAGLINE TEXT," +
                                "DESCRIPTION TEXT,ICON TEXT)"
                    )
                    db.execSQL(
                            "CREATE TABLE PlanOffersId (_id INTEGER PRIMARY KEY AUTOINCREMENT,PRODUCT_ID INTEGER," +
                                    "INTERNAL_PLAN_ID INTEGER)"
                    )
                    db.execSQL(
                            "CREATE TABLE PlanOffersFeature (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "INTERNAL_PLAN_ID INTEGER,STRING_ID TEXT UNIQUE,NAME TEXT,DESCRIPTION TEXT)"
                    )
                }
                51 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("CREATE TABLE WCOrderStatusModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "LOCAL_SITE_ID INTEGER,STATUS_KEY TEXT NOT NULL,LABEL TEXT NOT NULL)"
                    )
                }
                52 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE PlanOffersFeatureTemp (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "INTERNAL_PLAN_ID INTEGER,STRING_ID TEXT,NAME TEXT,DESCRIPTION TEXT)"
                    )
                    db.execSQL("INSERT INTO PlanOffersFeatureTemp SELECT * FROM PlanOffersFeature")
                    db.execSQL("DROP TABLE PlanOffersFeature")
                    db.execSQL("ALTER TABLE PlanOffersFeatureTemp RENAME TO PlanOffersFeature")
                }
                53 -> migrate(version) {
                    db.execSQL("ALTER TABLE QuickStartTaskModel ADD TASK_TYPE TEXT")
                }
                54 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD IS_CUSTOM_FIELD INTEGER")
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD DATE TEXT")
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD ENDDATE TEXT")
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD STARTDATE TEXT")
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD QUANTITY TEXT")
                }
                55 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCOrderStatsModel")
                    db.execSQL(
                            "CREATE TABLE WCOrderStatsModel(" +
                                "LOCAL_SITE_ID INTEGER," +
                                "UNIT TEXT NOT NULL," +
                                "DATE TEXT NOT NULL," +
                                "START_DATE TEXT NOT NULL," +
                                "END_DATE TEXT NOT NULL," +
                                "QUANTITY TEXT NOT NULL," +
                                "IS_CUSTOM_FIELD INTEGER," +
                                "FIELDS TEXT NOT NULL," +
                                "DATA TEXT NOT NULL," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT)"
                    )
                }
                56 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCProductModel")
                    db.execSQL(
                            "CREATE TABLE WCProductModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_PRODUCT_ID INTEGER," +
                                "NAME TEXT NOT NULL,SLUG TEXT NOT NULL,PERMALINK TEXT NOT NULL," +
                                "DATE_CREATED TEXT NOT NULL,DATE_MODIFIED TEXT NOT NULL," +
                                "TYPE TEXT NOT NULL,STATUS TEXT NOT NULL,FEATURED INTEGER," +
                                "CATALOG_VISIBILITY TEXT NOT NULL,DESCRIPTION TEXT NOT NULL," +
                                "SHORT_DESCRIPTION TEXT NOT NULL,SKU TEXT NOT NULL," +
                                "PRICE TEXT NOT NULL,REGULAR_PRICE TEXT NOT NULL, SALE_PRICE TEXT NOT NULL," +
                                "ON_SALE INTEGER,TOTAL_SALES INTEGER,VIRTUAL INTEGER,DOWNLOADABLE INTEGER," +
                                "TAX_STATUS TEXT NOT NULL,TAX_CLASS TEXT NOT NULL," +
                                "MANAGE_STOCK INTEGER,STOCK_QUANTITY INTEGER,STOCK_STATUS TEXT NOT NULL," +
                                "BACKORDERS TEXT NOT NULL,BACKORDERS_ALLOWED INTEGER,BACKORDERED INTEGER," +
                                "SOLD_INDIVIDUALLY INTEGER,WEIGHT TEXT NOT NULL,LENGTH TEXT NOT NULL," +
                                "WIDTH TEXT NOT NULL,HEIGHT TEXT NOT NULL,SHIPPING_REQUIRED INTEGER," +
                                "SHIPPING_TAXABLE INTEGER,SHIPPING_CLASS TEXT NOT NULL," +
                                "SHIPPING_CLASS_ID INTEGER,REVIEWS_ALLOWED INTEGER,AVERAGE_RATING TEXT NOT NULL," +
                                "RATING_COUNT INTEGER,PARENT_ID INTEGER,PURCHASE_NOTE TEXT NOT NULL," +
                                "CATEGORIES TEXT NOT NULL,TAGS TEXT NOT NULL," +
                                "IMAGES TEXT NOT NULL,ATTRIBUTES TEXT NOT NULL," +
                                "VARIATIONS TEXT NOT NULL)"
                    )
                }
                57 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DELETE FROM WCOrderStatsModel")
                }
                58 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCProductVariationModel")
                    db.execSQL(
                            "CREATE TABLE WCProductVariationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER," +
                                "REMOTE_PRODUCT_ID INTEGER," +
                                "REMOTE_VARIATION_ID INTEGER," +
                                "DATE_CREATED TEXT NOT NULL," +
                                "DATE_MODIFIED TEXT NOT NULL," +
                                "DESCRIPTION TEXT NOT NULL," +
                                "PERMALINK TEXT NOT NULL," +
                                "SKU TEXT NOT NULL," +
                                "STATUS TEXT NOT NULL," +
                                "PRICE TEXT NOT NULL," +
                                "REGULAR_PRICE TEXT NOT NULL," +
                                "SALE_PRICE TEXT NOT NULL," +
                                "ON_SALE INTEGER," +
                                "PURCHASABLE INTEGER," +
                                "VIRTUAL INTEGER," +
                                "DOWNLOADABLE INTEGER," +
                                "MANAGE_STOCK INTEGER," +
                                "STOCK_QUANTITY INTEGER," +
                                "STOCK_STATUS TEXT NOT NULL," +
                                "IMAGE_URL TEXT NOT NULL," +
                                "WEIGHT TEXT NOT NULL," +
                                "LENGTH TEXT NOT NULL," +
                                "WIDTH TEXT NOT NULL," +
                                "HEIGHT TEXT NOT NULL," +
                                "ATTRIBUTES TEXT NOT NULL)"
                    )
                }
                59 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS StatsBlock")
                    db.execSQL(
                            "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT,POST_ID INTEGER," +
                                "JSON TEXT NOT NULL)"
                    )
                }
                60 -> migrate(version) {
                    db.execSQL("DROP TABLE StatsBlock")
                    db.execSQL(
                            "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER," +
                                "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT,POST_ID INTEGER," +
                                "JSON TEXT NOT NULL)"
                    )
                    db.execSQL(
                            "CREATE TABLE StatsRequest (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL," +
                                "DATE TEXT,TIME_STAMP INTEGER,REQUESTED_ITEMS INTEGER)"
                    )
                }
                61 -> {
                    migrateAddOn(ADDON_WOOCOMMERCE, version) {
                        db.execSQL("DROP TABLE IF EXISTS WCProductModel")
                        db.execSQL(
                                "CREATE TABLE WCProductModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_PRODUCT_ID INTEGER," +
                                    "NAME TEXT NOT NULL," +
                                    "SLUG TEXT NOT NULL," +
                                    "PERMALINK TEXT NOT NULL," +
                                    "DATE_CREATED TEXT NOT NULL," +
                                    "DATE_MODIFIED TEXT NOT NULL," +
                                    "TYPE TEXT NOT NULL," +
                                    "STATUS TEXT NOT NULL," +
                                    "FEATURED INTEGER," +
                                    "CATALOG_VISIBILITY TEXT NOT NULL," +
                                    "DESCRIPTION TEXT NOT NULL," +
                                    "SHORT_DESCRIPTION TEXT NOT NULL," +
                                    "SKU TEXT NOT NULL," +
                                    "PRICE TEXT NOT NULL," +
                                    "REGULAR_PRICE TEXT NOT NULL," +
                                    "SALE_PRICE TEXT NOT NULL," +
                                    "ON_SALE INTEGER," +
                                    "TOTAL_SALES INTEGER," +
                                    "VIRTUAL INTEGER," +
                                    "DOWNLOADABLE INTEGER," +
                                    "DOWNLOAD_LIMIT INTEGER," +
                                    "DOWNLOAD_EXPIRY INTEGER," +
                                    "DOWNLOADS TEXT NOT NULL," +
                                    "EXTERNAL_URL TEXT NOT NULL," +
                                    "TAX_STATUS TEXT NOT NULL," +
                                    "TAX_CLASS TEXT NOT NULL," +
                                    "MANAGE_STOCK INTEGER," +
                                    "STOCK_QUANTITY INTEGER," +
                                    "STOCK_STATUS TEXT NOT NULL," +
                                    "BACKORDERS TEXT NOT NULL," +
                                    "BACKORDERS_ALLOWED INTEGER," +
                                    "BACKORDERED INTEGER," +
                                    "SOLD_INDIVIDUALLY INTEGER," +
                                    "WEIGHT TEXT NOT NULL," +
                                    "LENGTH TEXT NOT NULL," +
                                    "WIDTH TEXT NOT NULL," +
                                    "HEIGHT TEXT NOT NULL," +
                                    "SHIPPING_REQUIRED INTEGER," +
                                    "SHIPPING_TAXABLE INTEGER," +
                                    "SHIPPING_CLASS TEXT NOT NULL," +
                                    "SHIPPING_CLASS_ID INTEGER," +
                                    "REVIEWS_ALLOWED INTEGER," +
                                    "AVERAGE_RATING TEXT NOT NULL," +
                                    "RATING_COUNT INTEGER," +
                                    "PARENT_ID INTEGER," +
                                    "PURCHASE_NOTE TEXT NOT NULL," +
                                    "CATEGORIES TEXT NOT NULL," +
                                    "TAGS TEXT NOT NULL," +
                                    "IMAGES TEXT NOT NULL," +
                                    "ATTRIBUTES TEXT NOT NULL," +
                                    "RELATED_IDS TEXT NOT NULL," +
                                    "CROSS_SELL_IDS TEXT NOT NULL," +
                                    "UPSELL_IDS TEXT NOT NULL," +
                                    "VARIATIONS TEXT NOT NULL)"
                        )
                    }
                    migrate(version) {
                        db.execSQL("DROP TABLE StatsRequest")
                        db.execSQL(
                                "CREATE TABLE StatsRequest (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER,BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL," +
                                    "DATE TEXT,POST_ID INTEGER,TIME_STAMP INTEGER,REQUESTED_ITEMS INTEGER)"
                        )
                    }
                }
                62 -> migrate(version) {
                    db.execSQL("DROP TABLE StatsRequest")
                    db.execSQL(
                            "CREATE TABLE StatsRequest (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL," +
                                "DATE TEXT,POST_ID INTEGER,TIME_STAMP INTEGER,REQUESTED_ITEMS INTEGER)"
                    )
                }
                63 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCProductSettingsModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER," +
                                "WEIGHT_UNIT TEXT NOT NULL," +
                                "DIMENSION_UNIT TEXT NOT NULL)"
                    )
                }
                64 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCOrderShipmentTrackingModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER," +
                                "LOCAL_ORDER_ID INTEGER," +
                                "REMOTE_TRACKING_ID TEXT NOT NULL," +
                                "TRACKING_NUMBER TEXT NOT NULL," +
                                "TRACKING_PROVIDER TEXT NOT NULL," +
                                "TRACKING_LINK TEXT NOT NULL," +
                                "DATE_SHIPPED TEXT NOT NULL)"
                    )
                }
                65 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCOrderShipmentProviderModel (" +
                                "LOCAL_SITE_ID INTEGER," +
                                "COUNTRY TEXT NOT NULL," +
                                "CARRIER_NAME TEXT NOT NULL," +
                                "CARRIER_LINK TEXT NOT NULL," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT)"
                    )
                }
                66 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE InsightTypes (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER,INSIGHT_TYPE TEXT NOT NULL," +
                                "POSITION INTEGER,STATUS TEXT NOT NULL)"
                    )
                }
                67 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCSettingsModel ADD COUNTRY_CODE TEXT")
                }
                68 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS NotificationModel")
                    db.execSQL(
                            "CREATE TABLE NotificationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "REMOTE_NOTE_ID INTEGER,LOCAL_SITE_ID INTEGER,NOTE_HASH INTEGER,TYPE TEXT," +
                                "SUBTYPE TEXT,READ INTEGER,ICON TEXT,NOTICON TEXT,TIMESTAMP TEXT,URL TEXT," +
                                "TITLE TEXT,FORMATTABLE_BODY TEXT,FORMATTABLE_SUBJECT TEXT,FORMATTABLE_META TEXT," +
                                "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE)"
                    )
                }
                69 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderModel ADD DATE_MODIFIED TEXT")
                    db.execSQL(
                            "CREATE TABLE WCOrderSummaryModel (LOCAL_SITE_ID INTEGER,REMOTE_ORDER_ID INTEGER," +
                                "DATE_CREATED TEXT NOT NULL,_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE," +
                                "UNIQUE (REMOTE_ORDER_ID, LOCAL_SITE_ID) ON CONFLICT REPLACE)"
                    )
                }
                70 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS NotificationModel")
                    db.execSQL(
                            "CREATE TABLE NotificationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "REMOTE_NOTE_ID INTEGER,LOCAL_SITE_ID INTEGER,NOTE_HASH INTEGER,TYPE TEXT," +
                                "SUBTYPE TEXT,READ INTEGER,ICON TEXT,NOTICON TEXT,TIMESTAMP TEXT,URL TEXT," +
                                "TITLE TEXT,FORMATTABLE_BODY TEXT,FORMATTABLE_SUBJECT TEXT,FORMATTABLE_META TEXT)"
                    )
                }
                71 -> migrate(version) {
                    db.execSQL("ALTER TABLE MediaModel ADD MARKED_LOCALLY_AS_FEATURED INTEGER")
                }
                72 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostUploadModel ADD NUMBER_OF_UPLOAD_ERRORS_OR_CANCELLATIONS INTEGER")
                }
                73 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS NotificationModel")
                    db.execSQL(
                            "CREATE TABLE NotificationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "REMOTE_NOTE_ID INTEGER,REMOTE_SITE_ID INTEGER,NOTE_HASH INTEGER,TYPE TEXT," +
                                "SUBTYPE TEXT,READ INTEGER,ICON TEXT,NOTICON TEXT,TIMESTAMP TEXT,URL TEXT," +
                                "TITLE TEXT,FORMATTABLE_BODY TEXT,FORMATTABLE_SUBJECT TEXT,FORMATTABLE_META TEXT)"
                    )
                }
                74 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD WEB_EDITOR TEXT")
                    db.execSQL("ALTER TABLE SiteModel ADD MOBILE_EDITOR TEXT")
                }
                75 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCRevenueStatsModel(" +
                                "LOCAL_SITE_ID INTEGER," +
                                "INTERVAL TEXT NOT NULL," +
                                "START_DATE TEXT NOT NULL," +
                                "END_DATE TEXT NOT NULL," +
                                "DATA TEXT NOT NULL," +
                                "TOTAL TEXT NOT NULL," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT)"
                    )
                }
                76 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE PostSchedulingReminder (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "POST_ID INTEGER,SCHEDULED_TIME TEXT NOT NULL)"
                    )
                }
                77 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD AUTHOR_ID INTEGER")
                    db.execSQL("ALTER TABLE PostModel ADD AUTHOR_DISPLAY_NAME TEXT")
                }
                78 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCVisitorStatsModel(" +
                                "LOCAL_SITE_ID INTEGER," +
                                "UNIT TEXT NOT NULL," +
                                "DATE TEXT NOT NULL," +
                                "START_DATE TEXT NOT NULL," +
                                "END_DATE TEXT NOT NULL," +
                                "QUANTITY TEXT NOT NULL," +
                                "IS_CUSTOM_FIELD INTEGER," +
                                "FIELDS TEXT NOT NULL," +
                                "DATA TEXT NOT NULL," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT)"
                    )
                }
                79 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD CHANGES_CONFIRMED_CONTENT_HASHCODE INTEGER")
                }
                80 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCNewVisitorStatsModel(" +
                                "LOCAL_SITE_ID INTEGER," +
                                "GRANULARITY TEXT NOT NULL," +
                                "DATE TEXT NOT NULL," +
                                "START_DATE TEXT NOT NULL," +
                                "END_DATE TEXT NOT NULL," +
                                "QUANTITY TEXT NOT NULL," +
                                "IS_CUSTOM_FIELD INTEGER," +
                                "FIELDS TEXT NOT NULL," +
                                "DATA TEXT NOT NULL," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT)"
                    )
                }
                81 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCProductReviewModel (" +
                                "LOCAL_SITE_ID INTEGER," +
                                "REMOTE_PRODUCT_REVIEW_ID INTEGER," +
                                "REMOTE_PRODUCT_ID INTEGER," +
                                "DATE_CREATED TEXT NOT NULL," +
                                "STATUS TEXT NOT NULL," +
                                "REVIEWER_NAME TEXT NOT NULL," +
                                "REVIEWER_EMAIL TEXT NOT NULL," +
                                "REVIEW TEXT NOT NULL," +
                                "RATING INTEGER," +
                                "VERIFIED INTEGER," +
                                "REVIEWER_AVATARS_JSON TEXT NOT NULL," +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE," +
                                "UNIQUE (REMOTE_PRODUCT_REVIEW_ID, REMOTE_PRODUCT_ID, LOCAL_SITE_ID) " +
                                "ON CONFLICT REPLACE)"
                    )
                }
                82 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderModel ADD COLUMN DATE_PAID TEXT NOT NULL DEFAULT ''")
                }
                83 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderStatusModel ADD STATUS_COUNT INTEGER")
                }
                84 -> migrate(version) {
                    db.execSQL("ALTER TABLE AccountModel ADD USERNAME_CAN_BE_CHANGED BOOLEAN")
                }
                85 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_REVISION_ID INTEGER")
                    db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_MODIFIED TEXT")
                    db.execSQL("ALTER TABLE PostModel ADD REMOTE_AUTO_SAVE_MODIFIED TEXT")
                    db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_PREVIEW_URL TEXT")
                    db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_TITLE TEXT")
                    db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_CONTENT TEXT")
                    db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_EXCERPT TEXT")
                }
                86 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostUploadModel ADD NUMBER_OF_AUTO_UPLOAD_ATTEMPTS INTEGER")
                }
                87 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCRefunds (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER," +
                                "ORDER_ID INTEGER," +
                                "REFUND_ID INTEGER," +
                                "DATA TEXT NOT NULL)"
                    )
                }
                88 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderNoteModel ADD AUTHOR TEXT")
                }
                89 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD JETPACK_USER_EMAIL TEXT")
                }
                90 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCGateways (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "GATEWAY_ID TEXT NOT NULL," +
                                    "DATA TEXT NOT NULL)"
                    )
                }
                91 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderModel ADD SHIPPING_LINES TEXT")
                }
                92 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD DATE_ON_SALE_FROM TEXT")
                    db.execSQL("ALTER TABLE WCProductModel ADD DATE_ON_SALE_TO TEXT")
                }
                93 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductVariationModel ADD MENU_ORDER INTEGER")
                }
                94 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCProductShippingClassModel(" +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_SHIPPING_CLASS_ID INTEGER," +
                                    "NAME TEXT NOT NULL," +
                                    "SLUG TEXT NOT NULL," +
                                    "DESCRIPTION TEXT NOT NULL," +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE," +
                                    "UNIQUE (REMOTE_SHIPPING_CLASS_ID, LOCAL_SITE_ID) " +
                                    "ON CONFLICT REPLACE)"
                    )
                }
                95 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCTaxClassModel(" +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "NAME TEXT NOT NULL," +
                                    "SLUG TEXT NOT NULL," +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE," +
                                    "UNIQUE (SLUG, LOCAL_SITE_ID) " +
                                    "ON CONFLICT REPLACE)"
                    )
                }
                96 -> migrate(version) {
                    db.execSQL("CREATE TABLE PostSummaryModel (REMOTE_ID INTEGER,LOCAL_SITE_ID INTEGER,STATUS TEXT," +
                            "DATE_CREATED TEXT,_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE," +
                            "UNIQUE(REMOTE_ID) ON CONFLICT REPLACE)")
                }
                97 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD DATE_ON_SALE_FROM_GMT TEXT")
                    db.execSQL("ALTER TABLE WCProductModel ADD DATE_ON_SALE_TO_GMT TEXT")
                }
                98 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS PostSummaryModel")
                }
                99 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD WP_API_REST_URL TEXT")
                }
                100 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD IS_WPCOM_ATOMIC BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_COMING_SOON BOOLEAN")
                }
                101 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD MENU_ORDER INTEGER")
                }
                102 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD BUTTON_TEXT TEXT")
                }
                103 -> migrate(version) {
                    db.execSQL("ALTER TABLE CommentModel ADD URL TEXT")
                }
                104 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCShippingLabelModel (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "LOCAL_ORDER_ID INTEGER," +
                                    "REMOTE_SHIPPING_LABEL_ID INTEGER," +
                                    "CARRIER_ID TEXT NOT NULL," +
                                    "PRODUCT_NAMES TEXT NULL," +
                                    "TRACKING_NUMBER TEXT NOT NULL," +
                                    "SERVICE_NAME TEXT NOT NULL," +
                                    "STATUS TEXT NOT NULL," +
                                    "PACKAGE_NAME TEXT NOT NULL," +
                                    "RATE REAL NOT NULL," +
                                    "REFUNDABLE_AMOUNT REAL NOT NULL," +
                                    "CURRENCY TEXT NOT NULL," +
                                    "PAPER_SIZE TEXT NOT NULL," +
                                    "FORM_DATA TEXT NOT NULL," +
                                    "STORE_OPTIONS TEXT NOT NULL," +
                                    "REFUND TEXT NULL)"
                    )
                }
                105 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCProductCategoryModel (" +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_CATEGORY_ID INTEGER," +
                                    "NAME TEXT NOT NULL," +
                                    "SLUG TEXT NOT NULL," +
                                    "PARENT INTEGER," +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE," +
                                    "UNIQUE (REMOTE_CATEGORY_ID, LOCAL_SITE_ID) " +
                                    "ON CONFLICT REPLACE)"
                    )
                }
                106 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD SHOW_ON_FRONT TEXT")
                    db.execSQL("ALTER TABLE SiteModel ADD PAGE_ON_FRONT INTEGER")
                    db.execSQL("ALTER TABLE SiteModel ADD PAGE_FOR_POSTS INTEGER")
                }
                107 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCProductVariationModel")
                    db.execSQL("CREATE TABLE WCProductVariationModel (" +
                            "LOCAL_SITE_ID INTEGER," +
                            "REMOTE_PRODUCT_ID INTEGER," +
                            "REMOTE_VARIATION_ID INTEGER," +
                            "DATE_CREATED TEXT NOT NULL," +
                            "DATE_MODIFIED TEXT NOT NULL," +
                            "DESCRIPTION TEXT NOT NULL," +
                            "PERMALINK TEXT NOT NULL," +
                            "SKU TEXT NOT NULL," +
                            "STATUS TEXT NOT NULL," +
                            "PRICE TEXT NOT NULL," +
                            "REGULAR_PRICE TEXT NOT NULL," +
                            "SALE_PRICE TEXT NOT NULL," +
                            "DATE_ON_SALE_FROM TEXT NOT NULL," +
                            "DATE_ON_SALE_TO TEXT NOT NULL," +
                            "DATE_ON_SALE_FROM_GMT TEXT NOT NULL," +
                            "DATE_ON_SALE_TO_GMT TEXT NOT NULL," +
                            "ON_SALE INTEGER," +
                            "PURCHASABLE INTEGER," +
                            "VIRTUAL INTEGER," +
                            "DOWNLOADABLE INTEGER," +
                            "MANAGE_STOCK INTEGER," +
                            "STOCK_QUANTITY INTEGER," +
                            "STOCK_STATUS TEXT NOT NULL," +
                            "IMAGE TEXT NOT NULL," +
                            "WEIGHT TEXT NOT NULL," +
                            "LENGTH TEXT NOT NULL," +
                            "WIDTH TEXT NOT NULL," +
                            "HEIGHT TEXT NOT NULL," +
                            "MENU_ORDER INTEGER," +
                            "ATTRIBUTES TEXT NOT NULL," +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT)")
                }
                108 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCProductVariationModel")
                    db.execSQL("CREATE TABLE WCProductVariationModel (" +
                            "LOCAL_SITE_ID INTEGER," +
                            "REMOTE_PRODUCT_ID INTEGER," +
                            "REMOTE_VARIATION_ID INTEGER," +
                            "DATE_CREATED TEXT NOT NULL," +
                            "DATE_MODIFIED TEXT NOT NULL," +
                            "DESCRIPTION TEXT NOT NULL," +
                            "PERMALINK TEXT NOT NULL," +
                            "SKU TEXT NOT NULL," +
                            "STATUS TEXT NOT NULL," +
                            "PRICE TEXT NOT NULL," +
                            "REGULAR_PRICE TEXT NOT NULL," +
                            "SALE_PRICE TEXT NOT NULL," +
                            "DATE_ON_SALE_FROM TEXT NOT NULL," +
                            "DATE_ON_SALE_TO TEXT NOT NULL," +
                            "DATE_ON_SALE_FROM_GMT TEXT NOT NULL," +
                            "DATE_ON_SALE_TO_GMT TEXT NOT NULL," +
                            "ON_SALE INTEGER," +
                            "PURCHASABLE INTEGER," +
                            "VIRTUAL INTEGER," +
                            "DOWNLOADABLE INTEGER," +
                            "TAX_STATUS TEXT NOT NULL," +
                            "TAX_CLASS TEXT NOT NULL," +
                            "DOWNLOAD_LIMIT INTEGER," +
                            "DOWNLOAD_EXPIRY INTEGER," +
                            "DOWNLOADS TEXT NOT NULL," +
                            "BACKORDERS TEXT NOT NULL," +
                            "BACKORDERS_ALLOWED INTEGER," +
                            "BACKORDERED INTEGER," +
                            "SHIPPING_CLASS TEXT NOT NULL," +
                            "SHIPPING_CLASS_ID INTEGER," +
                            "MANAGE_STOCK INTEGER," +
                            "STOCK_QUANTITY INTEGER," +
                            "STOCK_STATUS TEXT NOT NULL," +
                            "IMAGE TEXT NOT NULL," +
                            "WEIGHT TEXT NOT NULL," +
                            "LENGTH TEXT NOT NULL," +
                            "WIDTH TEXT NOT NULL," +
                            "HEIGHT TEXT NOT NULL," +
                            "MENU_ORDER INTEGER," +
                            "ATTRIBUTES TEXT NOT NULL," +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT)")
                }
                109 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE EditorTheme(" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "STYLESHEET TEXT," +
                                    "VERSION TEXT," +
                                    "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE)"
                    )
                    db.execSQL(
                            "CREATE TABLE EditorThemeElement(" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "THEME_ID INTEGER," +
                                    "TYPE TEXT NOT NULL," +
                                    "NAME TEXT NOT NULL," +
                                    "SLUG TEXT NOT NULL," +
                                    "VALUE TEXT NOT NULL," +
                                    "CHECK(TYPE IN (\"color\", \"gradient\") )," +
                                    "FOREIGN KEY(THEME_ID) REFERENCES EditorTheme(_id) ON DELETE CASCADE)"
                    )
                }
                110 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS WhatsNewAnnouncement")
                    db.execSQL("DROP TABLE IF EXISTS WhatsNewAnnouncementFeature")
                    db.execSQL(
                            "CREATE TABLE WhatsNewAnnouncement (_announcement_id INTEGER PRIMARY KEY," +
                                    "APP_VERSION_NAME TEXT NOT NULL,MINIMUM_APP_VERSION TEXT NOT NULL," +
                                    "MAXIMUM_APP_VERSION TEXT NOT NULL,LOCALIZED INTEGER," +
                                    "RESPONSE_LOCALE TEXT NOT NULL,DETAILS_URL TEXT)"
                    )
                    db.execSQL(
                            "CREATE TABLE WhatsNewAnnouncementFeature (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "ANNOUNCEMENT_ID INTEGER,TITLE TEXT,SUBTITLE TEXT,ICON_URL TEXT,ICON_BASE64 TEXT)"
                    )
                }
                111 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE WCProductTagModel (" +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_TAG_ID INTEGER," +
                                    "NAME TEXT NOT NULL," +
                                    "SLUG TEXT NOT NULL," +
                                    "DESCRIPTION TEXT," +
                                    "COUNT INTEGER," +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE," +
                                    "UNIQUE (REMOTE_TAG_ID, LOCAL_SITE_ID) " +
                                    "ON CONFLICT REPLACE)"
                    )
                }
                112 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS WhatsNewAnnouncement")
                    db.execSQL(
                            "CREATE TABLE WhatsNewAnnouncement (_announcement_id INTEGER PRIMARY KEY," +
                                    "APP_VERSION_NAME TEXT NOT NULL,MINIMUM_APP_VERSION TEXT NOT NULL," +
                                    "MAXIMUM_APP_VERSION TEXT NOT NULL,APP_VERSION_TARGETS TEXT NOT NULL," +
                                    "LOCALIZED INTEGER,RESPONSE_LOCALE TEXT NOT NULL,DETAILS_URL TEXT)"
                    )
                }
                113 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCShippingLabelModel ADD DATE_CREATED TEXT")
                }
                114 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD GROUPED_PRODUCT_IDS TEXT")
                }
                115 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE EncryptedLogModel (UUID TEXT,FILE_PATH TEXT,DATE_CREATED TEXT," +
                                    "UPLOAD_STATE_DB_VALUE INTEGER,FAILED_COUNT INTEGER," +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT,UNIQUE(UUID) ON CONFLICT REPLACE)"
                    )
                }
                116 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCTopPerformerProductModel")
                    db.execSQL("CREATE TABLE WCTopPerformerProductModel (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "LOCAL_SITE_ID INTEGER," +
                            "PRODUCT_INFO TEXT, " +
                            "CURRENCY TEXT, " +
                            "QUANTITY INTEGER, " +
                            "UNIT TEXT," +
                            "TOTAL REAL)")
                }
                117 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS StockMedia")
                    db.execSQL("CREATE TABLE StockMedia (_id INTEGER PRIMARY KEY AUTOINCREMENT,ITEM_ID TEXT," +
                            "NAME TEXT,TITLE TEXT,URL TEXT,DATE TEXT,THUMBNAIL TEXT)")
                    db.execSQL("DROP TABLE IF EXISTS StockMediaPage")
                    db.execSQL("CREATE TABLE StockMediaPage (_id INTEGER PRIMARY KEY AUTOINCREMENT,PAGE INTEGER," +
                            "NEXT_PAGE INTEGER)")
                }
                118 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS GutenbergLayoutCategoryModel")
                    db.execSQL("CREATE TABLE GutenbergLayoutCategoryModel (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "SLUG TEXT NOT NULL," +
                            "SITE_ID INTEGER," +
                            "TITLE TEXT NOT NULL," +
                            "DESCRIPTION TEXT NOT NULL," +
                            "EMOJI TEXT NOT NULL)")
                    db.execSQL("DROP TABLE IF EXISTS GutenbergLayoutModel")
                    db.execSQL("CREATE TABLE GutenbergLayoutModel (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "SLUG TEXT NOT NULL," +
                            "SITE_ID INTEGER," +
                            "TITLE TEXT NOT NULL," +
                            "PREVIEW TEXT NOT NULL," +
                            "CONTENT TEXT NOT NULL)")
                    db.execSQL("DROP TABLE IF EXISTS GutenbergLayoutCategoriesModel")
                    db.execSQL("CREATE TABLE GutenbergLayoutCategoriesModel (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "LAYOUT_ID INTEGER," +
                            "CATEGORY_ID INTEGER," +
                            "SITE_ID INTEGER)")
                }
                119 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DELETE FROM WCOrderModel")
                }
                120 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS StatsBlock")
                    db.execSQL("CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "LOCAL_SITE_ID INTEGER,BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT," +
                            "POST_ID INTEGER,JSON TEXT NOT NULL)")
                }
                121 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCPlugins")
                    db.execSQL("CREATE TABLE WCPlugins (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "LOCAL_SITE_ID INTEGER," +
                            "ACTIVE BOOLEAN NOT NULL," +
                            "DISPLAY_NAME TEXT NOT NULL," +
                            "SLUG TEXT NOT NULL," +
                            "VERSION TEXT NOT NULL)"
                    )
                }
                122 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD IS_WP_FOR_TEAMS_SITE BOOLEAN")
                }
                123 -> migrate(version) {
                    db.execSQL(
                        "CREATE TABLE ScanState (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER," +
                            "START_DATE INTEGER," +
                            "DURATION INTEGER," +
                            "PROGRESS INTEGER," +
                            "SCAN_STATE TEXT," +
                            "ERROR TEXT," +
                            "INITIAL BOOLEAN," +
                            "REASON TEXT)"
                    )
                }
                124 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderModel ADD FEE_LINES TEXT")
                }
                125 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCShippingLabelModel ADD PRODUCT_IDS TEXT")
                }
                126 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCLocations")
                    db.execSQL("CREATE TABLE WCLocations (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "PARENT_CODE TEXT NOT NULL," +
                            "CODE TEXT NOT NULL," +
                            "NAME TEXT NOT NULL)"
                    )
                }
                127 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS ScanState")
                    db.execSQL(
                            "CREATE TABLE ScanState (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER, REMOTE_SITE_ID INTEGER," +
                                    "START_DATE INTEGER," +
                                    "DURATION INTEGER NOT NULL," +
                                    "PROGRESS INTEGER NOT NULL," +
                                    "STATE TEXT NOT NULL," +
                                    "ERROR BOOLEAN NOT NULL," +
                                    "INITIAL BOOLEAN NOT NULL," +
                                    "REASON TEXT, " +
                                    "HAS_CLOUD BOOLEAN NOT NULL)"
                    )
                }
                128 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS ThreatModel")
                    db.execSQL(
                        "CREATE TABLE ThreatModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "LOCAL_SITE_ID INTEGER, REMOTE_SITE_ID INTEGER," +
                                "THREAT_ID INTEGER," +
                                "SIGNATURE TEXT NOT NULL," +
                                "DESCRIPTION TEXT NOT NULL," +
                                "STATUS TEXT NOT NULL," +
                                "FIRST_DETECTED INTEGER," +
                                "FIXED_ON INTEGER," +
                                "FIXABLE_FILE TEXT," +
                                "FIXABLE_FIXER TEXT," +
                                "FIXABLE_TARGET TEXT," +
                                "FILE_NAME TEXT," +
                                "DIFF TEXT," +
                                "EXTENSION TEXT," +
                                "ROWS TEXT," +
                                "CONTEXT TEXT)"
                    )
                }
                129 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE XPostSites (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "BLOG_ID INTEGER," +
                                    "TITLE TEXT," +
                                    "SITE_URL TEXT," +
                                    "SUBDOMAIN TEXT," +
                                    "BLAVATAR TEXT," +
                                    "UNIQUE (BLOG_ID) ON CONFLICT REPLACE)"
                    )
                    db.execSQL(
                            "CREATE TABLE XPosts (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "SOURCE_SITE_ID INTEGER," +
                                    "TARGET_SITE_ID INTEGER," +
                                    "FOREIGN KEY(SOURCE_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE," +
                                    "FOREIGN KEY(TARGET_SITE_ID) REFERENCES XPostSites(BLOG_ID)," +
                                    "UNIQUE (SOURCE_SITE_ID, TARGET_SITE_ID) ON CONFLICT IGNORE)"
                    )
                }
                130 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS BackupDownloadStatus")
                    db.execSQL(
                            "CREATE TABLE BackupDownloadStatus (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_SITE_ID INTEGER," +
                                    "DOWNLOAD_ID INTEGER," +
                                    "REWIND_ID TEXT NOT NULL," +
                                    "BACKUP_POINT INTEGER," +
                                    "STARTED_AT INTEGER," +
                                    "PROGRESS INTEGER," +
                                    "DOWNLOAD_COUNT INTEGER," +
                                    "VALID_UNTIL INTEGER," +
                                    "URL TEXT)"
                    )
                }
                131 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS RewindStatus")
                    db.execSQL(
                            "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER,REMOTE_SITE_ID INTEGER,STATE TEXT NOT NULL," +
                                    "LAST_UPDATED INTEGER,REASON TEXT,CAN_AUTOCONFIGURE INTEGER,REWIND_ID TEXT," +
                                    "RESTORE_ID INTEGER,REWIND_STATUS TEXT,REWIND_PROGRESS INTEGER," +
                                    "REWIND_REASON TEXT,MESSAGE TEXT,CURRENT_ENTRY TEXT)"
                    )
                }
                132 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCProductAttributeModel")
                    db.execSQL(
                            "CREATE TABLE WCProductAttributeModel (" +
                                    "_id INTEGER PRIMARY KEY, " +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "NAME TEXT NOT NULL," +
                                    "SLUG TEXT, " +
                                    "TYPE TEXT, " +
                                    "ORDER_BY TEXT, " +
                                    "HAS_ARCHIVES BOOLEAN NOT NULL)"
                    )
                }
                133 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCSettingsModel ADD ADDRESS TEXT")
                    db.execSQL("ALTER TABLE WCSettingsModel ADD ADDRESS2 TEXT")
                    db.execSQL("ALTER TABLE WCSettingsModel ADD CITY TEXT")
                    db.execSQL("ALTER TABLE WCSettingsModel ADD POSTAL_CODE TEXT")
                    db.execSQL("ALTER TABLE WCSettingsModel ADD STATE_CODE TEXT")
                }
                134 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCAttributeTermModel")
                    db.execSQL("ALTER TABLE WCProductAttributeModel ADD TERMS TEXT")
                    db.execSQL("ALTER TABLE WCProductAttributeModel ADD REMOTE_ID INTEGER")
                    db.execSQL(
                            "CREATE TABLE WCAttributeTermModel (" +
                                    "_id INTEGER PRIMARY KEY, " +
                                    "REMOTE_ID INTEGER," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "ATTRIBUTE_ID INTEGER," +
                                    "NAME TEXT NOT NULL," +
                                    "SLUG TEXT, " +
                                    "DESCRIPTION TEXT, " +
                                    "COUNT INTEGER, " +
                                    "MENU_ORDER INTEGER)"
                    )
                }
                135 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductAttributeModel RENAME TO WCGlobalAttributeModel")
                }
                136 -> migrate(version) {
                    db.execSQL("CREATE TABLE DynamicCard (_id INTEGER PRIMARY KEY AUTOINCREMENT,SITE_ID INTEGER," +
                            "DYNAMIC_CARD_TYPE TEXT,STATE TEXT)")
                }
                137 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS GutenbergLayoutModel")
                    db.execSQL(
                            "CREATE TABLE GutenbergLayoutModel (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "SLUG TEXT NOT NULL," +
                                    "SITE_ID INTEGER," +
                                    "TITLE TEXT NOT NULL," +
                                    "PREVIEW TEXT NOT NULL," +
                                    "PREVIEW_TABLET TEXT NOT NULL," +
                                    "PREVIEW_MOBILE TEXT NOT NULL," +
                                    "CONTENT TEXT NOT NULL," +
                                    "DEMO_URL TEXT NOT NULL)"
                    )
                }
                138 -> migrate(version) {
                    db.execSQL("ALTER TABLE CommentModel ADD PUBLISHED_TIMESTAMP INTEGER")
                }
                139 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCCustomerModel")
                    db.execSQL("CREATE TABLE WCCustomerModel (AVATAR_URL TEXT NOT NULL," +
                            "DATE_CREATED TEXT NOT NULL,DATE_CREATED_GMT TEXT NOT NULL,DATE_MODIFIED TEXT NOT NULL," +
                            "DATE_MODIFIED_GMT TEXT NOT NULL,EMAIL TEXT NOT NULL,FIRST_NAME TEXT NOT NULL," +
                            "REMOTE_CUSTOMER_ID INTEGER,IS_PAYING_CUSTOMER INTEGER,LAST_NAME TEXT NOT NULL," +
                            "ROLE TEXT NOT NULL,USERNAME TEXT NOT NULL,LOCAL_SITE_ID INTEGER,BILLING_ADDRESS1" +
                            " TEXT NOT NULL,BILLING_ADDRESS2 TEXT NOT NULL,BILLING_CITY TEXT NOT NULL," +
                            "BILLING_COMPANY TEXT NOT NULL,BILLING_COUNTRY TEXT NOT NULL,BILLING_EMAIL" +
                            " TEXT NOT NULL,BILLING_FIRST_NAME TEXT NOT NULL,BILLING_LAST_NAME TEXT " +
                            "NOT NULL,BILLING_PHONE TEXT NOT NULL,BILLING_POSTCODE TEXT NOT NULL,BILLING_STATE" +
                            " TEXT NOT NULL,SHIPPING_ADDRESS1 TEXT NOT NULL,SHIPPING_ADDRESS2 TEXT NOT NULL," +
                            "SHIPPING_CITY TEXT NOT NULL,SHIPPING_COMPANY TEXT NOT NULL,SHIPPING_COUNTRY TEXT" +
                            " NOT NULL,SHIPPING_FIRST_NAME TEXT NOT NULL,SHIPPING_LAST_NAME TEXT NOT NULL," +
                            "SHIPPING_POSTCODE TEXT NOT NULL,SHIPPING_STATE TEXT NOT NULL,_id INTEGER" +
                            " PRIMARY KEY AUTOINCREMENT)")
                }
                140 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCProductModel")
                    db.execSQL("CREATE TABLE WCProductModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "LOCAL_SITE_ID INTEGER," +
                            "REMOTE_PRODUCT_ID INTEGER," +
                            "NAME TEXT NOT NULL," +
                            "SLUG TEXT NOT NULL," +
                            "PERMALINK TEXT NOT NULL," +
                            "DATE_CREATED TEXT NOT NULL," +
                            "DATE_MODIFIED TEXT NOT NULL," +
                            "TYPE TEXT NOT NULL," +
                            "STATUS TEXT NOT NULL," +
                            "FEATURED INTEGER," +
                            "CATALOG_VISIBILITY TEXT NOT NULL," +
                            "DESCRIPTION TEXT NOT NULL," +
                            "SHORT_DESCRIPTION TEXT NOT NULL," +
                            "SKU TEXT NOT NULL," +
                            "PRICE TEXT NOT NULL," +
                            "REGULAR_PRICE TEXT NOT NULL," +
                            "SALE_PRICE TEXT NOT NULL," +
                            "ON_SALE INTEGER," +
                            "TOTAL_SALES INTEGER," +
                            "DATE_ON_SALE_FROM TEXT NOT NULL," +
                            "DATE_ON_SALE_TO TEXT NOT NULL," +
                            "DATE_ON_SALE_FROM_GMT TEXT NOT NULL," +
                            "DATE_ON_SALE_TO_GMT TEXT NOT NULL," +
                            "VIRTUAL INTEGER," +
                            "DOWNLOADABLE INTEGER," +
                            "DOWNLOAD_LIMIT INTEGER," +
                            "DOWNLOAD_EXPIRY INTEGER," +
                            "SOLD_INDIVIDUALLY INTEGER," +
                            "EXTERNAL_URL TEXT NOT NULL," +
                            "BUTTON_TEXT TEXT NOT NULL," +
                            "TAX_STATUS TEXT NOT NULL," +
                            "TAX_CLASS TEXT NOT NULL," +
                            "MANAGE_STOCK INTEGER," +
                            "STOCK_QUANTITY REAL," +
                            "STOCK_STATUS TEXT NOT NULL," +
                            "BACKORDERS TEXT NOT NULL," +
                            "BACKORDERS_ALLOWED INTEGER," +
                            "BACKORDERED INTEGER," +
                            "SHIPPING_REQUIRED INTEGER," +
                            "SHIPPING_TAXABLE INTEGER," +
                            "SHIPPING_CLASS TEXT NOT NULL," +
                            "SHIPPING_CLASS_ID INTEGER," +
                            "REVIEWS_ALLOWED INTEGER," +
                            "AVERAGE_RATING TEXT NOT NULL," +
                            "RATING_COUNT INTEGER," +
                            "PARENT_ID INTEGER," +
                            "PURCHASE_NOTE TEXT NOT NULL," +
                            "MENU_ORDER INTEGER," +
                            "CATEGORIES TEXT NOT NULL," +
                            "TAGS TEXT NOT NULL," +
                            "IMAGES TEXT NOT NULL," +
                            "ATTRIBUTES TEXT NOT NULL," +
                            "VARIATIONS TEXT NOT NULL," +
                            "DOWNLOADS TEXT NOT NULL," +
                            "RELATED_IDS TEXT NOT NULL," +
                            "CROSS_SELL_IDS TEXT NOT NULL," +
                            "UPSELL_IDS TEXT NOT NULL," +
                            "GROUPED_PRODUCT_IDS TEXT NOT NULL," +
                            "WEIGHT TEXT NOT NULL," +
                            "LENGTH TEXT NOT NULL," +
                            "WIDTH TEXT NOT NULL," +
                            "HEIGHT TEXT NOT NULL)"
                    )
                    db.execSQL("DROP TABLE IF EXISTS WCProductVariationModel")
                    db.execSQL("CREATE TABLE WCProductVariationModel (" +
                            "LOCAL_SITE_ID INTEGER," +
                            "REMOTE_PRODUCT_ID INTEGER," +
                            "REMOTE_VARIATION_ID INTEGER," +
                            "DATE_CREATED TEXT NOT NULL," +
                            "DATE_MODIFIED TEXT NOT NULL," +
                            "DESCRIPTION TEXT NOT NULL," +
                            "PERMALINK TEXT NOT NULL," +
                            "SKU TEXT NOT NULL," +
                            "STATUS TEXT NOT NULL," +
                            "PRICE TEXT NOT NULL," +
                            "REGULAR_PRICE TEXT NOT NULL," +
                            "SALE_PRICE TEXT NOT NULL," +
                            "DATE_ON_SALE_FROM TEXT NOT NULL," +
                            "DATE_ON_SALE_TO TEXT NOT NULL," +
                            "DATE_ON_SALE_FROM_GMT TEXT NOT NULL," +
                            "DATE_ON_SALE_TO_GMT TEXT NOT NULL," +
                            "ON_SALE INTEGER," +
                            "PURCHASABLE INTEGER," +
                            "VIRTUAL INTEGER," +
                            "DOWNLOADABLE INTEGER," +
                            "TAX_STATUS TEXT NOT NULL," +
                            "TAX_CLASS TEXT NOT NULL," +
                            "DOWNLOAD_LIMIT INTEGER," +
                            "DOWNLOAD_EXPIRY INTEGER," +
                            "DOWNLOADS TEXT NOT NULL," +
                            "BACKORDERS TEXT NOT NULL," +
                            "BACKORDERS_ALLOWED INTEGER," +
                            "BACKORDERED INTEGER," +
                            "SHIPPING_CLASS TEXT NOT NULL," +
                            "SHIPPING_CLASS_ID INTEGER," +
                            "MANAGE_STOCK INTEGER," +
                            "STOCK_QUANTITY REAL," +
                            "STOCK_STATUS TEXT NOT NULL," +
                            "IMAGE TEXT NOT NULL," +
                            "WEIGHT TEXT NOT NULL," +
                            "LENGTH TEXT NOT NULL," +
                            "WIDTH TEXT NOT NULL," +
                            "HEIGHT TEXT NOT NULL," +
                            "MENU_ORDER INTEGER," +
                            "ATTRIBUTES TEXT NOT NULL," +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT)")
                }
                141 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE LikeModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "TYPE TEXT NOT NULL,REMOTE_SITE_ID INTEGER,REMOTE_ITEM_ID INTEGER,REMOTE_LIKE_ID INTEGER," +
                            "LIKER_NAME TEXT,LIKER_LOGIN TEXT,LIKER_AVATAR_URL TEXT,LIKER_SITE_ID INTEGER," +
                            "LIKER_SITE_URL TEXT)"
                    )
                }
                142 -> migrate(version) {
                    db.execSQL("ALTER TABLE CommentModel ADD HAS_PARENT BOOLEAN")
                    db.execSQL("ALTER TABLE CommentModel ADD PARENT_ID INTEGER")
                }
                143 -> migrate(version) {
                    db.execSQL(
                            "CREATE TABLE WCShippingLabelCreationEligibility (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_ORDER_ID INTEGER," +
                                    "CAN_CREATE_PACKAGE BOOLEAN," +
                                    "CAN_CREATE_PAYMENT_METHOD BOOLEAN," +
                                    "CAN_CREATE_CUSTOMS_FORM BOOLEAN," +
                                    "IS_ELIGIBLE BOOLEAN," +
                                    "REASON TEXT)"
                    )
                    db.execSQL("DROP TABLE IF EXISTS WCShippingLabelModel")
                    db.execSQL(
                            "CREATE TABLE WCShippingLabelModel (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_ORDER_ID INTEGER," +
                                    "REMOTE_SHIPPING_LABEL_ID INTEGER," +
                                    "CARRIER_ID TEXT NOT NULL," +
                                    "PRODUCT_NAMES TEXT NULL," +
                                    "TRACKING_NUMBER TEXT NOT NULL," +
                                    "SERVICE_NAME TEXT NOT NULL," +
                                    "STATUS TEXT NOT NULL," +
                                    "PACKAGE_NAME TEXT NOT NULL," +
                                    "RATE REAL NOT NULL," +
                                    "REFUNDABLE_AMOUNT REAL NOT NULL," +
                                    "CURRENCY TEXT NOT NULL," +
                                    "FORM_DATA TEXT NOT NULL," +
                                    "REFUND TEXT NULL," +
                                    "PRODUCT_IDS TEXT," +
                                    "DATE_CREATED TEXT)"
                    )
                }
                144 -> migrate(version) {
                    db.execSQL("ALTER TABLE ScanState ADD HAS_VALID_CREDENTIALS BOOLEAN")
                }
                145 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS WCShippingLabelModel")
                    db.execSQL(
                            "CREATE TABLE WCShippingLabelModel (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_ORDER_ID INTEGER," +
                                    "REMOTE_SHIPPING_LABEL_ID INTEGER," +
                                    "CARRIER_ID TEXT NOT NULL," +
                                    "PRODUCT_NAMES TEXT NULL," +
                                    "TRACKING_NUMBER TEXT NOT NULL," +
                                    "SERVICE_NAME TEXT NOT NULL," +
                                    "STATUS TEXT NOT NULL," +
                                    "PACKAGE_NAME TEXT NOT NULL," +
                                    "RATE REAL NOT NULL," +
                                    "REFUNDABLE_AMOUNT REAL NOT NULL," +
                                    "CURRENCY TEXT NOT NULL," +
                                    "FORM_DATA TEXT NOT NULL," +
                                    "REFUND TEXT NULL," +
                                    "PRODUCT_IDS TEXT," +
                                    "DATE_CREATED INTEGER," +
                                    "EXPIRY_DATE INTEGER)"
                    )
                }
                146 -> migrate(version) {
                    db.execSQL("ALTER TABLE LikeModel ADD PREFERRED_BLOG_ID INTEGER")
                    db.execSQL("ALTER TABLE LikeModel ADD PREFERRED_BLOG_NAME TEXT")
                    db.execSQL("ALTER TABLE LikeModel ADD PREFERRED_BLOG_URL TEXT")
                    db.execSQL("ALTER TABLE LikeModel ADD PREFERRED_BLOG_BLAVATAR_URL TEXT")
                }
                147 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS LikeModel")
                    db.execSQL(
                            "CREATE TABLE LikeModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,TYPE TEXT NOT NULL," +
                            "REMOTE_SITE_ID INTEGER,REMOTE_ITEM_ID INTEGER,LIKER_ID INTEGER,LIKER_NAME TEXT," +
                            "LIKER_LOGIN TEXT,LIKER_AVATAR_URL TEXT,LIKER_BIO TEXT,LIKER_SITE_ID INTEGER," +
                            "LIKER_SITE_URL TEXT,PREFERRED_BLOG_ID INTEGER,PREFERRED_BLOG_NAME TEXT," +
                            "PREFERRED_BLOG_URL TEXT,PREFERRED_BLOG_BLAVATAR_URL TEXT,DATE_LIKED TEXT)"
                    )
                }
                148 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS LikeModel")
                    db.execSQL("CREATE TABLE LikeModel (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "TYPE TEXT NOT NULL,REMOTE_SITE_ID INTEGER,REMOTE_ITEM_ID INTEGER,LIKER_ID INTEGER," +
                            "LIKER_NAME TEXT,LIKER_LOGIN TEXT,LIKER_AVATAR_URL TEXT,LIKER_BIO TEXT," +
                            "LIKER_SITE_ID INTEGER,LIKER_SITE_URL TEXT,PREFERRED_BLOG_ID INTEGER," +
                            "PREFERRED_BLOG_NAME TEXT,PREFERRED_BLOG_URL TEXT,PREFERRED_BLOG_BLAVATAR_URL TEXT," +
                            "DATE_LIKED TEXT,TIMESTAMP_FETCHED INTEGER)")
                }
                149 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCRefunds")
                    db.execSQL(
                            "CREATE TABLE WCRefunds (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "ORDER_ID INTEGER," +
                                    "REFUND_ID INTEGER," +
                                    "DATA TEXT NOT NULL)"
                    )
                }
                150 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL(
                            "CREATE TABLE WCUserModel (" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "REMOTE_USER_ID INTEGER," +
                                    "FIRST_NAME TEXT," +
                                    "LAST_NAME TEXT," +
                                    "USERNAME TEXT," +
                                    "EMAIL TEXT," +
                                    "ROLES TEXT)"
                    )
                }
                151 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS MediaUploadModel")
                    db.execSQL("CREATE TABLE MediaUploadModel (_id INTEGER PRIMARY KEY,UPLOAD_STATE INTEGER," +
                            "PROGRESS REAL,ERROR_TYPE TEXT,ERROR_MESSAGE TEXT,ERROR_SUB_TYPE TEXT," +
                            "FOREIGN KEY(_id) REFERENCES MediaModel(_id) ON DELETE CASCADE)")
                }
                152 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCShippingLabelModel ADD COMMERCIAL_INVOICE_URL TEXT")
                }
                153 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderModel ADD META_DATA TEXT")
                }
                154 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS EditorTheme")
                    db.execSQL(
                            "CREATE TABLE EditorTheme(" +
                                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    "LOCAL_SITE_ID INTEGER," +
                                    "STYLESHEET TEXT," +
                                    "VERSION TEXT," +
                                    "RAW_STYLES TEXT," +
                                    "RAW_FEATURES TEXT," +
                                    "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE)"
                    )
                }
                155 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS PlanOffers")
                    db.execSQL("DROP TABLE IF EXISTS PlanOffersFeature")
                    db.execSQL("DROP TABLE IF EXISTS PlanOffersId")
                }
                156 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD ACTIVE_MODULES TEXT")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_PUBLICIZE_PERMANENTLY_DISABLED BOOLEAN")
                }
                157 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD ZENDESK_PLAN TEXT")
                    db.execSQL("ALTER TABLE SiteModel ADD ZENDESK_ADD_ONS TEXT")
                }
                158 -> migrate(version) {
                    db.execSQL("ALTER TABLE EditorTheme ADD IS_FSETHEME BOOLEAN")
                }
                159 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD METADATA TEXT")
                }
                160 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderModel ADD ORDER_KEY TEXT")
                }
                161 -> migrate(version) {
                    db.execSQL("ALTER TABLE EditorTheme ADD GALLERY_WITH_IMAGE_BLOCKS BOOLEAN")
                }
                162 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD STICKY BOOLEAN")
                }
                163 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD ORGANIZATION_ID INTEGER")
                }
                164 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD IS_JETPACK_CP_CONNECTED BOOLEAN")
                }
                165 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCOrderModel ADD SHIPPING_PHONE TEXT")
                }
                166 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD ACTIVE_JETPACK_CONNECTION_PLUGINS TEXT")
                }
                167 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCOrderModel")
                }
                168 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD PURCHASABLE INTEGER")
                }
                169 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCPlugins")
                }
                170 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCOrderNoteModel")
                }
                171 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DELETE FROM WCOrderSummaryModel")
                    db.execSQL("DELETE FROM WCOrderShipmentTrackingModel")
                }
                172 -> migrate(version) {
                    db.execSQL("ALTER TABLE EditorTheme ADD QUOTE_BLOCK_V2 BOOLEAN")
                }
                173 -> migrate(version) {
                    db.execSQL("DELETE FROM QuickStartTaskModel WHERE TASK_NAME='explore_plans' " +
                        "AND TASK_TYPE='grow';")
                }
                174 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_PROMPTS_OPTED_IN BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_PROMPTS_CARD_OPTED_IN BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_POTENTIAL_BLOGGING_SITE BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_REMINDER_ON_MONDAY BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_REMINDER_ON_TUESDAY BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_REMINDER_ON_WEDNESDAY BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_REMINDER_ON_THURSDAY BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_REMINDER_ON_FRIDAY BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_REMINDER_ON_SATURDAY BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD IS_BLOGGING_REMINDER_ON_SUNDAY BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD BLOGGING_REMINDER_HOUR INTEGER")
                    db.execSQL("ALTER TABLE SiteModel ADD BLOGGING_REMINDER_MINUTE INTEGER")
                }
                175 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD ANSWERED_PROMPT_ID INTEGER")
                }
                176 -> migrate(version) {
                    db.execSQL("DELETE FROM QuickStartTaskModel WHERE TASK_NAME='edit_homepage' " +
                        "AND TASK_TYPE='customize';")
                }
                177 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCSettingsModel ADD COUPONS_ENABLED BOOLEAN NOT NULL DEFAULT 0")
                }
                178 -> migrate(version) {
                    db.execSQL("ALTER TABLE EditorTheme ADD LIST_BLOCK_V2 BOOLEAN")
                }
                179 -> migrate(version) {
                    db.execSQL("ALTER TABLE AccountModel ADD TWO_STEP_ENABLED BOOLEAN")
                }
                180 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD APPLICATION_PASSWORDS_AUTHORIZE_URL TEXT")
                }
                181 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductVariationModel ADD METADATA TEXT")
                }
                182 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD BUNDLED_ITEMS TEXT")
                }
                183 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD COMPOSITE_COMPONENTS TEXT")
                }
                184 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD SPECIAL_STOCK_STATUS TEXT")
                }
                185 -> migrate(version) {
                    // renaming tables is not supported by SQLite in some versions, so we need to:
                    // 1. create a new table
                    // 2. copy data from old table to new table, mapping to the new column names
                    // 3. drop the old table
                    // 4. rename the new table to the old table name
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS EditorTheme_new (" +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "LOCAL_SITE_ID INTEGER," +
                            "STYLESHEET TEXT," +
                            "VERSION TEXT," +
                            "RAW_STYLES TEXT," +
                            "RAW_FEATURES TEXT," +
                            "HAS_BLOCK_TEMPLATES INTEGER," +
                            "IS_BLOCK_BASED_THEME INTEGER," +
                            "GALLERY_WITH_IMAGE_BLOCKS INTEGER," +
                            "QUOTE_BLOCK_V2 INTEGER," +
                            "LIST_BLOCK_V2 INTEGER)"
                    )

                    db.execSQL(
                        "INSERT INTO EditorTheme_new (" +
                            "_id, LOCAL_SITE_ID, STYLESHEET, VERSION, RAW_STYLES, RAW_FEATURES, " +
                            "HAS_BLOCK_TEMPLATES, IS_BLOCK_BASED_THEME, GALLERY_WITH_IMAGE_BLOCKS, " +
                            "QUOTE_BLOCK_V2, LIST_BLOCK_V2) " +
                            "SELECT " +
                            "_id, LOCAL_SITE_ID, STYLESHEET, VERSION, RAW_STYLES, RAW_FEATURES, " +
                            "0, IS_FSETHEME, GALLERY_WITH_IMAGE_BLOCKS, " +
                            "QUOTE_BLOCK_V2, LIST_BLOCK_V2 " +
                            "FROM EditorTheme"
                    )

                    db.execSQL("DROP TABLE EditorTheme")
                    db.execSQL("ALTER TABLE EditorTheme_new RENAME TO EditorTheme")
                }
                186 -> migrate(version) {
                    db.execSQL("ALTER TABLE AccountModel ADD USER_IP_COUNTRY_CODE TEXT")
                }
                187 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD PUBLISHED_STATUS INTEGER")
                }
                188 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD CAN_BLAZE BOOLEAN")
                }
                189 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD PLAN_ACTIVE_FEATURES TEXT")
                }
                190 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD AUTO_SHARE_MESSAGE TEXT")
                    db.execSQL("ALTER TABLE PostModel ADD AUTO_SHARE_ID INTEGER")
                }
                191 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCRevenueStatsModel ADD RANGE_ID INTEGER")
                }
                192 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD WAS_ECOMMERCE_TRIAL BOOLEAN")
                    db.execSQL("ALTER TABLE SiteModel ADD PLAN_PRODUCT_SLUG TEXT")
                }
                193 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD PUBLICIZE_SKIP_CONNECTIONS_JSON TEXT")
                }
                194 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS DynamicCard")
                }
                195 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCRevenueStatsModel")
                    db.execSQL(
                        "CREATE TABLE WCRevenueStatsModel(" +
                            "LOCAL_SITE_ID INTEGER," +
                            "INTERVAL TEXT NOT NULL," +
                            "START_DATE TEXT NOT NULL," +
                            "END_DATE TEXT NOT NULL," +
                            "DATA TEXT NOT NULL," +
                            "TOTAL TEXT NOT NULL," +
                            "RANGE_ID TEXT NOT NULL," +
                            "_id INTEGER PRIMARY KEY AUTOINCREMENT)"
                    )
                }
                196 -> migrate(version) {
                    db.execSQL("ALTER TABLE SiteModel ADD IS_SINGLE_USER_SITE BOOLEAN")
                }
                197 -> migrate(version) {
                    db.execSQL("ALTER TABLE ThemeModel ADD THEME_TYPE TEXT")
                    db.execSQL("ALTER TABLE ThemeModel ADD IS_EXTERNAL_THEME BOOLEAN")
                }
                198 -> migrate(version) {
                    db.execSQL("DROP TABLE IF EXISTS WCOrderStatsModel")
                    db.execSQL("DROP TABLE IF EXISTS WCVisitorStatsModel")
                }
                199 -> migrate(version) {
                    db.execSQL("ALTER TABLE PostModel ADD DB_TIMESTAMP INTEGER")
                }
                200 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD IS_SAMPLE_PRODUCT BOOLEAN")
                }

                201 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD MIN_ALLOWED_QUANTITY INTEGER")
                    db.execSQL("ALTER TABLE WCProductModel ADD MAX_ALLOWED_QUANTITY INTEGER")
                    db.execSQL("ALTER TABLE WCProductModel ADD GROUP_OF_QUANTITY INTEGER")
                    db.execSQL("ALTER TABLE WCProductModel ADD COMBINE_VARIATION_QUANTITIES BOOLEAN")

                    db.execSQL("ALTER TABLE WCProductVariationModel ADD MIN_ALLOWED_QUANTITY INTEGER")
                    db.execSQL("ALTER TABLE WCProductVariationModel ADD MAX_ALLOWED_QUANTITY INTEGER")
                    db.execSQL("ALTER TABLE WCProductVariationModel ADD GROUP_OF_QUANTITY INTEGER")
                    db.execSQL("ALTER TABLE WCProductVariationModel ADD OVERRIDE_PRODUCT_QUANTITIES BOOLEAN")
                }

                202 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("ALTER TABLE WCProductModel ADD BUNDLE_MIN_SIZE REAL")
                    db.execSQL("ALTER TABLE WCProductModel ADD BUNDLE_MAX_SIZE REAL")
                }

                203 -> migrateAddOn(ADDON_WOOCOMMERCE, version) {
                    db.execSQL("DROP TABLE IF EXISTS WCProductModel")
                    db.execSQL("""
                        CREATE TABLE WCProductModel (
                        _id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,
                        REMOTE_PRODUCT_ID INTEGER,NAME TEXT NOT NULL,SLUG TEXT NOT NULL,PERMALINK TEXT NOT NULL,
                        DATE_CREATED TEXT NOT NULL,DATE_MODIFIED TEXT NOT NULL,TYPE TEXT NOT NULL,STATUS TEXT NOT NULL,
                        FEATURED INTEGER,CATALOG_VISIBILITY TEXT NOT NULL,DESCRIPTION TEXT NOT NULL,
                        SHORT_DESCRIPTION TEXT NOT NULL,SKU TEXT NOT NULL,PRICE TEXT NOT NULL,REGULAR_PRICE TEXT NOT NULL,
                        SALE_PRICE TEXT NOT NULL,ON_SALE INTEGER,TOTAL_SALES INTEGER,PURCHASABLE INTEGER,
                        DATE_ON_SALE_FROM TEXT NOT NULL,DATE_ON_SALE_TO TEXT NOT NULL,DATE_ON_SALE_FROM_GMT TEXT NOT NULL,
                        DATE_ON_SALE_TO_GMT TEXT NOT NULL,VIRTUAL INTEGER,DOWNLOADABLE INTEGER,DOWNLOAD_LIMIT INTEGER,
                        DOWNLOAD_EXPIRY INTEGER,SOLD_INDIVIDUALLY INTEGER,EXTERNAL_URL TEXT NOT NULL,BUTTON_TEXT TEXT NOT NULL,
                        TAX_STATUS TEXT NOT NULL,TAX_CLASS TEXT NOT NULL,MANAGE_STOCK INTEGER,STOCK_QUANTITY REAL,
                        STOCK_STATUS TEXT NOT NULL,BACKORDERS TEXT NOT NULL,BACKORDERS_ALLOWED INTEGER,BACKORDERED INTEGER,
                        SHIPPING_REQUIRED INTEGER,SHIPPING_TAXABLE INTEGER,SHIPPING_CLASS TEXT NOT NULL,SHIPPING_CLASS_ID INTEGER,
                        REVIEWS_ALLOWED INTEGER,AVERAGE_RATING TEXT NOT NULL,RATING_COUNT INTEGER,PARENT_ID INTEGER,
                        PURCHASE_NOTE TEXT NOT NULL,MENU_ORDER INTEGER,CATEGORIES TEXT NOT NULL,TAGS TEXT NOT NULL,
                        IMAGES TEXT NOT NULL,ATTRIBUTES TEXT NOT NULL,VARIATIONS TEXT NOT NULL,DOWNLOADS TEXT NOT NULL,
                        RELATED_IDS TEXT NOT NULL,CROSS_SELL_IDS TEXT NOT NULL,UPSELL_IDS TEXT NOT NULL,
                        GROUPED_PRODUCT_IDS TEXT NOT NULL,WEIGHT TEXT NOT NULL,LENGTH TEXT NOT NULL,WIDTH TEXT NOT NULL,
                        HEIGHT TEXT NOT NULL,METADATA TEXT NOT NULL,BUNDLED_ITEMS TEXT NOT NULL,
                        COMPOSITE_COMPONENTS TEXT NOT NULL,SPECIAL_STOCK_STATUS TEXT NOT NULL,BUNDLE_MIN_SIZE REAL,
                        BUNDLE_MAX_SIZE REAL,MIN_ALLOWED_QUANTITY INTEGER,MAX_ALLOWED_QUANTITY INTEGER,
                        GROUP_OF_QUANTITY INTEGER,COMBINE_VARIATION_QUANTITIES INTEGER,PASSWORD TEXT,IS_SAMPLE_PRODUCT INTEGER
                        )
                    """.trimIndent())
                }

                204 -> db.execSQL("ALTER TABLE SiteModel ADD IS_DELETED INTEGER DEFAULT 0")

                205 -> migrate(version) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS EditorSettings (
                            _id INTEGER PRIMARY KEY AUTOINCREMENT,
                            LOCAL_SITE_ID INTEGER NOT NULL,
                            RAW_SETTINGS TEXT,
                            FOREIGN KEY (LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                }

                206 -> {
                    db.execSQL("ALTER TABLE SiteModel ADD API_REST_USERNAME TEXT")
                    db.execSQL("ALTER TABLE SiteModel ADD API_REST_PASSWORD TEXT")
                }

                207 -> {
                    db.execSQL("ALTER TABLE SiteModel ADD API_REST_USERNAME_IV TEXT")
                    db.execSQL("ALTER TABLE SiteModel ADD API_REST_PASSWORD_IV TEXT")
                }

                208 -> {
                    db.execSQL("DROP TABLE IF EXISTS EncryptedLogModel")
                }
            }
        }
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    /**
     * Detect when the database is downgraded in debug builds so we can recreate all the tables. Note that we
     * hide this behind a BuildConfig flag as a protection against accidentally deleting the data (ie: we
     * don't want this to ever be enabled for release builds by mistake).
     */
    override fun onDowngrade(db: SQLiteDatabase?, helper: WellTableManager?, oldVersion: Int, newVersion: Int) {
        if (BuildConfig.DEBUG && BuildConfig.WP_ENABLE_DATABASE_DOWNGRADE) {
            // note: don't call super() here because it throws an exception
            val toast = Toast.makeText(
                    context,
                    "Database downgraded from version $oldVersion to $newVersion",
                    Toast.LENGTH_LONG
            )
            toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 0)
            toast.show()

            AppLog.d(T.DB, "Database downgraded from version $oldVersion to $newVersion")
            helper?.let { reset(it) }
        } else {
            super.onDowngrade(db, helper, oldVersion, newVersion)
        }
    }

    override fun onConfigure(db: SQLiteDatabase, helper: WellTableManager?) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    /**
     * Increase the cursor window size to 5MB for devices running API 28 and above. This should
     * reduce the number of SQLiteBlobTooBigExceptions.
     * NOTE: this is only called on API 28 and above since earlier versions don't allow adjusting
     * the cursor window size.
     */
    @Suppress("MagicNumber")
    override fun getCursorWindowSize() = (1024L * 1024L * 5L)

    /**
     * Drop and create all tables
     */
    @Suppress("CheckStyle")
    open fun reset() {
        val db = WellSql.giveMeWritableDb()
        mTables.forEach { clazz ->
            val table = getTable(clazz)
            db.execSQL("DROP TABLE IF EXISTS ${table.tableName}")
            db.execSQL(table.createStatement())
        }
    }

    /**
     * Recreates all the tables in this database - similar to the above but can be used from onDowngrade where we can't
     * call giveMeWritableDb (attempting to do so results in "IllegalStateException: getDatabase called recursively")
     */
    fun reset(helper: WellTableManager) {
        AppLog.d(T.DB, "resetting tables")
        for (table in mTables) {
            AppLog.d(T.DB, "dropping table " + table.simpleName)
            helper.dropTable(table)
            AppLog.d(T.DB, "creating table " + table.simpleName)
            helper.createTable(table)
        }
    }

    private fun migrate(version: Int, script: () -> Unit) {
        AppLog.d(T.DB, "Migrating to version ${version + 1}")
        script()
    }

    private fun migrateAddOn(@AddOn name: String, version: Int, script: () -> Unit) {
        AppLog.d(T.DB, "Migrating addon $name to version ${version + 1}")
        if (mActiveAddOns.contains(name)) {
            script()
        }
    }
}
