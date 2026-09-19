package com.civbuddy.snitch.mixins;

import com.civbuddy.snitch.SnitchClient;
import com.civbuddy.snitch.utils.JAItemHelper;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AbstractContainerScreen.class)
public class ItemClickMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void civbuddy$onSlotClicked(
            Slot slot,
            int slotId,
            int button,
            ClickType clickType,
            CallbackInfo ci
    ) {
        if (slot == null || slot.getItem().isEmpty())
            return;

        if (button == 0) {
            ItemStack stack = slot.getItem();

            if (JAItemHelper.isJAItem(stack)) {
                Optional<Vector3i> position = JAItemHelper.getPosition(stack);
                if (position.isEmpty()) return;

                SnitchClient.add(position.get());
            }
        }
    }
}
