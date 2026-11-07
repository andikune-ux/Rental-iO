package dev.andikune.rentalio.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.recyclerview.widget.RecyclerView
import dev.andikune.rentalio.R
import dev.andikune.rentalio.data.model.WhitelistApp
import dev.andikune.rentalio.ui.theme.GlassColors

class AppCarouselAdapter(
    private val context: Context,
    private val onItemClick: (WhitelistApp, Int) -> Unit
) : RecyclerView.Adapter<AppCarouselAdapter.ViewHolder>() {

    companion object {
        const val VIRTUAL_CYCLES = 600
    }

    private val items = mutableListOf<WhitelistApp>()
    private var itemHeightPx: Int = 0
    var focusedPosition: Int = -1
        private set
    private var currentGlowColor: Int = GlassColors.AccentGlow.toArgb()

    fun setItems(newItems: List<WhitelistApp>, itemHeight: Int) {
        items.clear()
        items.addAll(newItems)
        this.itemHeightPx = itemHeight
        focusedPosition = -1
        notifyDataSetChanged()
    }

    fun setItemHeight(height: Int) {
        if (this.itemHeightPx != height && height > 0) {
            this.itemHeightPx = height
            notifyDataSetChanged()
        }
    }

    fun getItems(): List<WhitelistApp> = items.toList()

    fun getInitialCenterPosition(): Int {
        if (items.isEmpty()) return 0
        return (items.size * (VIRTUAL_CYCLES / 2))
    }

    fun updateFocusedItem(position: Int, glowColor: Int) {
        val prev = focusedPosition
        focusedPosition = position
        currentGlowColor = glowColor
        if (prev in 0 until itemCount) notifyItemChanged(prev)
        if (position in 0 until itemCount) notifyItemChanged(position)
    }

    fun updateGlowColorDirectly(recyclerView: RecyclerView, glowColor: Int) {
        currentGlowColor = glowColor
        if (focusedPosition != -1) {
            val holder = recyclerView.findViewHolderForAdapterPosition(focusedPosition) as? ViewHolder
            holder?.applyGlow(glowColor, isFocused = true)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_carousel, parent, false)
        if (itemHeightPx > 0) {
            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeightPx)
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (items.isEmpty()) return
        if (itemHeightPx > 0 && holder.itemView.layoutParams.height != itemHeightPx) {
            holder.itemView.layoutParams.height = itemHeightPx
        }
        
        // FIX BUG CRASH: Memastikan hasil modulo selalu positif meskipun posisinya negatif
        val actualIndex = (position % items.size + items.size) % items.size
        val app = items[actualIndex]
        holder.bind(app, position, position == focusedPosition, currentGlowColor)
    }

    override fun getItemCount(): Int = if (items.isEmpty()) 0 else items.size * VIRTUAL_CYCLES

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

            applyGlow(glowColor, isFocused)
            tvAppName.setTextColor(if (isFocused) Color.WHITE else Color.parseColor("#E2E8F0"))

            itemView.setOnClickListener {
                onItemClick(app, position)
            }
        }

        fun applyGlow(glowColor: Int, isFocused: Boolean) {
            val borderDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14f)
                setColor(Color.TRANSPARENT)
                val strokeColor = if (isFocused) glowColor else Color.TRANSPARENT
                val strokeWidth = if (isFocused) dpToPx(1.5f).toInt() else 0
                setStroke(strokeWidth, strokeColor)
            }
            viewGlowBorder.background = borderDrawable
        }

        private fun dpToPx(dp: Float): Float {
            return dp * context.resources.displayMetrics.density
        }
    }
}
