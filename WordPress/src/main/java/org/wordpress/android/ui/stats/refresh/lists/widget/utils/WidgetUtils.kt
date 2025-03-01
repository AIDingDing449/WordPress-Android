package org.wordpress.android.ui.stats.refresh.lists.widget.utils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.ImageView.ScaleType.FIT_START
import android.widget.RemoteViews
import com.bumptech.glide.request.target.AppWidgetTarget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.ui.stats.refresh.StatsActivity
import org.wordpress.android.ui.stats.refresh.lists.widget.IS_WIDE_VIEW_KEY
import org.wordpress.android.ui.stats.refresh.lists.widget.SITE_ID_KEY
import org.wordpress.android.ui.stats.refresh.lists.widget.WidgetService
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.DARK
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color.LIGHT
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType
import org.wordpress.android.ui.stats.refresh.utils.StatsLaunchedFrom
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.ICON
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupUtils
import java.time.DayOfWeek.MONDAY
import java.time.temporal.TemporalAdjusters
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import kotlin.random.Random

private const val MIN_WIDTH = 250
private const val ICON_MAX_DIMENSION = 100

class WidgetUtils
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    val imageManager: ImageManager
) {
    private val coroutineScope = CoroutineScope(mainDispatcher)
    fun isWidgetWiderThanLimit(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        minWidthLimit: Int = MIN_WIDTH
    ): Boolean {
        val minWidth = appWidgetManager.getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
        return minWidth > minWidthLimit
    }

    fun getLayout(colorMode: Color): Int {
        return when (colorMode) {
            DARK -> R.layout.stats_widget_list_dark
            LIGHT -> R.layout.stats_widget_list_light
        }
    }

    fun setSiteIcon(
        siteModel: SiteModel?,
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        views.setViewVisibility(R.id.widget_site_icon, View.VISIBLE)
        coroutineScope.launch {
            val awt = AppWidgetTarget(context, R.id.widget_site_icon, views, appWidgetId)
            imageManager.load(
                awt,
                context,
                ICON,
                siteModel?.iconUrl ?: "",
                FIT_START,
                ICON_MAX_DIMENSION,
                ICON_MAX_DIMENSION
            )
        }
    }

    @Suppress("LongParameterList")
    fun showError(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetId: Int,
        networkAvailable: Boolean,
        hasAccessToken: Boolean,
        resourceProvider: ResourceProvider,
        context: Context,
        widgetType: Class<*>
    ) {
        views.setViewVisibility(R.id.widget_site_icon, View.GONE)
        views.setOnClickPendingIntent(
            R.id.widget_title_container,
            PendingIntent.getActivity(
                context,
                0,
                Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setViewVisibility(R.id.widget_content, View.GONE)
        views.setViewVisibility(R.id.widget_error, View.VISIBLE)
        val errorMessage = if (!networkAvailable) {
            R.string.stats_widget_error_no_network
        } else if (!hasAccessToken) {
            R.string.stats_widget_error_no_access_token
        } else {
            R.string.stats_widget_error_no_data
        }
        views.setTextViewText(
            R.id.widget_error_message,
            resourceProvider.getString(errorMessage)
        )
        val pendingSync = getRetryIntent(context, widgetType, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_error, pendingSync)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun getRetryIntent(
        context: Context,
        widgetType: Class<*>,
        appWidgetId: Int
    ): PendingIntent? {
        val intentSync = Intent(context, widgetType)
        intentSync.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        intentSync.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            Random(appWidgetId).nextInt(),
            intentSync,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Suppress("LongParameterList", "DEPRECATION")
    fun showList(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        colorMode: Color,
        siteId: Int,
        widgetType: WidgetType,
        isWideView: Boolean
    ) {
        views.setPendingIntentTemplate(R.id.widget_content, getPendingTemplate(context))
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error, View.GONE)
        val listIntent = Intent(context, WidgetService::class.java)
        listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        listIntent.putColorMode(colorMode)
        listIntent.putViewType(widgetType)
        listIntent.putExtra(SITE_ID_KEY, siteId)
        listIntent.putExtra(IS_WIDE_VIEW_KEY, isWideView)
        listIntent.data = Uri.parse(
            listIntent.toUri(Intent.URI_INTENT_SCHEME)
        )
        views.setRemoteAdapter(R.id.widget_content, listIntent)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_content)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun getPendingSelfIntent(
        context: Context,
        localSiteId: Int,
        statsTimeframe: StatsTimeframe,
        granularity: StatsGranularity? = null
    ): PendingIntent {
        val intent = Intent(context, StatsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(WordPress.LOCAL_SITE_ID, localSiteId)
        intent.putExtra(StatsActivity.ARG_DESIRED_TIMEFRAME, statsTimeframe)
        intent.putExtra(StatsActivity.ARG_LAUNCHED_FROM, StatsLaunchedFrom.WIDGET)
        intent.putExtra(StatsActivity.ARG_GRANULARITY, granularity)
        return PendingIntent.getActivity(
            context,
            getRandomId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPendingTemplate(context: Context): PendingIntent {
        val intent = Intent(context, StatsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Before SDK 31, this was mutable by default, but this condition is still needed to satisfy lint rules :)
        val templateFlags = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

        return PendingIntent.getActivity(
            context,
            getRandomId(),
            intent,
            templateFlags,
        )
    }

    private fun getRandomId(): Int {
        return Random(Date().time).nextInt()
    }

    fun getLastWeekPeriodData(visitsAndViewsModel: VisitsAndViewsModel): PeriodData? {
        val currentDateForSite = WeeklyRoundupUtils.parseStandardDate(visitsAndViewsModel.period) ?: return null
        val lastWeekStartDate = currentDateForSite.minusWeeks(1).with(TemporalAdjusters.previousOrSame(MONDAY))
        return visitsAndViewsModel.dates.find { WeeklyRoundupUtils.parseWeekPeriodDate(it.period) == lastWeekStartDate }
    }
}
