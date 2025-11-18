package com.st10028374.vitality_vault.routes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.routes.models.RouteModel
import com.st10028374.vitality_vault.routes.utils.TrackingUtils

class RoutesAdapter(
    private val onRouteClick: (RouteModel) -> Unit,
    private val onPlayClick: (RouteModel) -> Unit
) : ListAdapter<RouteModel, RoutesAdapter.RouteViewHolder>(RouteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = getItem(position)
        holder.bind(route)
    }

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRouteName: TextView = itemView.findViewById(R.id.tvRouteName)
        private val tvRouteDetails: TextView = itemView.findViewById(R.id.tvRouteDetails)
        private val btnPlay: ImageButton = itemView.findViewById(R.id.btnPlayRoute)

        fun bind(route: RouteModel) {
            tvRouteName.text = route.routeName

            val distance = TrackingUtils.formatDistance(route.distance)
            val duration = TrackingUtils.formatDuration(route.duration)
            tvRouteDetails.text = "$distance km • $duration"

            itemView.setOnClickListener {
                onRouteClick(route)
            }

            btnPlay.setOnClickListener {
                onPlayClick(route)
            }
        }
    }

    class RouteDiffCallback : DiffUtil.ItemCallback<RouteModel>() {
        override fun areItemsTheSame(oldItem: RouteModel, newItem: RouteModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RouteModel, newItem: RouteModel): Boolean {
            return oldItem == newItem
        }
    }
}