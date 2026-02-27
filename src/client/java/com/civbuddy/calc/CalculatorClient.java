package com.civbuddy.calc;

import com.civbuddy.utils.CommandsHelper;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.math.BigDecimal;
import java.util.Map;

import static com.civbuddy.utils.CommandsHelper.andRespondWith;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CalculatorClient implements CommandsHelper.CommandProvider {
    private CalculatorClient() {}
    private static final Map<String, Double> shortcuts = Map.of(
        "s", 64.0d,
        "b", 9.0d,
        "ci", 64.0d,
        "cs", 64.0d*64d,
        "k", 1000d
    );

    public static void onInitializeClient() {
        CalculatorClient instance = new CalculatorClient();
        CommandsHelper.register(instance);
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> commands() {
        return literal("calc").then(
                argument("expression", greedyString())
                        .executes(andRespondWith(CalculatorClient::calc))
        );
    }

    @Override
    public boolean commandsAlias() {
        return true;
    }

    public static Component calc(CommandContext<FabricClientCommandSource> ctx) {
        String exp = StringArgumentType.getString(ctx, "expression");
        exp = exp.toLowerCase();
        double result = eval(exp);
        String resultString = BigDecimal.valueOf(result)
                .stripTrailingZeros()
                .toPlainString();

        MutableComponent value = Component.literal(resultString).withStyle(s -> s
                .withColor(ChatFormatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(resultString))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(asCsCi(result)).withStyle(ChatFormatting.GRAY)))
        );

        MutableComponent hint = Component.literal(" [copy]")
                .withStyle(s -> s.withColor(ChatFormatting.YELLOW)
                                .withClickEvent(new ClickEvent.CopyToClipboard(resultString))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy").withStyle(ChatFormatting.GRAY))));

        return value.append(hint);
    }

    private static String asCsCi(double value) {
        long cs = (long)Math.floor(value / shortcuts.get("cs"));
        value -= cs * shortcuts.get("cs");
        long ci = (long)Math.floor(value / shortcuts.get("ci"));
        value -= ci * shortcuts.get("ci");

        StringBuilder sb = new StringBuilder();

        if (cs != 0)    sb.append(cs).append(" CS ");
        if (ci != 0)    sb.append(ci).append(" CI ");
        if (value != 0) sb.append(value % 1 == 0 ? (long) value : value);

        return sb.toString().trim();
    }

    public static double eval(String s) {
        return new ExpressionBuilder(s)
                .variables(shortcuts.keySet())
                .implicitMultiplication(true)
                .build()
                .setVariables(shortcuts)
                .evaluate();
    }
}
