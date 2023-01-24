package com.gmail.necnionch.myplugin.itemlistdumper.bukkit;

import com.gmail.necnionch.myplugin.itemlistdumper.bukkit.gui.Panel;
import com.google.common.collect.Maps;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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
        command.subcommands().put("importchestset", this::onImportChestSetCommand);
        command.subcommands().put("importchestunset", this::onImportChestUnsetCommand);
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
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;
        ListSelector selector = getListSelector(player);
        if (selector == null) {
            player.sendMessage(ChatColor.RED + "リストがありません");
        } else if (selector.getListedCount() <= 0) {
            player.sendMessage(ChatColor.RED + "1つも選択されていません");
        } else {
            selector.clearList();
            player.sendMessage(ChatColor.GOLD + "リストを空にしました");
        }
        return true;

    }

    private boolean onImportChestSetCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;

        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlockExact(4);

        if (targetBlock == null ||  !(targetBlock.getState() instanceof Container)) {
            player.sendMessage(ChatColor.RED + "チェストに視点を合わせてから実行してください");
            return true;
        }

        ListSelector selector = makeListSelector(player);
        Container container = (Container) targetBlock.getState();

        //noinspection ConstantConditions
        long added = Stream.of(container.getInventory().getContents())
                .filter(Objects::nonNull)
                .map(ItemStack::getType)
                .distinct()
                .filter(type -> !Material.AIR.equals(type))
                .filter(typ -> {
                    try {
                        if (!selector.getListSetting().types().contains(typ)) {
                            selector.getListSetting().types().add(typ);
                            return true;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .count();

        player.sendMessage(ChatColor.GOLD + "アイテムタイプ " + added + "個 がリストに追加されました。");
        return true;
    }

    private boolean onImportChestUnsetCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;

        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlockExact(4);

        if (targetBlock == null ||  !(targetBlock.getState() instanceof Container)) {
            player.sendMessage(ChatColor.RED + "チェストに視点を合わせてから実行してください");
            return true;
        }

        ListSelector selector = makeListSelector(player);
        Container container = (Container) targetBlock.getState();

        //noinspection ConstantConditions
        long removed = Stream.of(container.getInventory().getContents())
                .filter(Objects::nonNull)
                .map(ItemStack::getType)
                .distinct()
                .filter(type -> !Material.AIR.equals(type))
                .filter(typ -> selector.getListSetting().types().remove(typ))
                .count();

        player.sendMessage(ChatColor.GOLD + "アイテムタイプ " + removed + "個 がリストから削除されました。");
        return true;
    }

    private boolean onDumpCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;
        ListSelector selector = getListSelector(player);
        if (selector == null) {
            player.sendMessage(ChatColor.RED + "リストがありません");
        } else if (selector.getListedCount() <= 0) {
            player.sendMessage(ChatColor.RED + "1つも選択されていません");
        } else {
            player.sendMessage(ChatColor.WHITE + "リストを書き出しました");
            selector.printList(player);
        }
        return true;
    }


    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!EquipmentSlot.HAND.equals(event.getHand())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (!isConfigItem(handItem))
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
            if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof Container && !player.isSneaking()) {
                // open container
                event.setCancelled(false);
                return;
            } else if (Material.WRITABLE_BOOK.equals(handItem.getType())) {
                // open writable book
                player.sendMessage("");
                player.sendMessage(ChatColor.YELLOW + "フォーマット設定について");
                player.sendMessage("");
                player.sendMessage(ChatColor.WHITE + "・アイテムIDをフォーマットする文字列を本に入力してください");
                player.sendMessage(ChatColor.WHITE + "・{pid} が Material名 に、{mid} で Minecraft ID に置換されます");
                player.sendMessage("");
            }

            if (handItem.getItemMeta() instanceof BookMeta) {
                BookMeta bookMeta = (BookMeta) handItem.getItemMeta();
                if (bookMeta.hasPages())
                    makeListSelector(player).setLineFormatter(bookMeta.getPage(1));
            }

        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBook(PlayerEditBookEvent event) {
        BookMeta bookMeta = event.getNewBookMeta();
        if (!isConfigItem(bookMeta.getPersistentDataContainer()))
            return;

        Player player = event.getPlayer();
        ListSelector selector = makeListSelector(player);

        selector.setLineFormatter(bookMeta.getPage(1));
        if (event.isSigning()) {
            selector.printList(player);
        } else {
            player.sendMessage("フォーマット設定を反映しました");
            if (selector.getListedCount() > 0)
                selector.printList(player);
        }
    }

    private void onLeftClick(Player player) {
        makeListSelector(player).openListEditor(player);
        player.spigot().sendMessage(new ComponentBuilder("リストを書き出すには ").color(ChatColor.GOLD)
                .append("/dumpitemlist dump").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dumpitemlist dump")).color(ChatColor.YELLOW)
                .append(" を実行します", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GOLD).create());
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

    public ListSelector makeListSelector(Player player) {
        if (listSelectors.containsKey(player.getUniqueId()))
            return listSelectors.get(player.getUniqueId());
        ListSelector selector = new ListSelector();
        listSelectors.put(player.getUniqueId(), selector);
        return selector;
    }

    public @Nullable ListSelector getListSelector(Player player) {
        if (listSelectors.containsKey(player.getUniqueId()))
            return listSelectors.get(player.getUniqueId());
        return null;
    }

}
