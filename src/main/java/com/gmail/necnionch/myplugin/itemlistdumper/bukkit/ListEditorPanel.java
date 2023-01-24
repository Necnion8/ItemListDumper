package com.gmail.necnionch.myplugin.itemlistdumper.bukkit;

import com.gmail.necnionch.myplugin.itemlistdumper.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.itemlistdumper.bukkit.gui.PanelItem;
import com.google.common.collect.Lists;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListEditorPanel extends Panel {
    private final List<Material> listedTypes = Lists.newArrayList();
    private int pageIndex;
    private int maxPageIndex;
    private boolean showBlocks = true;
    private boolean showItems = true;
    private boolean showSelected;
    private Pattern searchQuery;

    public ListEditorPanel(Player player, @Nullable Setting setting) {
        super(player, 9 * 6, "", new ItemStack(Material.AIR));
        if (setting != null) {
            listedTypes.clear();
            listedTypes.addAll(setting.types);
            pageIndex = setting.pageIndex;
            showBlocks = setting.showBlocks;
            showItems = setting.showItems;
            showSelected = setting.showSelected;
            searchQuery = setting.searchQuery;
        }
    }

    public Setting setting() {
        return new Setting(Lists.newArrayList(listedTypes), pageIndex, showBlocks, showItems, showSelected, searchQuery);
    }

    public ListEditorPanel addAll(Collection<Material> types) {
        listedTypes.addAll(types);
        return this;
    }

    public ListEditorPanel clear() {
        listedTypes.clear();
        return this;
    }

    public List<Material> getListedTypes() {
        return listedTypes.stream()
                .sorted((t1, t2) -> t1.getKey().getKey().compareToIgnoreCase(t2.getKey().getKey()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String getTitle() {
        return ChatColor.DARK_AQUA + "ページ: " + (pageIndex + 1) + "/" + (maxPageIndex + 1) + ChatColor.DARK_GRAY + " | " + ChatColor.GOLD + "選択済み: " + listedTypes.size();
    }

    @Override
    public PanelItem[] build() {
        PanelItem[] items = new PanelItem[getSize()];

        List<String> lore = Lists.newArrayList();
        if (searchQuery != null)
            lore.add(ChatColor.GRAY + "現在の設定: " + ChatColor.WHITE + searchQuery);

        items[0] = PanelItem.createItem(Material.HOPPER, ChatColor.GOLD + "IDフィルタ", lore, searchQuery != null)
                .setClickListener(this::openFilterEdit);
        if (searchQuery != null)
            items[1] = PanelItem.createItem(Material.BARRIER, ChatColor.RED + "フィルタをリセット").setClickListener(() -> {
                searchQuery = null;
                saveSetting();
                open();
            });

        items[3] = PanelItem.createItem(Material.CHEST, ChatColor.GOLD + "選択済みのみ表示: " + (
                (showSelected) ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"), null, showSelected).setClickListener(() -> {
            showSelected = !showSelected;
            saveSetting();
            update();
        });

        items[4] = PanelItem.createItem(Material.STONE, ChatColor.GOLD + "ブロック: " + (
                (!showSelected && showBlocks) ? ChatColor.GREEN + "含む" : ChatColor.RED + "含まない"), null, !showSelected && showBlocks).setClickListener(() -> {
            showSelected = false;
            showBlocks = !showBlocks;
            saveSetting();
            update();
        });

        items[5] = PanelItem.createItem(Material.IRON_INGOT, ChatColor.GOLD + "アイテム: " + (
                (!showSelected && showItems) ? ChatColor.GREEN + "含む" : ChatColor.RED + "含まない"), null, !showSelected && showItems).setClickListener(() -> {
            showSelected = false;
            showItems = !showItems;
            saveSetting();
            update();
        });

        Material[] allTypes = Stream.of(Material.values())
                .filter(type -> searchQuery == null || searchQuery.matcher(type.name().toLowerCase(Locale.ROOT)).find() || searchQuery.matcher(type.getKey().getKey()).find())
                .filter(type -> {
                    if (showSelected)
                        return listedTypes.contains(type);
                    if (showItems && showBlocks)
                        return true;
                    if (showItems) {
                        return !type.isBlock();
                    } else if (showBlocks) {
                        return type.isBlock();
                    }
                    return false;
                })
                .toArray(Material[]::new);

        int itemSize = 9 * 5;
        maxPageIndex = allTypes.length / itemSize;
        pageIndex = Math.max(0, Math.min(pageIndex, maxPageIndex));

        String pageLabel = ChatColor.GRAY + "(" + ChatColor.BOLD + (pageIndex + 1) + "/" + (maxPageIndex + 1) + ChatColor.GRAY + ")";
        items[7] = PanelItem.createItem(
                (pageIndex > 0) ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE,
                ((pageIndex > 0) ? ChatColor.AQUA : ChatColor.GRAY + ChatColor.ITALIC.toString()) + "前のページへ " + pageLabel
        ).setClickListener((e, p) -> {
            pageIndex--;
            saveSetting();
            update();
        });

        items[8] = PanelItem.createItem(
                (maxPageIndex > pageIndex) ? Material.RED_DYE : Material.GRAY_DYE,
                ((maxPageIndex > pageIndex) ? ChatColor.RED : ChatColor.GRAY + ChatColor.ITALIC.toString()) + "次のページへ " + pageLabel
        ).setClickListener((e, p) -> {
            pageIndex++;
            saveSetting();
            update();
        });

        for (int i = 0; i < itemSize && pageIndex * itemSize + i < allTypes.length; i++) {
            Material type = allTypes[pageIndex * itemSize + i];
            lore = createItemDescription(type);
            if (listedTypes.contains(type)) {
                lore.add(0, "");
                lore.add(0, ChatColor.GREEN + "選択済み");
            }

            Material icon = type.isItem() ? type : Material.STONE;
            String name = type.isItem() ? null : type.getKey().toString();

            items[9 + i] = PanelItem.createItem(icon, name, lore, listedTypes.contains(type)).setClickListener(() -> {
                if (listedTypes.contains(type)) {
                    listedTypes.remove(type);
                } else {
                    listedTypes.add(type);
                }
                saveSetting();
                update();
            });
        }
        return items;
    }

    public static List<String> createItemDescription(Material type) {
        List<String> lines = Lists.newArrayList(
                ChatColor.GRAY + "Minecraft: " + ChatColor.WHITE + type.getKey(),
                ChatColor.GRAY + "Material: " + ChatColor.WHITE + type.name(),
                ChatColor.GRAY + "最大スタック: " + type.getMaxStackSize()
        );
        if (type.getMaxDurability() > 0)
            lines.add(ChatColor.GRAY + "耐久値: " + type.getMaxDurability());
        return lines;
    }

    private void openFilterEdit() {
        openFilterEdit(null);
    }

    private void openFilterEdit(String title) {
        ItemListDumpPlugin plugin = JavaPlugin.getPlugin(ItemListDumpPlugin.class);
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(Optional.ofNullable(title).orElse("IDを指定"))
                .itemLeft(new ItemStack(Material.PAPER))
                .text(Optional.ofNullable(searchQuery).map(Pattern::pattern).orElse(""))
                .onComplete((complete) -> {
                    if (complete.getText().isEmpty()) {
                        searchQuery = null;
                    } else {
                        try {
                            searchQuery = Pattern.compile(complete.getText());
                        } catch (PatternSyntaxException e) {
                            return Collections.singletonList(AnvilGUI.ResponseAction.run(() ->
                                    openFilterEdit(ChatColor.RED + "正規表現パターンが不正です")));
                        }
                        long count = Stream.of(Material.values())
                                .filter(type -> searchQuery.matcher(type.name().toLowerCase(Locale.ROOT)).find() || searchQuery.matcher(type.getKey().getKey()).find())
                                .count();
                        if (count <= 0) {
                            saveSetting();
                            return Collections.singletonList(AnvilGUI.ResponseAction.run(() ->
                                    openFilterEdit(ChatColor.RED + "見つかりませんでした")));
                        }
                    }
                    saveSetting();
                    return Collections.singletonList(AnvilGUI.ResponseAction.run(this::open));
                })
                .open(getPlayer());

    }

    private void saveSetting() {
        JavaPlugin.getPlugin(ItemListDumpPlugin.class).getListSelector(getPlayer()).setListSetting(setting());
    }

    public static final class Setting {
        private List<Material> types;
        private int pageIndex;
        private boolean showBlocks;
        private boolean showItems;
        private boolean showSelected;
        @Nullable private Pattern searchQuery;

        public static Setting createDefault() {
            return new Setting(Lists.newArrayList(), 0, true, true, false, null);
        }

        public Setting(List<Material> types, int pageIndex, boolean showBlocks, boolean showItems, boolean showSelected, @Nullable Pattern searchQuery) {
            this.types = types;
            this.pageIndex = pageIndex;
            this.showBlocks = showBlocks;
            this.showItems = showItems;
            this.showSelected = showSelected;
            this.searchQuery = searchQuery;
        }

        public List<Material> getTypes() {
            return types;
        }

        public int getPageIndex() {
            return pageIndex;
        }

        public boolean isShowBlocks() {
            return showBlocks;
        }

        public boolean isShowItems() {
            return showItems;
        }

        public boolean isShowSelected() {
            return showSelected;
        }

        public @Nullable Pattern getSearchQuery() {
            return searchQuery;
        }

        public void setTypes(List<Material> types) {
            this.types = types;
        }

        public void setPageIndex(int pageIndex) {
            this.pageIndex = pageIndex;
        }

        public void setShowBlocks(boolean showBlocks) {
            this.showBlocks = showBlocks;
        }

        public void setShowItems(boolean showItems) {
            this.showItems = showItems;
        }

        public void setShowSelected(boolean showSelected) {
            this.showSelected = showSelected;
        }

        public void setSearchQuery(@Nullable Pattern searchQuery) {
            this.searchQuery = searchQuery;
        }
    }
}
