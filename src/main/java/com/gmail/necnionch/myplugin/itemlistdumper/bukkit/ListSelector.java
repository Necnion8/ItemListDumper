package com.gmail.necnionch.myplugin.itemlistdumper.bukkit;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ListSelector {
    public static final String DEFAULT_LINE_FORMATTER = ",";
    private ListEditorPanel.Setting listSetting;
    private String lineFormatter = DEFAULT_LINE_FORMATTER;


    public void openListEditor(Player player) {
        new ListEditorPanel(player, listSetting).open();
    }


    public ListEditorPanel.Setting getListSetting() {
        return listSetting;
    }

    public void setListSetting(ListEditorPanel.Setting setting) {
        this.listSetting = setting;
    }

    public @Nullable String getLineFormatter() {
        return lineFormatter;
    }

    public void setLineFormatter(@Nullable String formatter) {
        this.lineFormatter = formatter;
    }

    public void printList(Player player) {

    }

    public String dumpList() {
        return "";
    }

}
