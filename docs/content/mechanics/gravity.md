Gravity is not the same for all blocks in VoxelEngine. Most blocks magically float in the air, but some obey the laws of physics!

### Which Blocks Fall?
All blocks that inherit from the `GravityBlock` class are affected by gravity. Currently, this mainly includes:
- **Sand**
- **Gravel**

### How Does Falling Work?
As soon as a block underneath a gravity-affected block is broken (or changed), the block waits exactly **2 Ticks** (0.1 seconds).
If there is still air underneath it after this short delay, the block transforms into a dynamic object (a `FallingBlockEntity`) and begins to fall.

The object accelerates downwards due to the game's defined gravity until it hits a solid surface. Upon impact, it turns back into a normal, solid block.

### Fall Damage for Players!
> [!CAUTION]
> Be careful in caves! If a falling sand or gravel block lands on a player, it deals extreme damage!
> As long as the block is falling and is inside the player's hitbox, the player takes **2.0 Damage (1 full heart)** per tick.
> This means a block falling on your head can take you out in a fraction of a second!
