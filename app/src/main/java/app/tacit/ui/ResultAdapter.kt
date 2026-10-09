package app.tacit.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.tacit.Graph
import app.tacit.R
import app.tacit.core.ItemKind
import app.tacit.core.SearchItem
import app.tacit.sources.ContactInfo

interface RowCallbacks {
    fun onPrimary(item: SearchItem, view: View)
    fun onSecondary(item: SearchItem, view: View)
    fun onContactAction(item: SearchItem, action: ContactAction, view: View)
}

enum class ContactAction { CALL, MESSAGE, WHATSAPP, WHATSAPP_BUSINESS, SIGNAL, TELEGRAM }

class ResultAdapter(private val context: Context, private val callbacks: RowCallbacks) : BaseAdapter() {

    private var items: List<SearchItem> = emptyList()
    var palette: Palette = Ui.palette(context)
    var selected = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var installedMessengers: Set<ContactAction> = emptySet()

    fun submit(newItems: List<SearchItem>) {
        items = newItems
        if (selected >= items.size) selected = 0
        notifyDataSetChanged()
    }

    fun itemAt(position: Int): SearchItem? = items.getOrNull(position)

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Any = items[position]
    override fun getItemId(position: Int): Long = items[position].key.hashCode().toLong()
    override fun hasStableIds(): Boolean = true
    override fun getViewTypeCount(): Int = 1
    override fun getItemViewType(position: Int): Int = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder = (convertView?.tag as? Holder) ?: Holder(context)
        val item = items[position]
        holder.bind(item, position == selected)
        return holder.root
    }

    private inner class Holder(context: Context) {
        val root: LinearLayout = Ui.row(context)
        val glyph = TextView(context)
        val icon = ImageView(context)
        val title: TextView
        val subtitle: TextView
        val actions = Ui.row(context)
        var boundKey: String? = null

        init {
            val pad = Ui.dp(context, 12)
            root.setPadding(pad, Ui.dp(context, 8), Ui.dp(context, 6), Ui.dp(context, 8))
            root.minimumHeight = Ui.dp(context, 56)
            root.tag = this
            root.isFocusable = false
            val iconSize = Ui.dp(context, 32)
            glyph.layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            glyph.gravity = Gravity.CENTER
            glyph.textSize = 14f
            icon.layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            val texts = Ui.column(context)
            texts.setPadding(pad, 0, pad / 2, 0)
            title = Ui.text(context, "", 16f, palette.foreground)
            subtitle = Ui.text(context, "", 12f, palette.muted)
            texts.addView(title)
            texts.addView(subtitle)
            root.addView(glyph)
            root.addView(icon)
            root.addView(texts, Ui.weight())
            root.addView(actions)
        }

        fun bind(item: SearchItem, isSelected: Boolean) {
            boundKey = item.key
            title.setTextColor(palette.foreground)
            subtitle.setTextColor(palette.muted)
            title.text = item.label
            subtitle.text = subtitleFor(item)
            Ui.gone(subtitle, subtitle.text.isEmpty())
            root.background = if (isSelected) GradientDrawable().apply {
                setColor(palette.surface)
                cornerRadius = Ui.dp(context, 10).toFloat()
            } else null
            bindIcon(item)
            bindActions(item)
            root.setOnClickListener { callbacks.onPrimary(item, it) }
            root.setOnLongClickListener {
                callbacks.onSecondary(item, it)
                true
            }
            root.contentDescription = "${item.label}, ${subtitleFor(item)}"
        }

        private fun subtitleFor(item: SearchItem): String = when (item.kind) {
            ItemKind.CLIPBOARD -> if (item.subtitle == "hidden") context.getString(R.string.hidden_clip) else item.subtitle
            else -> item.subtitle
        }

        private fun bindIcon(item: SearchItem) {
            if (item.kind == ItemKind.APP && Graph.prefs.iconMode != "none") {
                glyph.visibility = View.GONE
                icon.visibility = View.VISIBLE
                val cached = Graph.icons.cached(item.key)
                icon.setImageBitmap(cached)
                if (cached == null) {
                    val key = item.key
                    Graph.icons.load(item.key, item.payload, palette.foreground) { bitmap ->
                        if (boundKey == key) icon.setImageBitmap(bitmap)
                    }
                }
                return
            }
            icon.visibility = View.GONE
            glyph.visibility = if (Graph.prefs.iconMode == "none" && item.kind == ItemKind.APP) View.GONE else View.VISIBLE
            glyph.setTextColor(palette.foreground)
            glyph.text = glyphFor(item)
            glyph.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(Ui.dp(context, 1), palette.line)
            }
        }

        private fun glyphFor(item: SearchItem): String = when (item.kind) {
            ItemKind.CONTACT -> item.label.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()
            ItemKind.SETTING -> "⚙"
            ItemKind.FILE -> if (item.subtitle.endsWith("/")) "▢" else "▤"
            ItemKind.SNIPPET -> "❝"
            ItemKind.CLIPBOARD -> "⎘"
            ItemKind.CALCULATOR -> "="
            ItemKind.WEB -> "↗"
            ItemKind.ACTION -> "›"
            ItemKind.APP -> item.label.take(1).uppercase()
        }

        private fun bindActions(item: SearchItem) {
            actions.removeAllViews()
            if (item.kind != ItemKind.CONTACT) return
            val info = item.extra as? ContactInfo
            if (info != null && info.numbers.isEmpty()) return
            addAction(item, ContactAction.CALL, "☎", R.string.action_call)
            addAction(item, ContactAction.MESSAGE, "✉", R.string.action_message)
            val hasWa = ContactAction.WHATSAPP in installedMessengers
            val hasWab = ContactAction.WHATSAPP_BUSINESS in installedMessengers
            if (hasWa) addAction(item, ContactAction.WHATSAPP, "W", R.string.action_whatsapp)
            if (hasWab && (!hasWa || info?.whatsappBusinessDataId != null)) addAction(item, ContactAction.WHATSAPP_BUSINESS, "B", R.string.action_whatsapp_business)
        }

        private fun addAction(item: SearchItem, action: ContactAction, symbol: String, labelRes: Int) {
            val size = Ui.dp(context, 44)
            val button = TextView(context).apply {
                text = symbol
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(palette.foreground)
                contentDescription = context.getString(labelRes)
                isClickable = true
                setOnClickListener { callbacks.onContactAction(item, action, it) }
            }
            actions.addView(button, LinearLayout.LayoutParams(size, size))
        }
    }
}
