package ru.joutak.adhd.game.concrete.casino

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CasinoMenu {
    const val TITLE_COLOR_MENU = "Выберите цвет"
    const val TITLE_AMOUNT = "Выберите сумму"

    const val SLOT_RED = 11
    const val SLOT_GREEN = 13
    const val SLOT_BLACK = 15
    const val SLOT_BALANCE = 22

    const val SLOT_AMOUNT_10 = 10
    const val SLOT_AMOUNT_25 = 12
    const val SLOT_AMOUNT_50 = 14
    const val SLOT_AMOUNT_ALL = 16
    const val SLOT_BACK = 22


    fun amountForSlot(slot: Int, balance: Int): Int? = when (slot) {
        SLOT_AMOUNT_10  -> (balance * 10 / 100).coerceAtLeast(1)
        SLOT_AMOUNT_25  -> (balance * 25 / 100).coerceAtLeast(1)
        SLOT_AMOUNT_50  -> (balance * 50 / 100).coerceAtLeast(1)
        SLOT_AMOUNT_ALL -> balance
        else -> null
    }

    fun makeOpenButton() : ItemStack = button(
        Material.NETHER_STAR,
        "Сделать ставку",
        listOf(
            "ПКМ - открыть меню",
            "",
            "Выбирай цвет и сумму!"
        )
    )

    fun openColorMenu(player: Player, balance: Int, goal: Int){
        val inv = Bukkit.createInventory(null, 27, Component.text(TITLE_COLOR_MENU))

        inv.setItem(SLOT_RED, button(
            Material.RED_CONCRETE,
            "Красное",
            listOf("Шанс ~48%", "Выплата x2", "", "Клик - выбрать")
        ))
        inv.setItem(SLOT_GREEN, button(
            Material.GREEN_CONCRETE,
            "Зелёное",
            listOf("Шанс ~2%", "Выплата x35", "", "Клик - выбрать")
        ))
        inv.setItem(SLOT_BLACK, button(
            Material.BLACK_CONCRETE,
            "Чёрное",
            listOf("Шанс ~48%", "Выплата x2", "", "Клик - выбрать")
        ))
        inv.setItem(SLOT_BALANCE, button(
            Material.GOLD_INGOT,
            "Баланс: $balance",
            listOf("Цель: $goal", "", "Делай ставку, деньги сами себя не заработают!")
        ))

        player.openInventory(inv)
    }

    fun openAmountMenu(player: Player, color: String, balance: Int){
        val inv = Bukkit.createInventory(null, 27, Component.text(TITLE_AMOUNT))

        val colorMat = when (color) {
            "красное" -> Material.RED_CONCRETE
            "черное" -> Material.BLACK_CONCRETE
            else -> Material.GREEN_CONCRETE
        }
        inv.setItem(4, button(colorMat, "Ставка на $color", listOf("Баланс: $balance")))

        val bets = listOf(
            10 to SLOT_AMOUNT_10,
            25 to SLOT_AMOUNT_25,
            50 to SLOT_AMOUNT_50,
            -1 to SLOT_AMOUNT_ALL,
        )


        for ((percent,slot) in bets) {
            val isAllin = percent == -1
            val realAmount = if (isAllin) balance else (balance * percent / 100).coerceAtLeast(1)
            val canAfford = realAmount <= balance && realAmount > 0

            val mat = if (canAfford) Material.LIME_DYE else Material.GRAY_DYE
            val name = if (isAllin) "ОЛЛ ИН" else "$percent%"
            val lore = if (canAfford)
                listOf("Поставить $realAmount", "", "Клик - поставить")
            else
                listOf("Недостаточно монет")

            inv.setItem(slot, button(mat, name, lore))
        }

        inv.setItem(22, button(Material.ARROW, "<- Назад", listOf("К выбору цвета")))
        player.openInventory(inv)
    }

    private fun button(mat: Material, name: String, lore: List<String>): ItemStack{
        val item = ItemStack(mat)
        val meta = item.itemMeta
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false))
        meta.lore(lore.map { Component.text(it).decoration(TextDecoration.ITALIC, false) })
        item.itemMeta = meta
        return item

    }
}