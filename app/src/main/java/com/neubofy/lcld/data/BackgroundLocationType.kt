package com.neubofy.lcld.data

class BackgroundLocationType(inState: Int) {
    var fused: Boolean = (inState and FUSED) != 0
    var gps: Boolean = (inState and GPS) != 0
    var cell: Boolean = (inState and CELL) != 0

    /**
     * Use bitmasking to encode the state into an integer that can be persisted in the settings.
     */
    fun encode(): Int {
        var state = BASE
        if (fused) state = state or FUSED
        if (gps) state = state or GPS
        if (cell) state = state or CELL
        return state
    }

    fun isAll(): Boolean {
        return fused && gps && cell
    }

    fun isEmpty(): Boolean {
        return !fused && !gps && !cell
    }

    companion object {
        // Offset/prefix values in order to distinguish them from the old encoding
        const val BASE = 0x1000 // 4096 --> plenty of room for future extensions

        @JvmStatic
        var EMPTY = BASE // only for Java interop

        // One for each relevant "locate" option.
        const val FUSED = 0x1
        const val GPS = 0x2
        const val CELL = 0x4

        fun fromEmpty(): BackgroundLocationType {
            return BackgroundLocationType(BASE)

        }

        // OLD ENCODING:
        // 0=GPS, 1=CELL, 2=ALL, 3=NONE
        fun fromOldEncoding(inState: Int): BackgroundLocationType {
            val state = BackgroundLocationType(BASE)

            when (inState) {
                0 -> state.gps = true
                1 -> state.cell = true
                2 -> {
                    state.gps = true
                    state.cell = true
                }

                else -> {
                    // invalid state
                }
            }

            return state
        }
    }
}
