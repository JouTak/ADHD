package ru.joutak.adhd.listener.mode.ricochet_arena

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector

class FireListener : Listener {

    private val bulletKey = NamespacedKey("adhd", "ricochet_bullet")

    private val maxBounces = 3;

    @EventHandler
    fun onLapisRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val item = event.item ?: return
        if (item.type == Material.LAPIS_LAZULI) {
            // Отменяем ванильное действие (например, чтобы лазурит случайно не засунули в чародейский стол)
            event.isCancelled = true

            val player = event.player
            player.sendMessage("§9[Ricochet] Вы активировали способность лазурита!")

            // Здесь будет код вашей кастомной механики для Ricochet Arena
            val projectile = player.launchProjectile(Snowball::class.java)

            projectile.velocity = player.location.direction.multiply(2.0)
            //Bukkit.getLogger().info("Вектор скорости: ${player.location.direction.multiply(2.0)}")

            projectile.shooter = player
            projectile.persistentDataContainer.set(bulletKey, PersistentDataType.INTEGER, 3)

        }
    }
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        // 1. Проверяем, что летит именно снежок
        val snowball = event.entity as? Snowball ?: return

        // 2. Проверяем наличие вашего кастомного тега
        val container = snowball.persistentDataContainer

        if (!container.has(bulletKey, PersistentDataType.INTEGER)) return

        // 3. Проверяем, что снаряд врезался именно в блок (стену, пол или потолок)
        val block = event.hitBlock ?: return
        val face = event.hitBlockFace ?: return

        val velocity = snowball.velocity

        val shooter = snowball.shooter

        val amount_ricochet = container.get(bulletKey, PersistentDataType.INTEGER) ?: 0

        if (amount_ricochet == 0) return
        Bukkit.getLogger().info("Количество рикошета: ${amount_ricochet}")

        val spawnLocation = snowball.location.add(face.direction.multiply(0.2))

        snowball.remove()

        val reflectedVelocity: Vector = when (face) {
            org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST -> {
                Vector(-velocity.x, velocity.y, velocity.z)
            }
            org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN -> {
                Vector(velocity.x, -velocity.y, velocity.z)
            }
            org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH -> {
                Vector(velocity.x, velocity.y, -velocity.z)
            }

            else -> {
                Bukkit.getLogger().info("connect else")
                velocity
            }
        }

        val newSnowball = snowball.world.spawn(spawnLocation, Snowball::class.java)
        newSnowball.persistentDataContainer.set(bulletKey, PersistentDataType.INTEGER, amount_ricochet-1)

        newSnowball.velocity = reflectedVelocity
        newSnowball.shooter = shooter
    }
}