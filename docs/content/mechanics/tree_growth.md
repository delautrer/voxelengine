The growth of trees in VoxelEngine is an organic process that happens in multiple phases. Here is a detailed breakdown of how the system works.

### Growth Conditions
For a sapling to grow, the following conditions must be met:
- **Ground:** The sapling must be planted on a `grass_block` or `dirt`.
- **Growth Stages:** A sapling has two stages (Stage 0 and Stage 1). When planted, it starts at Stage 0.

### Growth Timers (Ticks)
Every tree species has a defined minimum and maximum growth time (in seconds) in the configuration. A tick equals one-twentieth of a second (20 Ticks = 1 Second).
When a sapling is planted or changes its stage, a random timer between the minimum and maximum time is chosen.
*Note: You can view the exact times for saplings in the JSON configuration. On average, a stage takes a few minutes.*

### The Growth Process
1. **Planting:** The sapling is placed. The timer to reach Stage 1 starts.
2. **Stage 1:** The sapling reaches Stage 1. Visually, nothing changes, but the timer restarts for the final tree generation.
3. **Tree Generation:** Once the second timer expires, the tree sprouts.
   - The grass block underneath the sapling is converted to dirt.
   - The sapling disappears and is replaced by logs and leaves.
   - **Pine Trees Exception:** When a pine sapling grows, there is a **50% chance** that it will generate as an exceptionally tall pine tree (`alpha_tall_pine`) instead of a normal one!

### Farming Saplings
Trees drop new saplings when you break their leaves. Breaking logs does not trigger an automatic leaf decay; leaves must be broken manually to roll for a sapling drop.
