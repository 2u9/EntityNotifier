package me.nepnep.donkeynotifier

import com.lambda.client.plugin.api.Plugin

object DonkeyNotifierPlugin : Plugin() {
    override fun onLoad() {
        modules.add(DonkeyNotifier)
    }
}