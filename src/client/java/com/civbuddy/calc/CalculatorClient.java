package com.civbuddy.calc;

import com.civbuddy.utils.CommandsHelper;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.objecthunter.exp4j.ExpressionBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

import java.math.BigDecimal;
import java.util.Map;

import static com.civbuddy.utils.CommandsHelper.andRespondWith;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static  net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static  net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class CalculatorClient implements CommandsHelper.CommandProvider {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, Double> shortcuts = Map.of(
        "s", 64.0d,
        "ci", 64.0d,
        "cs", 64.0d*64d,
        "k", 1000d
    );

    public void onInitializeClient() {
        CommandsHelper.register(this);
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> commands() {
        return literal("calc").then(argument("expression", greedyString()).executes(andRespondWith(this::calc)));
    }

    @Override
    public boolean commandsAlias() {
        return true;
    }

    public Text calc(CommandContext<FabricClientCommandSource> ctx) {
        String exp = StringArgumentType.getString(ctx, "expression");
        double result = eval(exp);

        String resultString = BigDecimal.valueOf(result)
                .stripTrailingZeros()
                .toPlainString();

        MutableText value = Text.literal(resultString).styled(s -> s
                .withColor(Formatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(resultString))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy").formatted(Formatting.GRAY)))
        );

        MutableText hint = Text.literal(" [copy]")
                .styled(s -> s.withColor(Formatting.YELLOW)
                                .withClickEvent(new ClickEvent.CopyToClipboard(resultString))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy").formatted(Formatting.GRAY))));

        return value.append(hint);
    }

    public double eval(String s) {
        return new ExpressionBuilder(s)
                .variables(shortcuts.keySet())
                .implicitMultiplication(true)
                .build()
                .setVariables(shortcuts)
                .evaluate();
    }
}
