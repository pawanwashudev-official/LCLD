package com.neubofy.lcld.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.lcld.R
import com.neubofy.lcld.commands.availableCommands
import com.neubofy.lcld.ui.TaggedFragment

class PermissionManagerFragment : TaggedFragment() {

    override fun getStaticTag() = "PermissionManagerFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_command_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val commandListAdapter = CommandListAdapter(activity as AppCompatActivity)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_commands)
        recyclerView.adapter = commandListAdapter

        commandListAdapter.submitList(availableCommands(view.context))
    }
}
