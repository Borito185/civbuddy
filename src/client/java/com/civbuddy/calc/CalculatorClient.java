package com.civbuddy.calc;

import com.civbuddy.utils.CommandsHelper;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    public static Text calc(CommandContext<FabricClientCommandSource> ctx) {
        String exp = StringArgumentType.getString(ctx, "expression");
        exp = exp.toLowerCase();
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

    public static double eval(String s) {
        return new ExpressionBuilder(s)
                .variables(shortcuts.keySet())
                .implicitMultiplication(true)
                .build()
                .setVariables(shortcuts)
                .evaluate();
    }
}
