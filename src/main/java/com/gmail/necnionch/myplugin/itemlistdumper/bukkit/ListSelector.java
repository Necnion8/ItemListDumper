package com.gmail.necnionch.myplugin.itemlistdumper.bukkit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ListSelector {
    public static final String DEFAULT_LINE_FORMATTER = "{pid},";
    private ListEditorPanel.Setting listSetting = ListEditorPanel.Setting.createDefault();
    private String lineFormatter = DEFAULT_LINE_FORMATTER;


    public void openListEditor(Player player) {
        new ListEditorPanel(player, listSetting).open();
    }


    public ListEditorPanel.Setting getListSetting() {
        return listSetting;
    }

    public void setListSetting(ListEditorPanel.Setting setting) {
        this.listSetting = Optional.ofNullable(setting).orElseGet(ListEditorPanel.Setting::createDefault);
    }

    public @Nullable String getLineFormatter() {
        return lineFormatter;
    }

    public void setLineFormatter(@Nullable String formatter) {
        this.lineFormatter = formatter;
    }

    public List<Material> getListed() {
        return listSetting.types();
    }

    public int getListedCount() {
        return listSetting.types().size();
    }

    public void clearList() {
        listSetting.types().clear();
    }

    public void printList(Player player) {
        String dumpList = dumpList();
        player.spigot().sendMessage(new ComponentBuilder(
                "このメッセージをクリックするとクリップボードにコピーされます").color(ChatColor.GOLD)
                .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dumpList))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] {new TextComponent("クリックしてコピー")}))
                .create());
    }

    public String dumpList() {
        String fmt = Optional.ofNullable(lineFormatter).orElse(DEFAULT_LINE_FORMATTER);
        if (fmt.contains("\n") && !fmt.endsWith("\n"))
            fmt += "\n";
        String format = fmt;

        return listSetting.types()
                .stream()
                .map(typ -> format
                        .replace("{pid}", typ.name())
                        .replace("{mid}", typ.getKey().toString())
                        .replace("{id}", typ.name()))
                .collect(Collectors.joining());
    }

}
