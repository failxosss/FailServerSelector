package com.failxos.serverselector.item;

import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;

/** Simplified click types usable in the config. */
public enum Click {
    LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT;

    public static Click of(Action action, boolean sneaking) {
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        if (!left && !right) {
            return null;
        }
        if (left) {
            return sneaking ? SHIFT_LEFT : LEFT;
        }
        return sneaking ? SHIFT_RIGHT : RIGHT;
    }

    public static Click of(ClickType type) {
        return switch (type) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            case SHIFT_LEFT -> SHIFT_LEFT;
            case SHIFT_RIGHT -> SHIFT_RIGHT;
            default -> null;
        };
    }
}
