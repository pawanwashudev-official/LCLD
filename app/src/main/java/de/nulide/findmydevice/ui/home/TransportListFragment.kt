package de.nulide.findmydevice.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import de.nulide.findmydevice.R
import de.nulide.findmydevice.transports.availableTransports
import de.nulide.findmydevice.ui.TaggedFragment


class TransportListFragment : TaggedFragment() {

    override fun getStaticTag() = "TransportListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transport_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transportListAdapter = TransportListAdapter(activity as AppCompatActivity)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_transports)
        recyclerView.adapter = transportListAdapter

        transportListAdapter.submitList(availableTransports(view.context))
    }
}
