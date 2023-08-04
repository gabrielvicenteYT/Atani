package wtf.atani.module.impl.player;

import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;

import wtf.atani.event.events.GuiHandleEvent;
import wtf.atani.event.radbus.Listen;
import wtf.atani.module.Module;
import wtf.atani.module.data.ModuleInfo;
import wtf.atani.module.data.enums.Category;
import wtf.atani.utils.math.random.RandomUtil;
import wtf.atani.utils.math.time.TimeHelper;
import wtf.atani.value.impl.CheckBoxValue;
import wtf.atani.value.impl.SliderValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ModuleInfo(category = Category.PLAYER, description = "Automatically takes items in chests", name = "ChestStealer")
public class Stealer extends Module {

    private final SliderValue<Long> minStartDelay = new SliderValue<>("Minimum Start Delay", "What will be the minimum delay for starting to operate?", this, 250L, 0L, 1000L, 0);
    private final SliderValue<Long> maxStartDelay = new SliderValue<>("Maximum Start Delay", "What will be the maximum delay for starting to operate?", this, 300L, 0L, 1000L, 0);
    private final SliderValue<Long> minGrabDelay = new SliderValue<>("Minimum Grab Delay", "What will be the minimum delay between grabbing each item?", this, 250L, 0L, 1000L, 0);
    private final SliderValue<Long> maxGrabDelay = new SliderValue<>("Maximum Grab Delay", "What will be the maximum delay between grabbing each item?", this, 300L, 0L, 1000L, 0);
    private final CheckBoxValue intelligent = new CheckBoxValue("Intelligent", "Pick items intelligently?", this, true);
    private final CheckBoxValue stackItems = new CheckBoxValue("Stack items", "Steal items of the same type without reseting the timer?", this, false);
    private final CheckBoxValue randomPick = new CheckBoxValue("Random Pick", "Pick items randomly?", this, true);
    private final CheckBoxValue autoClose = new CheckBoxValue("Auto Close", "Close automatically?", this, true);

    private final TimeHelper timeHelper = new TimeHelper();
    private final TimeHelper startTimer = new TimeHelper();
    private final List<Integer> itemsToSteal = new ArrayList<>();

    @Listen
    public final void onGui(GuiHandleEvent guiHandleEvent) {
        if (mc.currentScreen == null) {
            startTimer.reset();
            return;
        }

        if (mc.currentScreen instanceof GuiChest) {
            if (!startTimer.hasReached(RandomUtil.randomBetween(minStartDelay.getValue(), maxStartDelay.getValue())))
                return;
            itemsToSteal.clear();


            final ContainerChest chest = (ContainerChest) getPlayer().openContainer;
            final IInventory inventory = chest.getLowerChestInventory();
            boolean isEmpty = true;

            if (intelligent.getValue()) {
                addIntelligentSlotsToSteal();
            } else {
                for (int i = 0; i < inventory.getSizeInventory(); i++) {
                    final ItemStack stack = inventory.getStackInSlot(i);
                    if (stack != null) {
                        itemsToSteal.add(i);
                    }
                }
            }

            if (randomPick.getValue())
                Collections.shuffle(itemsToSteal);

            for (final int i : itemsToSteal) {
                final ItemStack stack = inventory.getStackInSlot(i);
                if (stack != null) {
                    // Commented out for picking multiple items within a blink of an eye randomly.
                    //double grabDelay = RandomUtil.INSTANCE.randomInRange(this.minGrabDelay, this.maxGrabDelay);

                    double grabDelay = RandomUtil.randomBetween(minGrabDelay.getValue(), maxGrabDelay.getValue());
                    if (!timeHelper.hasReached((long) (grabDelay)))
                        return;
                    if (stackItems.getValue() && stack.stackSize != 64 && stack.getMaxStackSize() != 1 && getItemSize(stack.getItem(), inventory) != 0 && getItemSize(stack.getItem(), inventory) != 1) {
                        getPlayerController().windowClick(chest.windowId, i, 0, 0, mc.thePlayer);
                        getPlayerController().windowClick(chest.windowId, i, 0, 6, mc.thePlayer);
                        getPlayerController().windowClick(chest.windowId, i, 0, 0, mc.thePlayer);
                        getPlayerController().windowClick(chest.windowId, i, 0, 1, mc.thePlayer);
                    } else {
                        getPlayerController().windowClick(chest.windowId, i, 0, 1, mc.thePlayer);
                    }
                    timeHelper.reset();
                    isEmpty = false;
                }
            }

            if (isEmpty && autoClose.getValue())
                getPlayer().closeScreen();
        }
    };

    private void addIntelligentSlotsToSteal() {
        float bestSwordDamage = -1, bestBowDamage = -1, bestPickAxeStrength = -1, bestAxeStrength = -1;
        float bestBootsProtection = -1, bestLeggingsProtection = -1, bestChestPlateProtection = -1, bestHelmetProtection = -1;
        //searching in inventory
        for (int i = 9; i < 45; i++) {
            final Slot slot = getPlayer().inventoryContainer.getSlot(i);
            if (slot != null && slot.getStack() != null) {
                final ItemStack stack = slot.getStack();
                if (stack != null) {
                    final Item item = stack.getItem();
                    if (item instanceof ItemSword) {
                        final float damage = 4 + ((ItemSword) stack.getItem()).getDamageVsEntity() + (EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25f);
                        if (damage > bestSwordDamage) {
                            bestSwordDamage = damage;
                        }
                    } else if (item instanceof ItemBow) {
                        bestBowDamage = 4.5f;
                    } else if (item instanceof ItemTool) {
                        Block block = null;
                        if (item instanceof ItemPickaxe) {
                            block = Blocks.stone;
                        } else if (item instanceof ItemAxe) {
                            block = Blocks.log;
                        }
                        final float toolStrength = item.getStrVsBlock(stack, block);
                        if (item instanceof ItemPickaxe) {
                            if (toolStrength > bestPickAxeStrength) {
                                bestPickAxeStrength = toolStrength;
                            }
                        } else if (item instanceof ItemAxe) {
                            if (toolStrength > bestAxeStrength) {
                                bestAxeStrength = toolStrength;
                            }
                        }
                    } else if (item instanceof ItemArmor) {
                        final float prot = ((ItemArmor) stack.getItem()).damageReduceAmount + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.04f;
                        switch (((ItemArmor) item).armorType) {
                            case 0:
                                if (prot > bestHelmetProtection) {
                                    bestHelmetProtection = prot;
                                }
                                break;
                            case 1:
                                if (prot > bestChestPlateProtection) {
                                    bestChestPlateProtection = prot;
                                }
                                break;
                            case 2:
                                if (prot > bestLeggingsProtection) {
                                    bestLeggingsProtection = prot;
                                }
                                break;
                            case 3:
                                if (prot > bestBootsProtection) {
                                    bestBootsProtection = prot;
                                }
                                break;
                        }
                    }
                }
            }
        }

        int bestSwordSlot = -1, bestBowSlot = -1, bestPickAxeSlot = -1, bestAxeSlot = -1, bestBootsSlot = -1, bestLeggingsSlot = -1, bestChestPlateSlot = -1, bestHelmetSlot = -1;
        for (int i = 0; i < ((ContainerChest) getPlayer().openContainer).getLowerChestInventory().getSizeInventory(); i++) {
            final ItemStack stack = getPlayer().openContainer.getSlot(i).getStack();
            if (stack != null) {
                final Item item = getPlayer().openContainer.getSlot(i).getStack().getItem();
                if (item instanceof ItemSword) {
                    final float damage = 4 + ((ItemSword) stack.getItem()).getDamageVsEntity() + (EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25f);
                    if (damage > bestSwordDamage) {
                        bestSwordDamage = damage;
                        bestSwordSlot = i;
                    }
                } else if (item instanceof ItemBow) {
                    if (4.5f > bestBowDamage) {
                        bestBowDamage = 4.5f;
                        bestBowSlot = i;
                    }
                } else if (item instanceof ItemTool) {
                    Block block = null;
                    if (item instanceof ItemPickaxe) {
                        block = Blocks.stone;
                    } else if (item instanceof ItemAxe) {
                        block = Blocks.log;
                    }
                    final float toolStrength = item.getStrVsBlock(stack, block);
                    if (item instanceof ItemPickaxe) {
                        if (toolStrength > bestPickAxeStrength) {
                            bestPickAxeStrength = toolStrength;
                            bestPickAxeSlot = i;
                        }
                    } else if (item instanceof ItemAxe) {
                        if (toolStrength > bestAxeStrength) {
                            bestAxeStrength = toolStrength;
                            bestAxeSlot = i;
                        }
                    }
                } else if (item instanceof ItemArmor) {
                    final float prot = ((ItemArmor) stack.getItem()).damageReduceAmount + EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.04f;
                    switch (((ItemArmor) item).armorType) {
                        case 0:
                            if (prot > bestHelmetProtection) {
                                bestHelmetProtection = prot;
                                bestHelmetSlot = i;
                            }
                            break;
                        case 1:
                            if (prot > bestChestPlateProtection) {
                                bestChestPlateProtection = prot;
                                bestChestPlateSlot = i;
                            }
                            break;
                        case 2:
                            if (prot > bestLeggingsProtection) {
                                bestLeggingsProtection = prot;
                                bestLeggingsSlot = i;
                            }
                            break;
                        case 3:
                            if (prot > bestBootsProtection) {
                                bestBootsProtection = prot;
                                bestBootsSlot = i;
                            }
                            break;
                    }
                } else {
                    itemsToSteal.add(i);
                }
            }
        }
        itemsToSteal.add(bestSwordSlot);
        itemsToSteal.add(bestBowSlot);
        itemsToSteal.add(bestPickAxeSlot);
        itemsToSteal.add(bestAxeSlot);
        itemsToSteal.add(bestHelmetSlot);
        itemsToSteal.add(bestChestPlateSlot);
        itemsToSteal.add(bestLeggingsSlot);
        itemsToSteal.add(bestBootsSlot);
    }

    public int getItemSize(Item item, IInventory inventory) {
        int size = 0;
        for(int i = 0; i < inventory.getSizeInventory(); i++) {
            if(inventory.getStackInSlot(i) != null) {
                ItemStack stack = inventory.getStackInSlot(i);
                if(stack.getItem().equals(item) && stack.stackSize != stack.getMaxStackSize())
                    size++;
            }
        }
        return size;
    }


    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}