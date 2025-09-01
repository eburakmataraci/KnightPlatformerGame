package com.game;

import java.awt.Rectangle;

public class Attack {
    public enum Type { LIGHT, HEAVY }

    public final Type type;
    public final int damage;
    public final int knockbackX;
    public final int durationMs;
    public final Rectangle hitbox; // dünya koordinatı
    public long startTimeMs;

    public Attack(Type type, int dmg, int kbX, int durMs, Rectangle hitbox){
        this.type = type;
        this.damage = dmg;
        this.knockbackX = kbX;
        this.durationMs = durMs;
        this.hitbox = hitbox;
    }

    public boolean active(long now){
        return now - startTimeMs <= durationMs;
    }
}
