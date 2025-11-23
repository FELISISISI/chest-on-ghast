package me.noramibu.element;

import java.util.Locale;

/**
 * 快乐恶魂属性枚举
 */
public enum GhastElement {
    FIRE("fire"),
    ICE("ice"),
    WIND("wind"),
    SAND("sand");

    private final String id;

    GhastElement(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static GhastElement fromId(String id) {
        if (id == null) {
            return FIRE;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (GhastElement element : values()) {
            if (element.id.equals(normalized)) {
                return element;
            }
        }
        return FIRE;
    }
}
