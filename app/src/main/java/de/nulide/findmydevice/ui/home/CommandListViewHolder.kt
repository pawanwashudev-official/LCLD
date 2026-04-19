package de.nulide.findmydevice.ui.home

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import de.nulide.findmydevice.R
import de.nulide.findmydevice.commands.Command
import de.nulide.findmydevice.ui.setupPermissionsList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import de.nulide.findmydevice.commands.CommandHandler
import de.nulide.findmydevice.data.Settings
import de.nulide.findmydevice.data.SettingsRepository
import de.nulide.findmydevice.transports.InAppTransport


class CommandListViewHolder(
    private val activity: AppCompatActivity,
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: Command) {
        val context = itemView.context

        itemView.findViewById<TextView>(R.id.usage).apply {
            text = item.usage
            val drawable = ContextCompat.getDrawable(context, item.icon)
            setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        }

        itemView.findViewById<TextView>(R.id.description_short).text =
            context.getString(item.shortDescription)

        val textViewLongDescription = itemView.findViewById<TextView>(R.id.description_long)
        val longDesc = item.longDescription
        if (longDesc != null) {
            textViewLongDescription.text = context.getString(longDesc)
            textViewLongDescription.visibility = View.VISIBLE
        } else {
            textViewLongDescription.visibility = View.GONE
        }

        // Required permissions
        val permReqTitle = itemView.findViewById<TextView>(R.id.permissions_required_title)
        val permReqList = itemView.findViewById<LinearLayout>(R.id.permissions_required_list)
        setupPermissionsList(activity, permReqTitle, permReqList, item.requiredPermissions)

        // Optional permissions
        val permOptTitle = itemView.findViewById<TextView>(R.id.permissions_optional_title)
        val permOptList = itemView.findViewById<LinearLayout>(R.id.permissions_optional_list)
        setupPermissionsList(activity, permOptTitle, permOptList, item.optionalPermissions)

        val testButton = itemView.findViewById<Button>(R.id.button_test_command)
        if (item.keyword == "delete") {
            testButton.visibility = View.GONE
        } else {
            testButton.visibility = View.VISIBLE
            testButton.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    val triggerWord = SettingsRepository.getInstance(context).get(Settings.SET_FMD_COMMAND) as String

                    // Simple split to get default required parameters if any
                    val commandArgs = item.usage.split(" ")
                    var rawCommand = "$triggerWord ${item.keyword}"
                    if (commandArgs.size > 1 && commandArgs[1].startsWith("[")) {
                        rawCommand += " " + commandArgs[1].replace("[", "").replace("]", "")
                    } else if (item.keyword == "locate") {
                        rawCommand += " gps"
                    }

                    val handler = CommandHandler(InAppTransport(context))
                    handler.execute(context, rawCommand)
                }
            }
        }
    }
}
