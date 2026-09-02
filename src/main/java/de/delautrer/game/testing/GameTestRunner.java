package de.delautrer.game.testing;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.IInteractable;
import de.delautrer.game.blocks.entities.BlockEntity;
import de.delautrer.game.blocks.entities.ChestBlockEntity;
import de.delautrer.game.blocks.entities.FurnaceBlockEntity;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.state.Property;
import de.delautrer.game.entity.Entity;
import de.delautrer.game.entity.FallingBlockEntity;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.items.Item;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.nbt.TagIo;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.World;
import de.delautrer.game.world.generation.structure.StructureRegistry;
import de.delautrer.game.world.generation.structure.StructureTemplate;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;
import java.util.Map;

public class GameTestRunner {

    public static GameTestResult run(World world, LocalPlayer playerOrNull, GameTest test) {
        if (world == null || test == null) {
            return GameTestResult.fail(test, 0, "Null world or test provided to GameTestRunner");
        }

        Vector3i origin = calculateOrigin(playerOrNull, test.getOriginMode());
        LocalPlayer dummyPlayer = (playerOrNull != null) ? playerOrNull : new LocalPlayer(new Vector3d(origin.x, origin.y, origin.z));

        // Pre-test cleanup: 12x10x12 air around origin, y=-1 filled with stone
        prepareTestArea(world, origin);

        int executedTicks = 0;
        List<GameTestStep> steps = test.getSteps();

        for (int i = 0; i < steps.size(); i++) {
            GameTestStep step = steps.get(i);
            if (step == null || step.type == null) {
                return GameTestResult.fail(test, i, "step " + i + " null step or step type");
            }

            String type = step.type.toLowerCase();
            switch (type) {
                case "set_block": {
                    Vector3i pos = getPos(origin, step.pos);
                    Block block = getBlock(step.block);
                    if (block == null) {
                        return GameTestResult.fail(test, i, "step " + i + " set_block unknown block '" + step.block + "'");
                    }
                    world.setBlockWithState(pos.x, pos.y, pos.z, block, (byte) step.state, false);
                    break;
                }
                case "place_block": {
                    Vector3i pos = getPos(origin, step.pos);
                    Block block = getBlock(step.block);
                    if (block == null) {
                        return GameTestResult.fail(test, i, "step " + i + " place_block unknown block '" + step.block + "'");
                    }
                    Vector3i hitFace = parseHitFace(step.face);
                    Vector3f exactHit = new Vector3f(pos.x + 0.5f, pos.y + 0.5f, pos.z + 0.5f);

                    BlockState newState = block.getStateForPlacement(world, dummyPlayer, pos, hitFace, exactHit);
                    if (newState != null) {
                        world.setBlockState(pos.x, pos.y, pos.z, newState);
                        block.onBlockPlaced(world, pos, newState, dummyPlayer);
                    }
                    break;
                }
                case "fill": {
                    Vector3i from = getPos(origin, step.from);
                    Vector3i to = getPos(origin, step.to);
                    Block block = getBlock(step.block);
                    if (block == null) {
                        return GameTestResult.fail(test, i, "step " + i + " fill unknown block '" + step.block + "'");
                    }
                    int minX = Math.min(from.x, to.x);
                    int maxX = Math.max(from.x, to.x);
                    int minY = Math.min(from.y, to.y);
                    int maxY = Math.max(from.y, to.y);
                    int minZ = Math.min(from.z, to.z);
                    int maxZ = Math.max(from.z, to.z);

                    for (int fx = minX; fx <= maxX; fx++) {
                        for (int fy = minY; fy <= maxY; fy++) {
                            for (int fz = minZ; fz <= maxZ; fz++) {
                                world.setBlockWithState(fx, fy, fz, block, (byte) step.state, false);
                            }
                        }
                    }
                    break;
                }
                case "tick": {
                    int count = Math.max(1, step.count);
                    executedTicks += count;
                    if (executedTicks > test.getTimeoutTicks()) {
                        return GameTestResult.fail(test, i, "Timeout: executed ticks (" + executedTicks + ") exceeded timeout_ticks (" + test.getTimeoutTicks() + ")");
                    }
                    for (int t = 0; t < count; t++) {
                        world.getTickScheduler().update(0.05f, dummyPlayer);
                        for (BlockEntity be : world.getBlockEntities().values()) {
                            be.tick();
                        }
                        for (Entity e : world.getEntities()) {
                            if (e instanceof FallingBlockEntity fbe) {
                                fbe.update(0.05f, world.getChunkManager(), world);
                            } else {
                                e.update(0.05f, world.getChunkManager());
                            }
                        }
                    }
                    break;
                }
                case "call_scheduled": {
                    Vector3i pos = getPos(origin, step.pos);
                    int count = Math.max(1, step.count);
                    for (int c = 0; c < count; c++) {
                        Block b = world.getBlock(pos.x, pos.y, pos.z);
                        if (b != null) {
                            b.scheduledTick(world, pos.x, pos.y, pos.z);
                        }
                        world.getTickScheduler().update(0.05f, dummyPlayer);
                        for (BlockEntity be : world.getBlockEntities().values()) {
                            be.tick();
                        }
                    }
                    break;
                }
                case "place_template": {
                    Vector3i pos = getPos(origin, step.pos);
                    if (step.template == null) {
                        return GameTestResult.fail(test, i, "step " + i + " place_template missing template");
                    }
                    NamespacedKey tKey = step.template.contains(":") ? NamespacedKey.fromString(step.template) : NamespacedKey.fromString("veinstride:" + step.template);
                    StructureTemplate template = StructureRegistry.getTemplate(tKey);
                    if (template == null) {
                        return GameTestResult.fail(test, i, "step " + i + " place_template unknown template '" + step.template + "'");
                    }
                    for (StructureTemplate.StructureBlock sb : template.getBlocks()) {
                        int wx = pos.x + sb.dx;
                        int wy = pos.y + sb.dy;
                        int wz = pos.z + sb.dz;
                        Block b = sb.block;
                        byte st = sb.state;
                        CompoundTag nbt = sb.nbt;
                        if (b != null && b.isStructureVoid()) {
                            b = Registries.BLOCKS.get("veinstride:air");
                            st = 0;
                            nbt = null;
                        }
                        world.setBlockWithState(wx, wy, wz, b, st, false);
                        if (nbt != null) {
                            int cx = wx >> 4;
                            int cz = wz >> 4;
                            Chunk chunk = world.getChunkManager().getChunk(cx, cz);
                            if (chunk != null) {
                                chunk.setBlockEntityTag(wx & 15, wy, wz & 15, nbt);
                            }
                        }
                    }
                    break;
                }
                case "set_be_tag": {
                    Vector3i pos = getPos(origin, step.pos);
                    if (step.nbt == null) {
                        return GameTestResult.fail(test, i, "step " + i + " set_be_tag missing nbt object");
                    }
                    CompoundTag tag = (CompoundTag) TagIo.fromJson(step.nbt);
                    int cx = pos.x >> 4;
                    int cz = pos.z >> 4;
                    Chunk chunk = world.getChunkManager().getChunk(cx, cz);
                    if (chunk != null) {
                        chunk.setBlockEntityTag(pos.x & 15, pos.y, pos.z & 15, tag);
                    }
                    world.setBlockEntity(pos, null);
                    break;
                }
                case "interact": {
                    Vector3i pos = getPos(origin, step.pos);
                    Block block = world.getBlock(pos.x, pos.y, pos.z);
                    if (block instanceof IInteractable interactable) {
                        interactable.onInteract(world, pos, dummyPlayer);
                    } else {
                        return GameTestResult.fail(test, i, "step " + i + " interact target at (" + pos.x + "," + pos.y + "," + pos.z + ") is not IInteractable");
                    }
                    break;
                }
                case "set_slot": {
                    Vector3i pos = getPos(origin, step.pos);
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be == null) {
                        return GameTestResult.fail(test, i, "step " + i + " set_slot expected block entity at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    IInventory inv = getInventoryFromBE(be);
                    if (inv == null) {
                        return GameTestResult.fail(test, i, "step " + i + " set_slot block entity has no inventory at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    if (step.slot < 0 || step.slot >= inv.getSize()) {
                        return GameTestResult.fail(test, i, "step " + i + " set_slot slot " + step.slot + " out of bounds at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    if (step.item == null || step.item.equalsIgnoreCase("air") || step.item.equalsIgnoreCase("veinstride:air")) {
                        inv.setStack(step.slot, null);
                    } else {
                        NamespacedKey itemKey = step.item.contains(":") ? NamespacedKey.fromString(step.item) : NamespacedKey.fromString("veinstride:" + step.item);
                        Item item = Registries.ITEMS.get(itemKey);
                        if (item == null) {
                            return GameTestResult.fail(test, i, "step " + i + " set_slot unknown item: " + step.item);
                        }
                        int count = Math.max(1, step.count);
                        inv.setStack(step.slot, new ItemStack(item, count));
                    }
                    break;
                }
                case "assert_block": {
                    Vector3i pos = getPos(origin, step.pos);
                    Block expBlock = getBlock(step.block);
                    Block actBlock = world.getBlock(pos.x, pos.y, pos.z);
                    if (actBlock != expBlock) {
                        String expName = step.block;
                        String actName = actBlock != null ? Registries.BLOCKS.getKey(actBlock).toString() : "null";
                        return GameTestResult.fail(test, i, "step " + i + " assert_block expected " + expName + " got " + actName + " at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    break;
                }
                case "assert_air": {
                    Vector3i pos = getPos(origin, step.pos);
                    Block actBlock = world.getBlock(pos.x, pos.y, pos.z);
                    if (actBlock != null && !actBlock.isAir()) {
                        String actName = Registries.BLOCKS.getKey(actBlock).toString();
                        return GameTestResult.fail(test, i, "step " + i + " assert_air expected air got " + actName + " at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    break;
                }
                case "assert_state": {
                    Vector3i pos = getPos(origin, step.pos);
                    BlockState state = world.getBlockState(pos.x, pos.y, pos.z);
                    if (state == null || state.getBlock().isAir()) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_state expected state on block but found air at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    String propName = step.prop;
                    String expectedValStr = step.getValueAsString();

                    boolean foundProp = false;
                    boolean match = false;
                    String actualValStr = "";

                    if ("level".equalsIgnoreCase(propName) || "state".equalsIgnoreCase(propName) || "state_id".equalsIgnoreCase(propName)) {
                        byte stId = state.getStateId();
                        foundProp = true;
                        actualValStr = String.valueOf(stId);
                        if (actualValStr.equalsIgnoreCase(expectedValStr)) {
                            match = true;
                        }
                    }

                    if (!match && state.getProperties() != null) {
                        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
                            if (entry.getKey().getName().equalsIgnoreCase(propName)) {
                                foundProp = true;
                                actualValStr = entry.getValue().toString();
                                if (actualValStr.equalsIgnoreCase(expectedValStr)) {
                                    match = true;
                                }
                                break;
                            }
                        }
                    }

                    if (!foundProp) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_state property '" + propName + "' missing on " + Registries.BLOCKS.getKey(state.getBlock()) + " at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    if (!match) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_state prop '" + propName + "' expected '" + expectedValStr + "' got '" + actualValStr + "' at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    break;
                }
                case "assert_be": {
                    Vector3i pos = getPos(origin, step.pos);
                    BlockEntity be = world.getBlockEntity(pos);
                    String expectedType = step.getBeType();
                    if (be == null) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_be expected block entity of type '" + expectedType + "' got null at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    if ("chest".equalsIgnoreCase(expectedType) || "veinstride:chest".equalsIgnoreCase(expectedType)) {
                        if (!(be instanceof ChestBlockEntity)) {
                            return GameTestResult.fail(test, i, "step " + i + " assert_be expected type '" + expectedType + "' got " + be.getClass().getSimpleName() + " at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                        }
                    }
                    break;
                }
                case "assert_loot": {
                    Vector3i pos = getPos(origin, step.pos);
                    BlockEntity be = world.getBlockEntity(pos);
                    if (!(be instanceof ChestBlockEntity chest)) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_loot expected ChestBlockEntity at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    boolean hasLoot = false;
                    for (int s = 0; s < chest.getInventory().getSize(); s++) {
                        if (chest.getInventory().getStack(s) != null) {
                            hasLoot = true;
                            break;
                        }
                    }
                    if (!hasLoot) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_loot inventory empty at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    break;
                }
                case "assert_item": {
                    Vector3i pos = getPos(origin, step.pos);
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be == null) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_item expected block entity at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    IInventory inv = getInventoryFromBE(be);
                    if (inv == null) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_item block entity has no inventory at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    if (step.slot < 0 || step.slot >= inv.getSize()) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_item slot " + step.slot + " out of bounds at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    ItemStack stack = inv.getStack(step.slot);
                    boolean expectAir = (step.item == null || step.item.equalsIgnoreCase("air") || step.item.equalsIgnoreCase("veinstride:air"));
                    if (expectAir) {
                        if (stack != null && stack.amount > 0 && stack.type != null) {
                            return GameTestResult.fail(test, i, "step " + i + " assert_item expected air at slot " + step.slot + " got " + Registries.ITEMS.getKey(stack.type) + " at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                        }
                    } else {
                        if (stack == null || stack.amount <= 0 || stack.type == null) {
                            return GameTestResult.fail(test, i, "step " + i + " assert_item expected " + step.item + " at slot " + step.slot + " got empty slot at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                        }
                        NamespacedKey expItemKey = step.item.contains(":") ? NamespacedKey.fromString(step.item) : NamespacedKey.fromString("veinstride:" + step.item);
                        NamespacedKey actItemKey = Registries.ITEMS.getKey(stack.type);
                        if (!expItemKey.equals(actItemKey)) {
                            return GameTestResult.fail(test, i, "step " + i + " assert_item expected item " + expItemKey + " got " + actItemKey + " at slot " + step.slot + " at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                        }
                        int minCount = step.min_count > 0 ? step.min_count : (step.count > 0 ? step.count : 1);
                        if (stack.amount < minCount) {
                            return GameTestResult.fail(test, i, "step " + i + " assert_item expected min count " + minCount + " got " + stack.amount + " at slot " + step.slot + " at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                        }
                    }
                    break;
                }
                case "assert_empty_inv": {
                    Vector3i pos = getPos(origin, step.pos);
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be == null) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_empty_inv expected block entity at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    IInventory inv = getInventoryFromBE(be);
                    if (inv == null) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_empty_inv block entity has no inventory at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                    }
                    for (int s = 0; s < inv.getSize(); s++) {
                        ItemStack st = inv.getStack(s);
                        if (st != null && st.amount > 0 && st.type != null) {
                            return GameTestResult.fail(test, i, "step " + i + " assert_empty_inv found non-empty slot " + s + " with item " + Registries.ITEMS.getKey(st.type) + " at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                        }
                    }
                    break;
                }
                case "assert_entity": {
                    Vector3i pos = getPos(origin, step.pos);
                    String eType = step.getBeType().toLowerCase();
                    boolean found = false;
                    for (Entity e : world.getEntities()) {
                        if (e.position.x >= pos.x - 1.0 && e.position.x <= pos.x + 2.0 &&
                            e.position.z >= pos.z - 1.0 && e.position.z <= pos.z + 2.0 &&
                            e.position.y >= pos.y - 1.0 && e.position.y <= pos.y + 3.0) {
                            if ("falling_block".equals(eType) && e instanceof FallingBlockEntity) {
                                found = true;
                                break;
                            } else if ("item".equals(eType) && e instanceof ItemEntity) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        return GameTestResult.fail(test, i, "step " + i + " assert_entity expected entity '" + eType + "' near (" + pos.x + "," + pos.y + "," + pos.z + ") but none found");
                    }
                    break;
                }
                case "assert_no_entity": {
                    Vector3i pos = getPos(origin, step.pos);
                    String eType = step.getBeType().toLowerCase();
                    for (Entity e : world.getEntities()) {
                        if (e.position.x >= pos.x - 1.0 && e.position.x <= pos.x + 2.0 &&
                            e.position.z >= pos.z - 1.0 && e.position.z <= pos.z + 2.0 &&
                            e.position.y >= pos.y - 1.0 && e.position.y <= pos.y + 3.0) {
                            if ("falling_block".equals(eType) && e instanceof FallingBlockEntity) {
                                return GameTestResult.fail(test, i, "step " + i + " assert_no_entity found falling_block entity at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                            } else if ("item".equals(eType) && e instanceof ItemEntity) {
                                return GameTestResult.fail(test, i, "step " + i + " assert_no_entity found item entity at (" + pos.x + "," + pos.y + "," + pos.z + ")");
                            }
                        }
                    }
                    break;
                }
                case "assert_sapling_staged_or_grown":
                case "sapling_grows_or_stages": {
                    Vector3i pos = getPos(origin, step.pos);
                    Block blockAtPos = world.getBlock(pos.x, pos.y, pos.z);
                    boolean isSaplingStillThere = blockAtPos != null && Registries.BLOCKS.getKey(blockAtPos).toString().contains("sapling");

                    boolean foundLog = false;
                    Block oakLog = Registries.BLOCKS.get("veinstride:oak_log");
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dy = 0; dy <= 9; dy++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                Block b = world.getBlock(origin.x + dx, origin.y + dy, origin.z + dz);
                                if (b == oakLog || (b != null && Registries.BLOCKS.getKey(b).toString().contains("_log"))) {
                                    foundLog = true;
                                    break;
                                }
                            }
                            if (foundLog) break;
                        }
                        if (foundLog) break;
                    }

                    if (!isSaplingStillThere && !foundLog) {
                        return GameTestResult.fail(test, i, "step " + i + " sapling_grows_or_stages expected sapling or grown log near origin (" + origin.x + "," + origin.y + "," + origin.z + ")");
                    }
                    break;
                }
                default: {
                    return GameTestResult.fail(test, i, "step " + i + " unknown step type '" + step.type + "'");
                }
            }
        }

        return GameTestResult.pass(test);
    }

    public static Vector3i calculateOrigin(LocalPlayer playerOrNull, String originMode) {
        if ("player".equalsIgnoreCase(originMode) && playerOrNull != null) {
            Vector3d pPos = playerOrNull.getPosition();
            int px = (int) Math.floor(pPos.x);
            int py = (int) Math.floor(pPos.y);
            int pz = (int) Math.floor(pPos.z);
            return new Vector3i(px, py, pz + 4);
        }
        return new Vector3i(0, 0, 0);
    }

    private static void prepareTestArea(World world, Vector3i origin) {
        Block air = Registries.BLOCKS.get("veinstride:air");
        Block stone = Registries.BLOCKS.get("veinstride:stone");

        // Clear 12x10x12 air at y=0..9
        for (int x = origin.x - 6; x <= origin.x + 5; x++) {
            for (int y = origin.y; y <= origin.y + 9; y++) {
                for (int z = origin.z - 6; z <= origin.z + 5; z++) {
                    world.setBlockWithState(x, y, z, air, (byte) 0, false);
                }
            }
        }

        // Fill floor at y = origin.y - 1 with stone
        for (int x = origin.x - 6; x <= origin.x + 5; x++) {
            for (int z = origin.z - 6; z <= origin.z + 5; z++) {
                world.setBlockWithState(x, origin.y - 1, z, stone, (byte) 0, false);
            }
        }
    }

    private static Vector3i getPos(Vector3i origin, int[] relativePos) {
        if (relativePos == null || relativePos.length < 3) {
            return new Vector3i(origin);
        }
        return new Vector3i(origin.x + relativePos[0], origin.y + relativePos[1], origin.z + relativePos[2]);
    }

    private static Block getBlock(String blockName) {
        if (blockName == null) return Registries.BLOCKS.get("veinstride:air");
        NamespacedKey bKey = blockName.contains(":") ? NamespacedKey.fromString(blockName) : NamespacedKey.fromString("veinstride:" + blockName);
        return Registries.BLOCKS.get(bKey);
    }

    private static Vector3i parseHitFace(String faceStr) {
        if (faceStr == null) return new Vector3i(0, 1, 0);
        return switch (faceStr.toLowerCase()) {
            case "down" -> new Vector3i(0, -1, 0);
            case "north" -> new Vector3i(0, 0, -1);
            case "south" -> new Vector3i(0, 0, 1);
            case "east" -> new Vector3i(1, 0, 0);
            case "west" -> new Vector3i(-1, 0, 0);
            default -> new Vector3i(0, 1, 0);
        };
    }

    private static IInventory getInventoryFromBE(BlockEntity be) {
        if (be instanceof ChestBlockEntity chest) {
            return chest.getInventory();
        } else if (be instanceof FurnaceBlockEntity furnace) {
            return furnace.getInventory();
        }
        return null;
    }
}
