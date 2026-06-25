VoxelEngine features a highly advanced, infinite procedural world generation system. The terrain and biomes are entirely driven by a sophisticated mathematical model known as **MultiNoise**.

### The 5 Dimensions of MultiNoise
Rather than relying on simple 2D heatmaps (like older generation techniques), the VoxelEngine determines which biome belongs where by calculating a 5-dimensional coordinate for every point in the world. The dimensions are:
1. **Temperature:** Ranges from freezing cold to scorching hot.
2. **Humidity:** Determines if an area is a dry desert or a lush, rainy forest.
3. **Continentalness:** Dictates whether a point is an ocean, a shore, flat inland, or deep inland.
4. **Erosion:** High erosion creates flatter, worn-down terrain, while low erosion allows for jagged, towering mountains.
5. **Weirdness:** Used for rare, unique biome variants and bizarre terrain formations.

### How Biomes are Chosen
Every biome in the game (defined in `assets/world/biomes.json`) is assigned an ideal "target point" within these 5 dimensions.
When the game generates a new chunk, it calculates the noise values for all 5 dimensions at that specific location. It then compares these values against every single biome to find the one with the lowest "fitness distance" (the closest match). That biome is then selected for that column of blocks!

### Seamless Blending
Because biomes are selected based on continuous noise functions, the transition between them is completely natural. You won't find a snowy tundra directly bordering a hot desert; there will always be mathematically appropriate transitional biomes in between.
Additionally, the terrain parameters (Base Height and Height Variation) are mathematically blended across biome borders. This means a flat plains biome gracefully ramps up into a mountainous biome, avoiding unnatural, sheer cliffs at biome borders.
