package de.delautrer.game.events;

import de.delautrer.engine.events.Event;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;

public class CommandExecutedEvent implements Event {
    public final String commandName;
    public final String[] args;
    public final LocalPlayer player;
    public final World world;

    public CommandExecutedEvent(String commandName, String[] args, LocalPlayer player, World world) {
        this.commandName = commandName;
        this.args = args;
        this.player = player;
        this.world = world;
    }
}