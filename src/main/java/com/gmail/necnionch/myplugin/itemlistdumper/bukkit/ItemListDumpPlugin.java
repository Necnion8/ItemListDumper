package com.gmail.necnionch.myplugin.itemlistdumper.bukkit;

import com.gmail.necnionch.myplugin.itemlistdumper.bukkit.gui.Panel;
import com.google.common.collect.Maps;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ItemListDumpPlugin extends JavaPlugin implements Listener {
    public static final String PERMISSION = "itemlistdump.command.dumpitemlist";
    private final NamespacedKey itemId = new NamespacedKey(this, "id");

    @Override
    public void onEnable() {
        Panel.OWNER = this;
        Objects.requireNonNull(getCommand("dumpitemlist"))
                .setExecutor(createCommand());
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::itemDisplayTask, 0, 20);
    }

    @Override
    public void onDisable() {
        Panel.destroyAll();
    }

    private SimpleCommand createCommand() {
        SimpleCommand command = new SimpleCommand();
        command.subcommands().put("givebook", this::onGiveBookCommand);
        command.subcommands().put("clear", this::onClearCommand);
        command.subcommands().put("importchest", this::onImportChestCommand);
        command.subcommands().put("dump", this::onDumpCommand);
        return command;
    }

    private void itemDisplayTask() {
        if (getServer().getOnlinePlayers().isEmpty())
            return;

        BaseComponent[] label = new ComponentBuilder("左クリック: ").color(ChatColor.GRAY)
                .append("選択画面 ").color(ChatColor.YELLOW)
                .append("|").color(ChatColor.WHITE)
                .append(" シフト右クリ: ").color(ChatColor.GRAY)
                .append("フォーマット編集").color(ChatColor.YELLOW)
                .create();

        getServer().getOnlinePlayers().forEach(p -> {
            if (p.hasPermission(PERMISSION) && isConfigItem(p.getInventory().getItemInMainHand())) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, label);
            }
        });
    }


    private boolean onGiveBookCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            ((Player) sender).getInventory().addItem(createConfigItem());
        }
        return true;
    }

    private boolean onClearCommand(CommandSender sender, Command command, String label, String[] args) {
        return true;
    }

    private boolean onImportChestCommand(CommandSender sender, Command command, String label, String[] args) {
        return true;
    }

    private boolean onDumpCommand(CommandSender sender, Command command, String label, String[] args) {
        return true;
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isConfigItem(player.getInventory().getItemInMainHand()))
            return;

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "このアイテムを使用する権限がありません");
            return;
        }

        if (Action.LEFT_CLICK_AIR.equals(event.getAction()) || Action.LEFT_CLICK_BLOCK.equals(event.getAction())) {
            onLeftClick(player);
        } else if (Action.RIGHT_CLICK_AIR.equals(event.getAction()) || Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) {
            // open writable book
            event.setUseItemInHand(Event.Result.ALLOW);
            event.setCancelled(false);
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "フォーマット設定について");
            player.sendMessage("");
            player.sendMessage(ChatColor.WHITE + "・アイテムIDをフォーマットする文字列を本に入力してください");
            player.sendMessage(ChatColor.WHITE + "・{material} が Material名 に、{item} で Minecraft ID に置換されます");
            player.sendMessage("");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBook(PlayerEditBookEvent event) {
        BookMeta bookMeta = event.getNewBookMeta();
        if (!isConfigItem(bookMeta.getPersistentDataContainer()))
            return;

        Player player = event.getPlayer();
        ListSelector selector = getListSelector(player);

        selector.setLineFormatter(bookMeta.getPage(1));
        if (event.isSigning()) {
            selector.printList(player);
        } else {
            player.sendMessage("フォーマット設定を反映しました");
        }
    }

    private void onLeftClick(Player player) {
        getListSelector(player).openListEditor(player);
    }


    public ItemStack createConfigItem() {
        ItemStack itemStack = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta itemMeta = Objects.requireNonNull((BookMeta) itemStack.getItemMeta());
        itemMeta.setDisplayName(ChatColor.GOLD + "アイテムリスト紙");
        itemMeta.getPersistentDataContainer().set(itemId, PersistentDataType.STRING, "config");
        itemMeta.setPages(ListSelector.DEFAULT_LINE_FORMATTER);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isConfigItem(ItemStack itemStack) {
        return Optional.ofNullable(itemStack)
                .map(ItemStack::getItemMeta)
                .map(PersistentDataHolder::getPersistentDataContainer)
                .map(data -> "config".equalsIgnoreCase(data.get(itemId, PersistentDataType.STRING)))
                .orElse(false);
    }

    public boolean isConfigItem(PersistentDataContainer container) {
        return Optional.ofNullable(container)
                .map(data -> "config".equalsIgnoreCase(data.get(itemId, PersistentDataType.STRING)))
                .orElse(false);
    }

    public NamespacedKey getItemId() {
        return itemId;
    }


    private final Map<UUID, ListSelector> listSelectors = Maps.newHashMap();

    public ListSelector getListSelector(Player player) {
        if (listSelectors.containsKey(player.getUniqueId()))
            return listSelectors.get(player.getUniqueId());
        ListSelector selector = new ListSelector();
        listSelectors.put(player.getUniqueId(), selector);
        return selector;
    }

}
