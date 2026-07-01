package ru.joutak.adhd

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import ru.joutak.adhd.listener.LobbyListener
import ru.joutak.adhd.listener.PlayerSessionListener
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.minigames.MiniGamesCore
import ru.joutak.minigames.managers.MatchmakingManager
import java.io.File

class ADHDPlugin : JavaPlugin() {
    companion object {
        @JvmStatic
        lateinit var instance: ADHDPlugin
    }

    private var customConfig = YamlConfiguration()

    private fun loadConfig() {
        val fx = File(dataFolder, "config.yml")
        if (!fx.exists()) {
            saveResource("config.yml", true)
        }
    }

    /**
     * Plugin startup logic
     */
    override fun onEnable() {
        instance = this

        MiniGamesCore.initialize(this)

        loadConfig()

        TournamentManager.load()

        Bukkit.getPluginManager().registerEvents(PlayerSessionListener(), instance)
        Bukkit.getPluginManager().registerEvents(LobbyListener(), instance)

        logger.info("Плагин ${pluginMeta.name} версии ${pluginMeta.version} включен!")

        Bukkit.getScheduler().runTaskTimer(instance, Runnable {
            val gInstance = MatchmakingManager.pollReady()

            if (gInstance != null) {
                instance.logger.info("Ready!")
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
