package de.delautrer.game.commands;

import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.world.World;
import de.delautrer.game.world.sky.Weather;
import java.util.ArrayList;
import java.util.List;


public class WeatherCommand implements ICommand {

    @Override
    public String getName() {
        return "weather";
    }

    @Override
    public String getUsage() {
        return "/weather [clear|partly_cloudy|overcast]";
    }

    @Override
    public void execute(LocalPlayer player, World world, String[] args, CommandManager commandManager) {
        if (args.length == 0) {
            Weather current = world.getSkyManager().getCurrentWeather();
            Weather[] weathers = Weather.values();

            int nextIndex = (current.ordinal() + 1) % weathers.length;
            Weather nextWeather = weathers[nextIndex];

            world.getSkyManager().forceWeather(nextWeather);
            commandManager.sendMessageInChat("Weather advanced to: " + nextWeather.name().toLowerCase());
            return;
        }

        if (args.length == 1) {
            String type = args[0].toUpperCase();
            try {
                Weather targetWeather = Weather.valueOf(type);
                world.getSkyManager().forceWeather(targetWeather);
                commandManager.sendMessageInChat("Weather set to: " + targetWeather.name().toLowerCase());
            } catch (IllegalArgumentException e) {
                commandManager.sendMessageInChat("Invalid weather type! Usage: " + getUsage());
            }
            return;
        }

        // If too many arguments are provided
        commandManager.sendMessageInChat("Usage: " + getUsage());
    }

    @Override
    public List<String> getTabCompletions(LocalPlayer player, String[] args) {
        List<String> completions = new ArrayList<>();

        // Autocomplete the weather types for the first argument
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Weather weather : Weather.values()) {
                String weatherName = weather.name().toLowerCase();
                if (weatherName.startsWith(input)) {
                    completions.add(weatherName);
                }
            }
        }

        return completions;
    }
}