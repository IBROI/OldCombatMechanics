package net.lastcraft.oldpvp;

import org.bukkit.util.Vector;


public class Hit {

    private Vector direction;
    private boolean sprint;
    private int enchantLevel;

    public Hit(Vector direction, boolean sprint, int enchantLevel) {
        this.direction = direction;
        this.sprint = sprint;
        this.enchantLevel = enchantLevel;
    }

    public Vector getDirection() {
        return this.direction;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public int getEnchantLevel() {
        return this.enchantLevel;
    }
}
