package me.nepnep.donkeynotifier

import com.lambda.client.event.events.PacketEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.event.listener.listener
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.entity.passive.EntityDonkey
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketSpawnMob
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.registry.EntityEntry
import net.minecraftforge.registries.RegistryManager

object DonkeyNotifier : PluginModule(
    name = "DonkeyNotifier",
    category = Category.MISC,
    description = "Notifies you about donkeys",
    pluginMain = DonkeyNotifierPlugin
) {

    private val playSound by setting("Play sound", true)
    private val sendMessage by setting("Send message", true)
    private val notifyHorse by setting("Notify horses", false)

    init {
        listener<PacketEvent.PostReceive> {
            val packet = it.packet
            if (packet is SPacketSpawnMob) {
                val clazz = RegistryManager.VANILLA
                    .getRegistry<EntityEntry>(ResourceLocation("minecraft", "entities"))
                    .getValue(packet.entityType)
                    .entityClass
                if (clazz.isAssignableFrom(EntityDonkey::class.java)
                    || (notifyHorse && clazz.isAssignableFrom(EntityHorse::class.java))) {
                    if (playSound) {
                        mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_DONKEY_DEATH, 1.0f, 1.0f))
                    }
                    if (sendMessage) {
                        MessageSendHelper.sendChatMessage("Donkey/Horse loaded at ${packet.x} ${packet.y} ${packet.z}")
                    }
                }
            }
        }
    }
}