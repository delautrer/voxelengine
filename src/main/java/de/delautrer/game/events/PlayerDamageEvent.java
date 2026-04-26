package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.entity.player.Player;

public class PlayerDamageEvent implements Event {
    private final Player player;
    private final float damage;

    public PlayerDamageEvent(Player player, float damage) {
        this.player = player;
        this.damage = damage;
    }

    public Player getPlayer() {
        return player;
    }

    public float getDamage() {
        return damage;
    }
}