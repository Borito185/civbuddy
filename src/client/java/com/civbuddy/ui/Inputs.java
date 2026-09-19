package com.civbuddy.ui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Inputs {
    public static ButtonComponent toggleButton(Supplier<Boolean> value, String trueString, String falseString, Consumer<Boolean> callback) {
        return UIComponents.button(Component.literal(value.get() ? trueString : falseString), btn -> {
            callback.accept(!value.get());
            btn.setMessage(Component.literal(value.get() ? trueString : falseString));
        });
    }

    public static ButtonComponent enumButton(
            Supplier<String> value,
            List<String> options,
            Consumer<String> onChanged
    ) {
        return UIComponents.button(Component.literal(value.get()), btn -> {
            String v = value.get();
            int index = options.indexOf(v);
            index = index+1 % options.size();
            onChanged.accept(options.get(index));

            btn.setMessage(Component.literal(value.get()));
        });
    }

    public static TextBoxComponent numberInput(
            Supplier<Number> value,
            Consumer<Double> onChanged
    ) {
        var input = UIComponents.textBox(
                Sizing.expand(),
                value.get().toString()
        );

        input.onChanged().subscribe(text -> {
            try {
                onChanged.accept(Double.parseDouble(text));
            } catch (NumberFormatException ignored) { }
        });

        input.focusLost().subscribe(() -> {
            String text = input.getValue();
            String newText = value.get().toString();
            if (!Objects.equals(newText, text)) {
                input.text(newText);
            }
        });

        return input;
    }

    public static FlowLayout vector3iInput(
            Supplier<Vector3i> value,
            Consumer<Vector3i> onChanged
    ) {
        var layout = UIContainers.horizontalFlow(
                Sizing.expand(),
                Sizing.content()
        );

        layout.gap(4);

        layout.child(numberInput(
                () -> value.get().x,
                x -> {
                    var current = value.get();
                    onChanged.accept(new Vector3i(
                            x.intValue(),
                            current.y,
                            current.z
                    ));
                }
        ).horizontalSizing(Sizing.fill(33)));

        layout.child(numberInput(
                () -> value.get().y,
                y -> {
                    var current = value.get();
                    onChanged.accept(new Vector3i(
                            current.x,
                            y.intValue(),
                            current.z
                    ));
                }
        ).horizontalSizing(Sizing.fill(33)));

        layout.child(numberInput(
                () -> value.get().z,
                z -> {
                    var current = value.get();
                    onChanged.accept(new Vector3i(
                            current.x,
                            current.y,
                            z.intValue()
                    ));
                }
        ).horizontalSizing(Sizing.fill(33)));

        return layout;
    }

    public static ColorPickerComponent colorInput(
            Supplier<Vector4f> value,
            Consumer<Vector4f> onChanged
    ) {
        var current = value.get();

        var picker = new ColorPickerComponent();

        picker.showAlpha(true)
                .selectedColor(new Color(
                        current.x,
                        current.y,
                        current.z,
                        current.w
                ))
                .sizing(
                        Sizing.fixed(160),
                        Sizing.fixed(100)
                );

        picker.onChanged().subscribe(color ->
                onChanged.accept(new Vector4f(
                        color.red(),
                        color.green(),
                        color.blue(),
                        color.alpha()
                ))
        );

        return picker;
    }
}
