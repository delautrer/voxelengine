Water is the only fluid in VoxelEngine and uses a complex algorithm to spread.

### Water Levels
Water internally has 9 stages (Level 0 to 8).
- **Level 8** is a full Source Block.
- **Level 7 to 1** are flowing water. For every block water flows horizontally, the level decreases by 1. This means water flows a maximum distance of **7 blocks**.
- **Level 0** is air (the water dissipates).

### Infinite Water Sources
Just like in Minecraft, there are infinite water sources in VoxelEngine!
An empty space will automatically turn back into a Level 8 Source Block if:
1. It is adjacent to at least **2 water sources** (Level 8).
2. The block underneath the space is solid OR is itself a full water source.

### Flow Behavior and Pathfinding
Water updates every **5 Ticks** (0.25 seconds) and calculates its path.
It prioritizes drops/cliffs:
1. **Falling Downwards:** If there is air beneath the water, it immediately drops straight down. It forms a vertical stream (Level 7) and will not spread horizontally on that level.
2. **Smart Pathfinding:** When water spreads horizontally, it scans a radius of up to 4 blocks to check if there is a drop (a hole) nearby. If it finds a hole, the water flows **directly** towards it, rather than spreading out in a circle in all directions!

### Block Destruction
Water is destructive to weak blocks! If water flows over plants (like grass, dandelions, saplings, etc.), the water automatically breaks these blocks. The items pop out and float away.
