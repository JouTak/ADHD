package ru.joutak.adhd

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.listener.LobbyListener
import ru.joutak.adhd.listener.PlayerSessionListener
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.minigames.MiniGamesCore
import ru.joutak.minigames.managers.MatchmakingManager

class ADHDPlugin : JavaPlugin() {
    companion object {
        @JvmStatic
        lateinit var instance: ADHDPlugin
    }

    /**
     * Plugin startup logic
     */
    override fun onEnable() {
        instance = this

        MiniGamesCore.initialize(this)

        ADHDConfig.load()

        TournamentManager.load()

        Bukkit.getPluginManager().registerEvents(PlayerSessionListener(), instance)
        Bukkit.getPluginManager().registerEvents(LobbyListener(), instance)

        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.event.pvp.RespawnListener(), instance)
        Bukkit.getPluginManager().registerEvents(ru.joutak.adhd.event.pillars.RespawnListener(), instance)

        logger.info("Плагин ${pluginMeta.name} версии ${pluginMeta.version} включен!")

        Bukkit.getScheduler().runTaskTimer(instance, Runnable {
            val gInstance = MatchmakingManager.pollReady()

            if (gInstance != null) {
                TournamentManager.createTournament(gInstance)
            }
        }, 20L, 20L)
    }

    /**
     * Plugin shutdown logic
     */
    override fun onDisable() {
        TournamentManager.shutdown()
    }
}
