package com.civbuddy.snitch.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static com.civbuddy.snitch.utils.JAItemHelper.getHighlight;

@Mixin(AbstractContainerScreen.class)
public class ItemRenderMixin {
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void civbuddy$renderSlot(
            GuiGraphics graphics, Slot slot, int i, int j, CallbackInfo ci
    ) {
        ItemStack stack = slot.getItem();

        if (stack.isEmpty())
            return;


        Optional<Integer> highlight = getHighlight(stack);

        if (highlight.isEmpty()) return;

        graphics.fill(
            slot.x,
            slot.y,
            slot.x + 16,
            slot.y + 16,
            highlight.get()
        );
    }
}
