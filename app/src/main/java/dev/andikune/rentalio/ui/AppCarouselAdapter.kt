package dev.andikune.rentalio.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.andikune.rentalio.R
import dev.andikune.rentalio.data.model.WhitelistApp
import dev.andikune.rentalio.ui.theme.GlassColors

class AppCarouselAdapter(
    private val context: Context,
    private val onItemClick: (WhitelistApp, Int) -> Unit
) : RecyclerView.Adapter<AppCarouselAdapter.ViewHolder>() {

    private val items = mutableListOf<WhitelistApp>()
    private var itemHeightPx: Int = 0
    var focusedPosition: Int = -1
        private set
    private var currentGlowColor: Int = GlassColors.AccentGlow.hashCode()

    fun setItems(newItems: List<WhitelistApp>, itemHeight: Int) {
        items.clear()
        items.addAll(newItems)
        this.itemHeightPx = itemHeight
        notifyDataSetChanged()
    }

    fun setItemHeight(height: Int) {
        if (this.itemHeightPx != height && height > 0) {
            this.itemHeightPx = height
            notifyDataSetChanged()
        }
    }

    fun getItems(): List<WhitelistApp> = items

    fun updateFocusedItem(position: Int, glowColor: Int) {
        val prev = focusedPosition
        focusedPosition = position
        currentGlowColor = glowColor
        if (prev != -1) notifyItemChanged(prev)
        if (position != -1) notifyItemChanged(position)
    }

    fun updateGlowColorOnly(glowColor: Int) {
        currentGlowColor = glowColor
        if (focusedPosition != -1) {
            notifyItemChanged(focusedPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_carousel, parent, false)
        if (itemHeightPx > 0) {
            view.layoutParams.height = itemHeightPx
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (items.isEmpty()) return
        if (itemHeightPx > 0 && holder.itemView.layoutParams.height != itemHeightPx) {
            holder.itemView.layoutParams.height = itemHeightPx
        }
        val actualIndex = position % items.size
        val app = items[actualIndex]
        holder.bind(app, position, position == focusedPosition, currentGlowColor)
    }

    // Infinite Virtual Wrap
    override fun getItemCount(): Int = if (items.isEmpty()) 0 else Int.MAX_VALUE

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val tvAppCategory: TextView = itemView.findViewById(R.id.tv_app_category)
        private val viewGlowBorder: View = itemView.findViewById(R.id.view_item_glow_border)

        fun bind(app: WhitelistApp, position: Int, isFocused: Boolean, glowColor: Int) {
            tvAppName.text = app.label
            tvAppCategory.text = app.category

            try {
                val iconDrawable = context.packageManager.getApplicationIcon(app.packageName)
                ivAppIcon.setImageDrawable(iconDrawable)
            } catch (_: Exception) {
                ivAppIcon.setImageResource(R.mipmap.ic_launcher)
            }

            // Outline / Border penanda dinamis saat fokus
            val borderDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14f)
                setColor(if (isFocused) Color.parseColor("#14FFFFFF") else Color.TRANSPARENT)
                val strokeColor = if (isFocused) glowColor else Color.parseColor("#1FFFFFFF")
                val strokeWidth = if (isFocused) dpToPx(2f).toInt() else dpToPx(1f).toInt()
                setStroke(strokeWidth, strokeColor)
            }
            viewGlowBorder.background = borderDrawable

            tvAppName.setTextColor(if (isFocused) Color.WHITE else Color.parseColor("#E2E8F0"))

            itemView.setOnClickListener {
                onItemClick(app, position)
            }
        }

        private fun dpToPx(dp: Float): Float {
            return dp * context.resources.displayMetrics.density
        }
    }
}
