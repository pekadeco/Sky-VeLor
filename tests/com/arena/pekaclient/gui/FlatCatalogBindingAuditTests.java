package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.List;

/** Exercises every flattened persisted id through its established inline binding. */
public final class FlatCatalogBindingAuditTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        everyToggleSliderAndChoiceStillWritesItsOriginalOwner();
        chatTextFieldsRemainScreenOwnedAndAddressable();
        done();
    }

    private static void everyToggleSliderAndChoiceStillWritesItsOriginalOwner() {
        for (ReferenceSettingsCatalog.Setting setting : configurableSettings()) {
            if (isScreenOwnedCustomTab(setting.id)) continue;
            if (setting.control == ReferenceSettingsCatalog.Control.TOGGLE
                    || (setting.control == ReferenceSettingsCatalog.Control.EDITOR && setting.hasMasterToggle())) {
                boolean before = InlineSettingsBindings.booleanValue(setting.id);
                InlineSettingsBindings.setBoolean(setting.id, true);
                check("toggle binding accepts flattened id: " + setting.id, InlineSettingsBindings.booleanValue(setting.id));
                InlineSettingsBindings.setBoolean(setting.id, before);
            } else if (setting.control == ReferenceSettingsCatalog.Control.SLIDER) {
                int before = InlineSettingsBindings.intValue(setting.id);
                InlineSettingsBindings.setInt(setting.id, setting.max);
                check("slider binding accepts flattened id: " + setting.id,
                        InlineSettingsBindings.intValue(setting.id) == setting.max);
                InlineSettingsBindings.setInt(setting.id, before);
            } else if (setting.control == ReferenceSettingsCatalog.Control.CHOICE) {
                if ("chat-tab-filter-type".equals(setting.id)) continue; // selected custom tab is screen-owned.
                int before = InlineSettingsBindings.choiceValue(setting.id);
                int target = Math.max(0, setting.choices.length - 1);
                InlineSettingsBindings.setChoice(setting.id, target);
                check("choice binding accepts flattened id: " + setting.id,
                        InlineSettingsBindings.choiceValue(setting.id) == target);
                InlineSettingsBindings.setChoice(setting.id, before);
            }
        }
    }

    private static void chatTextFieldsRemainScreenOwnedAndAddressable() {
        String[] ids = {"chat-tab-name", "chat-tab-filter-type", "chat-tab-filter-text", "chat-tab-send-prefix"};
        for (String id : ids) {
            ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
            check("Chat custom-tab field remains a direct flat option: " + id,
                    setting != null && (setting.control == ReferenceSettingsCatalog.Control.TEXT
                            || setting.control == ReferenceSettingsCatalog.Control.CHOICE)
                            && ReferenceSettingsCatalog.ownerOf(id) == ReferenceSettingsCatalog.Tab.GAMEPLAY);
        }
    }

    private static boolean isScreenOwnedCustomTab(String id) {
        return "chat-tab-visible".equals(id) || "chat-tab-sound".equals(id)
                || "chat-tab-filter-type".equals(id) || "chat-tab-name".equals(id)
                || "chat-tab-filter-text".equals(id) || "chat-tab-send-prefix".equals(id)
                || "chat-tab-accent-color".equals(id);
    }

    private static List<ReferenceSettingsCatalog.Setting> configurableSettings() {
        List<ReferenceSettingsCatalog.Setting> values = new ArrayList<ReferenceSettingsCatalog.Setting>();
        for (String id : ReferenceSettingsCatalog.configurableIds()) {
            ReferenceSettingsCatalog.Setting setting = ReferenceSettingsCatalog.find(id);
            if (setting != null) values.add(setting);
        }
        return values;
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("flat catalog bindings: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
