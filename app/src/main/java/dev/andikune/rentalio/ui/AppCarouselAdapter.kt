package dev.andikune.rentalio.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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

    fun updateFocusedItem(position: Int, glowColor: Int) {
        val prev = focusedPosition
        focusedPosition = position
        currentGlowColor = glowColor
        if (prev != -1) notifyItemChanged(prev)
        if (position != -1) notifyItemChanged(position)
    }

    fun updateGlowColorOnly(glowColor: Int) {
        currentGlowColor = glowColor
        if (focusedPosition in 0 until itemCount) {
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
        if (itemHeightPx > 0 && holder.itemView.layoutParams.height != itemHeightPx) {
            holder.itemView.layoutParams.height = itemHeightPx
        }
        val app = items[position]
        holder.bind(app, position == focusedPosition, currentGlowColor)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val tvAppCategory: TextView = itemView.findViewById(R.id.tv_app_category)
        private val viewGlowBorder: View = itemView.findViewById(R.id.view_item_glow_border)
        private val viewFocusIndicator: View = itemView.findViewById(R.id.view_focus_indicator)

        fun bind(app: WhitelistApp, isFocused: Boolean, glowColor: Int) {
            tvAppName.text = app.label
            tvAppCategory.text = app.category

            // Load app icon
            try {
                val iconDrawable = context.packageManager.getApplicationIcon(app.packageName)
                ivAppIcon.setImageDrawable(iconDrawable)
            } catch (_: Exception) {
                ivAppIcon.setImageResource(R.mipmap.ic_launcher)
            }

            // Dynamic border & glow on centered focused item
            if (isFocused) {
                val borderDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(16f)
                    setColor(Color.parseColor("#331E293B"))
                    setStroke(dpToPx(2f).toInt(), glowColor)
                }
                viewGlowBorder.background = borderDrawable
                viewFocusIndicator.alpha = 1f
                val dotDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(glowColor)
                }
                viewFocusIndicator.background = dotDrawable
                tvAppName.setTextColor(Color.WHITE)
            } else {
                val borderDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(16f)
                    setColor(Color.parseColor("#1A0F172A"))
                    setStroke(dpToPx(1f).toInt(), Color.parseColor("#26FFFFFF"))
                }
                viewGlowBorder.background = borderDrawable
                viewFocusIndicator.alpha = 0f
                tvAppName.setTextColor(Color.parseColor("#F8FAFC"))
            }

            itemView.setOnClickListener {
                onItemClick(app, bindingAdapterPosition)
            }
        }

        private fun dpToPx(dp: Float): Float {
            return dp * context.resources.displayMetrics.density
        }
    }
}
