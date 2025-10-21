package com.civbuddy.calc;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.objecthunter.exp4j.ExpressionBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

import java.math.BigDecimal;
import java.util.Map;

public class CalculatorClient {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, Double> shortcuts = Map.of(
        "s", 64.0d,
        "ci", 64.0d,
        "cs", 64.0d*64d,
        "k", 1000d
    );

    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess) -> commandDispatcher.register(
                ClientCommandManager.literal("calc")
                        .then(
                                ClientCommandManager.argument("expression", StringArgumentType.greedyString()).executes(this::calc)
                        )
        )));
    }
    public int calc(CommandContext<FabricClientCommandSource> ctx) {
        String exp = StringArgumentType.getString(ctx, "expression");
        double result = eval(exp);

        String resultString = BigDecimal.valueOf(result)
                .stripTrailingZeros()
                .toPlainString();

        MutableText prefix = Text.literal("calc")
                .styled(s -> s.withColor(Formatting.GOLD).withBold(true));

        MutableText sep = Text.literal(": ")
                .styled(s -> s.withColor(Formatting.DARK_GRAY));

        MutableText value = Text.literal(resultString).styled(s -> s
                .withColor(Formatting.AQUA)
                .withBold(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(resultString))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy").formatted(Formatting.GRAY)))
        );

        MutableText hint = Text.literal("  [copy]")
                .styled(s -> s.withColor(Formatting.GREEN)
                                .withClickEvent(new ClickEvent.CopyToClipboard(resultString))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy").formatted(Formatting.GRAY))));

        mc.player.sendMessage(prefix.append(sep).append(value).append(hint), false);
        return 0;
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
