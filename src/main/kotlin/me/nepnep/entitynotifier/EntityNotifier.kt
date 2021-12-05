package me.nepnep.entitynotifier

import com.lambda.client.event.events.PacketEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.event.listener.listener
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.passive.*
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketSpawnMob
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.registry.EntityEntry
import net.minecraftforge.registries.RegistryManager
import org.apache.logging.log4j.LogManager

object EntityNotifier : PluginModule(
    name = "EntityNotifier",
    category = Category.MISC,
    description = "Notifies you about entities",
    pluginMain = EntityNotifierPlugin
) {

    private val playSound by setting("Play sound", true)
    private val sendMessage by setting("Send message", true)
    private val donkeys by setting("Donkeys", true)
    private val horses by setting("Horses", false)
    private val ghasts by setting("Ghasts", false)
    private val sheep by setting("Sheep", false)
    private val cows by setting("Cows", false)
    private val villagers by setting("Villagers", false)

    init {
        listener<PacketEvent.PostReceive> {
            val packet = it.packet
            if (packet is SPacketSpawnMob) {
                val entityEntry = RegistryManager.VANILLA
                    .getRegistry<EntityEntry>(ResourceLocation("minecraft", "entities"))
                    .getValue(packet.entityType)
                val clazz = entityEntry.entityClass
                if (clazz.shouldNotify()) {
                    if (playSound) {
                        mc.soundHandler.playSound(clazz.getSound())
                    }
                    if (sendMessage) {
                        MessageSendHelper.sendChatMessage("${entityEntry.name} loaded at ${packet.x} ${packet.y} ${packet.z}")
                    }
                }
            }
        }
    }

    private fun Class<out Entity>.shouldNotify(): Boolean {
        return (donkeys && this == EntityDonkey::class.java)
            || (horses && this == EntityHorse::class.java)
            || (ghasts && this == EntityGhast::class.java)
            || (sheep && this == EntitySheep::class.java)
            || (cows && this == EntityCow::class.java)
            || (villagers && this == EntityVillager::class.java)
    }

    private fun Class<out Entity>.getSound(): PositionedSoundRecord {
        val event = when (this) {
            EntityDonkey::class.java -> SoundEvents.ENTITY_DONKEY_DEATH
            EntityHorse::class.java -> SoundEvents.ENTITY_HORSE_DEATH
            EntityGhast::class.java -> SoundEvents.ENTITY_GHAST_DEATH
            EntitySheep::class.java -> SoundEvents.ENTITY_SHEEP_DEATH
            EntityCow::class.java -> SoundEvents.ENTITY_SHEEP_DEATH
            EntityVillager::class.java -> SoundEvents.ENTITY_VILLAGER_DEATH
            else -> {
                val error = "Unknown sound for entity $typeName, please report this bug"
                MessageSendHelper.sendErrorMessage(error)
                LogManager.getLogger().error(error)
                SoundEvents.ENTITY_LLAMA_SPIT
            }
        }
        return PositionedSoundRecord.getRecord(event, 1.0f, 1.0f)
    }
}