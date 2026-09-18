package ru.joutak.adhd.listener.mode.ricochet_arena

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import ru.joutak.adhd.game.concrete.RicochetArenaGame
import ru.joutak.adhd.tournament.TournamentManager

class FireListener : Listener {

    private val bulletKey = NamespacedKey("adhd", "ricochet_bullet")

    @EventHandler
    fun onLapisRightClick(event: PlayerInteractEvent) {

        val player = event.player

        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }
        val item = event.item ?: return

        val game = TournamentManager.getGame(player) as? RicochetArenaGame ?: return

        if (item.type != Material.LAPIS_LAZULI) return

        event.isCancelled = true

        val meta = game.gameMeta ?: return

        if (player.hasCooldown(Material.LAPIS_LAZULI)) return

        val cooldownTicks = meta.cooldownTicks

        val projectileSpeed = meta.projectileSpeed

        val maxBounces = meta.maxBounces

        player.setCooldown(Material.LAPIS_LAZULI, cooldownTicks)

        val projectile = player.launchProjectile(Snowball::class.java)

        game.activeProjectiles.add(projectile)

        projectile.setGravity(false)

        projectile.velocity = player.location.direction.multiply(projectileSpeed)

        projectile.shooter = player
        projectile.persistentDataContainer.set(bulletKey, PersistentDataType.INTEGER, maxBounces)

        val lifetimeTicks = meta.lifetimeTicks

        object : BukkitRunnable() {
            override fun run() {
                if (projectile.isValid) {
                    projectile.remove()
                }
            }
        }.runTaskLater(ru.joutak.adhd.ADHDPlugin.instance, lifetimeTicks)
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {

        val snowball = event.entity as? Snowball ?: return

        val shooter = snowball.shooter as? Player ?: return

        val game = TournamentManager.getGame(shooter) as? RicochetArenaGame ?: return

        val container = snowball.persistentDataContainer

        if (!container.has(bulletKey, PersistentDataType.INTEGER)) return

        val velocity = snowball.velocity

        val meta = game.gameMeta ?: return

        val hitPlayer = event.hitEntity as? Player
        if (hitPlayer != null && hitPlayer.uniqueId in game.members) {

            snowball.remove()

            val damageAmount = meta.projectileDamage

            hitPlayer.damage(damageAmount, shooter)

            shooter.playSound(
                shooter.location,
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.5f,
                1.8f
            )

            if (hitPlayer.isDead || hitPlayer.health <= 0.0) {
                game.calculateResult(hitPlayer)
                game.finish()
            }
            return
        }

        val block = event.hitBlock ?: return
        val face = event.hitBlockFace ?: return

        val amountRicochet = container.get(bulletKey, PersistentDataType.INTEGER) ?: 0

        if (amountRicochet == 0) return

        val spawnLocation = snowball.location.add(face.direction.multiply(0.2))

        snowball.remove()

        val reflectedVelocity: Vector = when (face) {
            BlockFace.EAST, BlockFace.WEST -> {
                Vector(-velocity.x, velocity.y, velocity.z)
            }
            BlockFace.UP, BlockFace.DOWN -> {
                Vector(velocity.x, -velocity.y, velocity.z)
            }
            BlockFace.NORTH, BlockFace.SOUTH -> {
                Vector(velocity.x, velocity.y, -velocity.z)
            }
            else -> {
                velocity
            }
        }

        val newSnowball = snowball.world.spawn(spawnLocation, Snowball::class.java)
        newSnowball.setGravity(false)
        newSnowball.persistentDataContainer.set(bulletKey, PersistentDataType.INTEGER, amountRicochet-1)

        newSnowball.velocity = reflectedVelocity
        newSnowball.shooter = shooter

        game.activeProjectiles.add(newSnowball)

        val lifetimeTicks = meta.lifetimeTicks

        object : BukkitRunnable() {
            override fun run() {
                if (newSnowball.isValid) {
                    newSnowball.remove()
                }
            }
        }.runTaskLater(ru.joutak.adhd.ADHDPlugin.instance, lifetimeTicks)
    }
}