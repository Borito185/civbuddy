package com.civbuddy.veins.commands;

import com.civbuddy.common.commands.Command;
import com.civbuddy.veins.data.VeinDao;
import com.civbuddy.veins.data.VeinKVStore;
import com.civbuddy.veins.data.VeinRow;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import static com.civbuddy.common.commands.CommandUtils.*;

public class InfoCommands implements Command {
    @Override
    public void bind(Consumer<List<ArgumentBuilder<FabricClientCommandSource, ?>>> add) {
        add.accept(List.of(
                literal("info").executes(andRespondWith(InfoCommands::writeInfo))
        ));
    }


    public static Component writeInfo(CommandContext<FabricClientCommandSource> ctx) throws SQLException {
        String veinName = VeinKVStore.getActiveVeinName();
        long veinId = VeinDao.getOrCreateId(veinName);
        VeinRow vein = VeinDao.getOrCreate(veinName);
        List<VeinMarkingRow> markings = VeinMarkingDao.findAllForVein(veinId);

        int blockCount = markings.size();
        int diamondsFoundBySelf = vein.count();

        // Center (average)
        Vector3f average = new Vector3f(0);
        for (VeinMarkingRow marking : markings) {
            average.add(new Vector3f(marking.pos()).div(blockCount));
        }
        Vector3i center = new Vector3i(average, 2);

        // Bounds / size
        int minX = markings.stream().mapToInt(m -> m.pos().x()).min().orElse(0);
        int minY = markings.stream().mapToInt(m -> m.pos().y()).min().orElse(0);
        int minZ = markings.stream().mapToInt(m -> m.pos().z()).min().orElse(0);
        int maxX = markings.stream().mapToInt(m -> m.pos().x()).max().orElse(0);
        int maxY = markings.stream().mapToInt(m -> m.pos().y()).max().orElse(0);
        int maxZ = markings.stream().mapToInt(m -> m.pos().z()).max().orElse(0);

        Vector3i size = new Vector3i(
                maxX - minX + 1,
                maxY - minY + 1,
                maxZ - minZ + 1
        );

        if (blockCount == 0) size.mul(0);

        String text =
                "§6§lVein Info\n" +
                        "§7Name: §b" + veinName + "\n" +
                        "§7Blocks marked: §e" + blockCount + "\n" +
                        "§7Diamonds found (you): §a" + diamondsFoundBySelf + "\n" +
                        "§7Center: §9" + center.x() + ", " + center.y() + ", " + center.z() + "\n" +
                        "§7Size: §d" + size.x() + " × " + size.y() + " × " + size.z();

        return Component.literal(text);
    }
}
