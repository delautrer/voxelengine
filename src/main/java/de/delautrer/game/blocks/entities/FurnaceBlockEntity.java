package de.delautrer.game.blocks.entities;

import de.delautrer.game.blocks.FurnaceBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.inventory.FurnaceInventory;
import de.delautrer.game.items.ItemStack;
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
    public void write(java.io.DataOutputStream dos) throws java.io.IOException {
        super.write(dos);
        dos.writeInt(burnTime);
        dos.writeInt(maxBurnTime);
        dos.writeInt(cookTime);
        dos.writeInt(maxCookTime);
        dos.writeInt(inventory.getSize());
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack != null && stack.type != null && stack.amount > 0) {
                dos.writeInt(i);
                dos.writeUTF(de.delautrer.game.registry.Registries.ITEMS.getKey(stack.type).toString());
                dos.writeInt(stack.amount);
            } else {
                dos.writeInt(-1);
            }
        }
    }

    @Override
    public void read(java.io.DataInputStream dis) throws java.io.IOException {
        super.read(dis);
        this.burnTime = dis.readInt();
        this.maxBurnTime = dis.readInt();
        this.cookTime = dis.readInt();
        this.maxCookTime = dis.readInt();
        int size = dis.readInt();
        for (int i = 0; i < size; i++) {
            int slot = dis.readInt();
            if (slot != -1) {
                String itemKey = dis.readUTF();
                int amount = dis.readInt();
                de.delautrer.game.items.Item item = de.delautrer.game.registry.Registries.ITEMS.get(itemKey);
                if (item != null && amount > 0) {
                    inventory.setStack(slot, new ItemStack(item, amount));
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
            // Wenn nicht brennt, aber smelt-bereit ist und Brennstoff hat -> anzünden!
            if (burnTime <= 0 && fuel != null) {
                int currentFuelTime = de.delautrer.game.crafting.FurnaceRecipeManager.getBurnTime(fuel.type);
                if (currentFuelTime > 0) {
                    burnTime = currentFuelTime;
                    maxBurnTime = currentFuelTime;

                    // Brennstoff verbrauchen
                    fuel.amount--;
                    if (fuel.amount <= 0) {
                        inventory.setStack(1, null);
                    } else {
                        inventory.setStack(1, fuel);
                    }
                }
            }

            // Wenn aktiv brennt, Fortschritt erhöhen
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
            // Pausieren
            isBurning = false;
            if (!hasRecipe) {
                cookTime = 0;
            }
        }

        // Block-Status (LIT) aktualisieren, wenn sich der Brennstatus geändert hat
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
        // Input abziehen
        input.amount--;
        if (input.amount <= 0) {
            inventory.setStack(0, null);
        } else {
            inventory.setStack(0, input);
        }

        // Output hinzufügen
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
