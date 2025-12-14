package com.civbuddy.veins.commands;

import com.civbuddy.veins.data.VeinDao;
import com.civbuddy.veins.data.VeinKVStore;
import com.civbuddy.veins.data.VeinRow;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class InfoCommands {
    /**
     * Command: List all veins
     */
    public static Text cmdList(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        List<VeinRow> veins = VeinDao.top(10);
        VeinRow currentVein = VeinDao.getOrCreate(VeinKVStore.getActiveVeinName());
        if (!veins.contains(currentVein)) veins.add(currentVein);

        veins.removeIf(v -> Objects.equals(v.name(), "default"));

        if (veins.isEmpty()) {
            return Text.literal("§7No veins tracked yet");
        }
        MutableText text = Text.literal("\n§b━━━ Tracked Veins ━━━\n");
        veins.stream()
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .limit(10)
                .forEach(vein -> {
                    String active = vein.equals(currentVein) ? " §a✓" : "";
                    text.append(Text.literal(String.format("§7Key: §e%s §7Count: §a%d%s\n",
                            vein.name(), vein.count(), active)));
                });
        text.append(Text.literal("§b━━━━━━━━━━━━━━━━━━"));
        return text;
    }
}
