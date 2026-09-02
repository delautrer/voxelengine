package de.delautrer.game.blocks.entities;

import de.delautrer.game.blocks.FurnaceBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.inventory.FurnaceInventory;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.nbt.CompoundTag;
import de.delautrer.game.nbt.ListTag;
import de.delautrer.game.nbt.VsnbtTag;
import de.delautrer.game.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class FurnaceBlockEntity extends BlockEntity {
    private final FurnaceInventory inventory;

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int cookTime = 0;
    private int maxCookTime = 200; // Standard: 10 Sekunden

    @SuppressWarnings("this-escape")
    public FurnaceBlockEntity(World world, Vector3i pos) {
        super(world, pos);
        this.inventory = new FurnaceInventory(this);
    }

    public FurnaceInventory getInventory() {
        return inventory;
    }

    @Override
    public BlockEntityType<?> getType() {
        return BlockEntityTypeRegistry.FURNACE;
    }

    @Override
    public void writeTag(CompoundTag tag) {
        super.writeTag(tag);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putInt("CookTime", cookTime);
        tag.putInt("MaxCookTime", maxCookTime);

        ListTag itemsList = new ListTag();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack != null && stack.type != null && stack.amount > 0) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                itemTag.putString("id", de.delautrer.game.registry.Registries.ITEMS.getKey(stack.type).toString());
                itemTag.putInt("Count", stack.amount);
                if (stack.tag != null) {
                    itemTag.put("tag", stack.tag.copy());
                }
                itemsList.add(itemTag);
            }
        }
        tag.put("Items", itemsList);
    }

    @Override
    public void readTag(CompoundTag tag) {
        super.readTag(tag);
        this.burnTime = tag.getInt("BurnTime");
        this.maxBurnTime = tag.getInt("MaxBurnTime");
        this.cookTime = tag.getInt("CookTime");
        this.maxCookTime = tag.getInt("MaxCookTime");

        inventory.clear();
        if (tag.contains("Items", VsnbtTag.TYPE_LIST)) {
            ListTag itemsList = tag.getList("Items");
            for (int i = 0; i < itemsList.size(); i++) {
                VsnbtTag elem = itemsList.get(i);
                if (elem instanceof CompoundTag itemTag) {
                    int slot = itemTag.getByte("Slot") & 0xFF;
                    String itemKey = itemTag.getString("id");
                    int amount = itemTag.getInt("Count");
                    de.delautrer.game.items.Item item = de.delautrer.game.registry.Registries.ITEMS.get(itemKey);
                    if (item != null && amount > 0 && slot < inventory.getSize()) {
                        ItemStack stack = new ItemStack(item, amount);
                        if (itemTag.contains("tag", VsnbtTag.TYPE_COMPOUND)) {
                            stack.tag = itemTag.getCompound("tag");
                        }
                        inventory.setStack(slot, stack);
                    }
                }
            }
        }
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public int getCookTime() {
        return cookTime;
    }

    public int getMaxCookTime() {
        return maxCookTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public void setMaxBurnTime(int maxBurnTime) {
        this.maxBurnTime = maxBurnTime;
    }

    public void setCookTime(int cookTime) {
        this.cookTime = cookTime;
    }

    @Override
    public void tick() {
        ItemStack input = inventory.getStack(0);
        ItemStack fuel = inventory.getStack(1);

        de.delautrer.game.crafting.FurnaceRecipeManager.SmeltingRecipe recipe = (input == null) ? null : de.delautrer.game.crafting.FurnaceRecipeManager.getRecipe(input.type);
        ItemStack result = (recipe == null) ? null : recipe.result;
        if (recipe != null) {
            this.maxCookTime = recipe.cookTime;
        } else {
            this.maxCookTime = 200;
        }

        boolean hasRecipe = result != null;
        boolean canPlace = hasRecipe && canPlaceResult(result);
        boolean isBurning = false;

        if (canPlace) {
            if (burnTime <= 0 && fuel != null) {
                int currentFuelTime = de.delautrer.game.crafting.FurnaceRecipeManager.getBurnTime(fuel.type);
                if (currentFuelTime > 0) {
                    burnTime = currentFuelTime;
                    maxBurnTime = currentFuelTime;

                    fuel.amount--;
                    if (fuel.amount <= 0) {
                        inventory.setStack(1, null);
                    } else {
                        inventory.setStack(1, fuel);
                    }
                }
            }

            if (burnTime > 0) {
                burnTime--;
                isBurning = true;
                cookTime++;
                if (cookTime >= maxCookTime) {
                    cookTime = 0;
                    smeltItem(input, result);
                }
            }
        } else {
            isBurning = false;
            if (!hasRecipe) {
                cookTime = 0;
            }
        }

        BlockState state = world.getBlockState(pos.x, pos.y, pos.z);
        if (state.getBlock() instanceof FurnaceBlock) {
            boolean wasLit = state.getValue(FurnaceBlock.LIT);
            if (wasLit != isBurning) {
                world.setBlockState(pos.x, pos.y, pos.z, state.with(FurnaceBlock.LIT, isBurning));
            }
        }
    }

    private boolean canPlaceResult(ItemStack resultStack) {
        ItemStack currentOut = inventory.getStack(2);
        if (currentOut == null) return true;
        if (currentOut.type != resultStack.type) return false;
        return (currentOut.amount + resultStack.amount) <= currentOut.type.getMaxStackSize();
    }

    private void smeltItem(ItemStack input, ItemStack result) {
        input.amount--;
        if (input.amount <= 0) {
            inventory.setStack(0, null);
        } else {
            inventory.setStack(0, input);
        }

        ItemStack currentOut = inventory.getStack(2);
        if (currentOut == null) {
            inventory.setStack(2, new ItemStack(result.type, result.amount));
        } else {
            currentOut.amount += result.amount;
            inventory.setStack(2, currentOut);
        }
    }

    @Override
    public void onRemove() {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack != null && stack.amount > 0) {
                Vector3d spawnPos = new Vector3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5);
                Vector3f vel = new Vector3f((float)Math.random() * 2 - 1, 2.0f, (float)Math.random() * 2 - 1).normalize().mul(3.0f);

                ItemEntity drop = new ItemEntity(stack, spawnPos, vel);
                world.spawnEntity(drop);
            }
        }
        inventory.clear();
    }
}
