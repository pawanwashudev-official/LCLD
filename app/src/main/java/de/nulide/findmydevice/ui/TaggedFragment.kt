package de.nulide.findmydevice.ui

import androidx.fragment.app.Fragment


abstract class TaggedFragment : Fragment() {
    abstract fun getStaticTag(): String
}