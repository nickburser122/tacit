package app.tacit.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.os.Bundle
import android.widget.RemoteViews
import app.tacit.R
import app.tacit.ui.SearchActivity

object Launch {
    fun intent(context: Context): Intent =
        Intent(context, SearchActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun pending(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0, intent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}

class TacitTile : TileService() {
    override fun onClick() {
        super.onClick()
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(Launch.pending(this))
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(Launch.intent(this))
        }
    }
}

class SearchWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_search)
            views.setOnClickPendingIntent(R.id.widget_root, Launch.pending(context))
            manager.updateAppWidget(id, views)
        }
    }
}

class AssistService : VoiceInteractionService()

class AssistSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = AssistSession(this)
}

class AssistSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        startAssistantActivity(Launch.intent(context))
        hide()
    }
}

class AssistRecognitionService : android.speech.RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: android.speech.RecognitionService.Callback?) {
        runCatching { listener?.error(android.speech.SpeechRecognizer.ERROR_CLIENT) }
    }
    override fun onCancel(listener: android.speech.RecognitionService.Callback?) = Unit
    override fun onStopListening(listener: android.speech.RecognitionService.Callback?) = Unit
}
