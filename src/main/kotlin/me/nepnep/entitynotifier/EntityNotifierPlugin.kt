package me.nepnep.entitynotifier

import com.lambda.client.plugin.api.Plugin

object EntityNotifierPlugin : Plugin() {
    override fun onLoad() {
        modules.add(EntityNotifier)
    }
}