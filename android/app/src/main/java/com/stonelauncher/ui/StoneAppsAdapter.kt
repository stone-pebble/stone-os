package com.stonelauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stonelauncher.R
import com.stonelauncher.models.StoneApp

/**
 * RecyclerView adapter for displaying the Stone apps grid.
 *
 * Displays a 3x4 grid of minimalist text-only app buttons.
 */
class StoneAppsAdapter(
    private val apps: List<StoneApp>,
    private val onAppClick: (StoneApp) -> Unit
) : RecyclerView.Adapter<StoneAppsAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val appName: TextView = view.findViewById(R.id.appName)

        fun bind(app: StoneApp) {
            appName.text = app.name
            itemView.setOnClickListener { onAppClick(app) }
        }
    }
}
