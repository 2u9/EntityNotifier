package me.nepnep.entitynotifier

import com.lambda.client.event.events.PacketEvent
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.TickTimer
import com.lambda.client.util.TimeUnit
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.safeListener
import com.lambda.event.listener.listener
import net.minecraft.block.Block
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.passive.*
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.server.SPacketSpawnMob
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.fml.common.gameevent.TickEvent
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
    private val deadBodies by setting("Dead Bodies", false)
    private val deadBodyAmount by setting("Dead Body Item Amount", 5, 1..20, 1, { deadBodies })

    private val timer = TickTimer(TimeUnit.SECONDS)
    private val items = mutableListOf<EntityItem>()

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

        // Can't use EntityJoinWorldEvent because classes from different classloaders cannot be event listeners in forge's bus
        safeListener<TickEvent.ClientTickEvent> { event ->
            if (event.phase != TickEvent.Phase.START || !deadBodies || !timer.tick(1)) {
                return@safeListener
            }

            val loadedEntities = world.loadedEntityList
            var notified = false
            for (entity in loadedEntities) {
                if (entity is EntityItem && !items.contains(entity)) {
                    items.add(entity)

                    if (notified) {
                        break
                    }

                    val posX = entity.posX
                    val posY = entity.posY
                    val posZ = entity.posZ
                    val aabb = AxisAlignedBB(posX - 2, posY - 2, posZ - 2, posX + 2, posY + 2, posZ + 2)
                    val amount = world.getEntitiesWithinAABB(EntityItem::class.java, aabb) {
                        val item = it?.item?.item ?: return@getEntitiesWithinAABB false // Null checks may not be necessary
                        item == Items.DIAMOND_BOOTS
                            || item == Items.DIAMOND_CHESTPLATE
                            || item == Items.DIAMOND_HELMET
                            || item == Items.DIAMOND_LEGGINGS
                            || item == Items.DIAMOND_PICKAXE
                            || item == Items.DIAMOND_AXE
                            || item == Items.DIAMOND_SHOVEL
                            || item == Items.DIAMOND_HOE
                            || item == Items.DIAMOND_SWORD
                            || item == Items.ELYTRA
                            || item == Items.TOTEM_OF_UNDYING
                            || (item is ItemBlock && item.block.isShulkerBox())
                    }.size
                    if (amount >= deadBodyAmount) {
                        notified = true
                        mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f))
                        MessageSendHelper.sendChatMessage("Dead body with $amount valuable items found at $posX $posY $posZ")
                    }
                }
            }

            items.removeIf { !loadedEntities.contains(it) }
        }
    }

    private fun Class<out Entity>.shouldNotify() = when (this) {
        EntityDonkey::class.java -> donkeys
        EntityHorse::class.java -> horses
        EntityGhast::class.java -> ghasts
        EntitySheep::class.java -> sheep
        EntityCow::class.java -> cows
        EntityVillager::class.java -> villagers
        else -> false
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

    private fun Block.isShulkerBox(): Boolean {
        return this == Blocks.WHITE_SHULKER_BOX
            || this == Blocks.ORANGE_SHULKER_BOX
            || this == Blocks.MAGENTA_SHULKER_BOX
            || this == Blocks.LIGHT_BLUE_SHULKER_BOX
            || this == Blocks.YELLOW_SHULKER_BOX
            || this == Blocks.LIME_SHULKER_BOX
            || this == Blocks.PINK_SHULKER_BOX
            || this == Blocks.GRAY_SHULKER_BOX
            || this == Blocks.SILVER_SHULKER_BOX
            || this == Blocks.CYAN_SHULKER_BOX
            || this == Blocks.PURPLE_SHULKER_BOX
            || this == Blocks.BLUE_SHULKER_BOX
            || this == Blocks.BROWN_SHULKER_BOX
            || this == Blocks.GREEN_SHULKER_BOX
            || this == Blocks.RED_SHULKER_BOX
            || this == Blocks.BLACK_SHULKER_BOX
    }
}