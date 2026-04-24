package com.neubofy.lcld.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.card.MaterialCardView
import com.neubofy.lcld.R
import com.neubofy.lcld.ui.TaggedFragment
import com.neubofy.lcld.ui.settings.AllowlistActivity
import com.neubofy.lcld.ui.settings.FMDServerActivity

class MainPageFragment : TaggedFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_page, container, false)

        val cardServer = view.findViewById<MaterialCardView>(R.id.card_fmd_server)
        val cardContacts = view.findViewById<MaterialCardView>(R.id.card_allowed_contacts)
        val cardPermissions = view.findViewById<MaterialCardView>(R.id.card_permission_manager)

        cardServer.setOnClickListener {
            startActivity(Intent(requireContext(), FMDServerActivity::class.java))
        }

        cardContacts.setOnClickListener {
            startActivity(Intent(requireContext(), AllowlistActivity::class.java))
        }

        cardPermissions.setOnClickListener {
            // Replace with Permission Manager Fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PermissionManagerFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
