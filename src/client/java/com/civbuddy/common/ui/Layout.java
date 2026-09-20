package com.civbuddy.common.ui;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class Layout {
    public static void header(FlowLayout options, String text) {
        options.child(
                UIComponents.label(
                                Component.literal(text)
                                        .withStyle(ChatFormatting.BOLD)
                        )
                        .shadow(true)
                        .margins(Insets.top(12).withBottom(4))
                        .horizontalSizing(Sizing.fill(100))
        );
    }

    public static LabelComponent label(String text) {
        return UIComponents.label(Component.literal(text));
    }

    public static FlowLayout row(String name, UIComponent control) {
        var row = UIContainers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
        );

        row.verticalAlignment(VerticalAlignment.CENTER);

        var labelArea = UIContainers.horizontalFlow(
                Sizing.fill(30),
                Sizing.content()
        );

        labelArea.verticalAlignment(VerticalAlignment.CENTER);
        labelArea.child(UIComponents.label(Component.literal(name)));

        var controlArea = UIContainers.horizontalFlow(
                Sizing.fill(70),
                Sizing.content()
        );

        control.horizontalSizing(Sizing.fill(100));

        controlArea.verticalAlignment(VerticalAlignment.CENTER);
        controlArea.child(control);

        row.child(labelArea);
        row.child(controlArea);

        return row;
    }
}
