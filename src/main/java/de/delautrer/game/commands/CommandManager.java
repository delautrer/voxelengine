package de.delautrer.game.commands;

import de.delautrer.engine.events.EventBus;
import de.delautrer.engine.events.EventListener;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.events.ChatMessageEvent;
import de.delautrer.game.events.CommandExecutedEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager implements EventListener<CommandExecutedEvent> {

    private final EventBus eventBus;
    private final Map<String, ICommand> commands = new HashMap<>();

    @SuppressWarnings("this-escape")
    public CommandManager(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.subscribe(CommandExecutedEvent.class, this);
        registerDefaultCommands();
    }

    private void registerDefaultCommands() {
        register(new GameModeCommand());
        register(new TeleportCommand());
        register(new ClearCommand());
        register(new TimeCommand());
        register(new DebugCommand());
        register(new WeatherCommand());
        register(new FindCommand());
        register(new TestBiomesCommand());
        register(new GiveCommand());
        register(new SetCommand());
        register(new FillCommand());
        register(new StructureCommand());
        if (de.delautrer.Constants.IS_DEV) {
            register(new TestCommand());
        }
    }

    public void register(ICommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public void onEvent(CommandExecutedEvent event) {
        ICommand cmd = commands.get(event.commandName.toLowerCase());

        if (cmd != null) {
            if (cmd.requiresCheats() && !event.world.isCheatsAllowed()) {
                sendMessageInChat("Cheats are not enabled in this world.");
                return;
            }
            try {
                cmd.execute(event.player, event.world, event.args, this);
            } catch (Exception e) {
                sendMessageInChat("There was an error executing the command: " + event.commandName);
                System.err.println("Fehler beim Ausführen des Commands: " + event.commandName);
                e.printStackTrace();
            }
        } else {
            sendMessageInChat("Unknown Command: /" + event.commandName);
        }
    }

    public void sendMessageInChat(String message){
        eventBus.publish(new ChatMessageEvent(message));
    }

    public void sendMessageInChat(de.delautrer.game.ui.chat.ChatComponent component) {
        eventBus.publish(new ChatMessageEvent(component));
    }

    public List<String> getTabCompletions(LocalPlayer player, de.delautrer.game.world.World world, String input) {
        List<String> results = new ArrayList<>();
        if (!input.startsWith("/")) return results;

        String raw = input.substring(1);
        String[] parts = raw.split(" ", -1);
        String commandName = parts[0].toLowerCase();

        if (parts.length == 1) {
            for (String cmd : commands.keySet()) {
                ICommand commandObj = commands.get(cmd);
                if (commandObj != null && commandObj.requiresCheats() && !world.isCheatsAllowed()) {
                    continue;
                }
                if (cmd.startsWith(commandName)) {
                    results.add("/" + cmd);
                }
            }
        } else {
            ICommand cmd = commands.get(commandName);
            if (cmd != null) {
                if (cmd.requiresCheats() && !world.isCheatsAllowed()) {
                    return results;
                }
                String[] args = new String[parts.length - 1];
                System.arraycopy(parts, 1, args, 0, args.length);
                List<String> argCompletions = cmd.getTabCompletions(player, args);

                StringBuilder baseInput = new StringBuilder("/" + commandName);
                for (int i = 0; i < args.length - 1; i++) {
                    baseInput.append(" ").append(args[i]);
                }
                baseInput.append(" ");

                for (String completion : argCompletions) {
                    results.add(baseInput.toString() + completion);
                }
            }
        }
        return results;
    }
}