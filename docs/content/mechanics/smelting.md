The Furnace is one of the most important utility blocks in the game. Here you can find out exactly how the internal timers and rules are calculated.

### Smelting Speed (Cook Time)
By default, it takes **200 Ticks (10 seconds)** to smelt a single item in the furnace.
This value can theoretically be overridden by special recipes (`furnace_recipes.json`), but 10 seconds is the hardcoded default for almost all ores and food.

### Fuel
To operate the furnace, a valid fuel must be placed in the bottom slot. The exact burn times are defined in the `furnace_fuels.json`.
As soon as a smelting process can be started, **one** fuel item is consumed and the furnace stores the burn time.
* Coal, for example, burns long enough to smelt multiple items in a row.
* Wooden planks or sticks burn for a much shorter duration.

As long as the `burnTime` counter is greater than 0, the furnace lights up (Light Level 13) and the `cookTime` of the current item increases.

### Special Rules
* **Output Limit:** The furnace will only continue smelting if the resulting item can fit in the output slot on the right (the Max Stack Size must not be exceeded).
* **Destruction:** If you break a furnace, all items currently inside it will fly out into the world in random directions. Nothing is ever lost!
