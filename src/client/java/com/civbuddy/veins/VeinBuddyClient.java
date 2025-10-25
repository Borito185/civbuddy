package com.civbuddy.veins;

import java.util.Collection;
import java.util.List;

import com.civbuddy.Save;
import com.civbuddy.utils.CommandsHelper;
import com.civbuddy.veins.geo.AABBShape;
import com.civbuddy.veins.geo.CompoundShape;
import com.civbuddy.veins.geo.VoxelShape;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.text.Text;
import org.joml.Vector3f;
import org.joml.Vector3i;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import static com.civbuddy.utils.CommandsHelper.andRespondWith;
import static com.civbuddy.Save.data;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class VeinBuddyClient implements CommandsHelper.CommandProvider {
    private final static float speed = 0.2f;
    private final static float placeRange = 6.0f;
    private final static int maxTicks = (int) (placeRange / speed);
    private final static int delay = 5;

    private int selectionTicks = 0;

    private SimpleRenderer staticRenderer;
    private SimpleRenderer dynamicRenderer;

    private CompoundShape ranges = new CompoundShape();
    private CompoundShape selections = new CompoundShape();

    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        dynamicRenderer = new SimpleRenderer(true, false);
        staticRenderer = new SimpleRenderer();

        Save.SAVE_LOADED.register(save -> {
            ranges.clear();
            selections.clear();
            addSelection(save.selections);
        });

        // Initialize VeinBuddy Count system
        VeinBuddyCount.initialize();

        CommandsHelper.register(this);
    }

    private Text onDigRange(CommandContext<FabricClientCommandSource> ctx) {
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int y = IntegerArgumentType.getInteger(ctx, "y");
        int z = IntegerArgumentType.getInteger(ctx, "z");
        data.digRange = new Vector3i(x, y, z);
        Save.save();

        return Text.literal(String.format("§aChanged dig range to: %d %d %d", x, y, z));
    }

    private Text onChangeAllDigRange(CommandContext<FabricClientCommandSource> ctx) {
        int rad = IntegerArgumentType.getInteger(ctx, "radius");
        Vector3i radius = new Vector3i(rad);
        List<AABBShape> list = data.selections
                .stream()
                .map(s -> new AABBShape(s.center(), radius, s.color(), s.hasGrid()))
                .toList();
        clear();
        addSelection(list);
        return Text.literal(String.format("§aChanged dig range to: %d %d %d for %d markings", rad, rad, rad, list.size()));
    }

    private Text onDigRadius(CommandContext<FabricClientCommandSource> ctx) {
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        data.digRange = new Vector3i(radius);
        Save.save();

        return Text.literal(String.format("§aChanged dig range to: %d %d %d", radius, radius, radius));
    }

    private Text toggleRenderer(CommandContext<FabricClientCommandSource> fabricClientCommandSourceCommandContext) {
        data.render = !data.render;

        drawStatic();
        return Text.literal(String.format("§aVein rendering turned %s", data.render ? "on" : "off"));
    }

    private void clear() {
        data.selections.clear();
        ranges.clear();
        selections.clear();
    }

    private Text clear(CommandContext<FabricClientCommandSource> fabricClientCommandSourceCommandContext) {
        int size = data.selections.size();
        clear();
        drawStatic();
        return Text.literal(String.format("§aCleared %d markings", size));
    }

    private void onTick(MinecraftClient client) {
        if (null == client.player) return;
        if (null == client.mouse) return;
        if (null == client.world) return;
        Item item = client.player.getInventory().getSelectedStack().getItem();

        boolean isHoldingPickaxe = item.getName().toString().contains("pickaxe");
        boolean isHolding = client.mouse.wasRightButtonClicked();
        boolean released = !isHolding && selectionTicks > 0;
        boolean isCharged = selectionTicks > delay;

        int chargeTime = Math.clamp(selectionTicks - delay, 0, maxTicks);
        if (!isHoldingPickaxe || released) {
            selectionTicks = 0;
            dynamicRenderer.clear();
        }
        if (isHolding) selectionTicks++;
        if (!isHoldingPickaxe || (!isHolding && !released) || (isHolding && !isCharged)) return;

        Vec3d playerPos = client.player.getEyePos();
        Vec3d playerDir = client.player.getRotationVector();


        if (released && !isCharged) {
            removeTargetedBlock(playerPos, playerDir);
            return;
        }
        Vector3f targetedBlock = playerDir.toVector3f();
        targetedBlock = targetedBlock
            .mul(speed)
            .mul(chargeTime)
            .add(playerPos.toVector3f())
            .floor();
        if (released && isCharged) {
            addSelection(new Vector3i(targetedBlock, 2));
        }
        if (isHolding && isCharged) {
            AABBShape aabb = new AABBShape(new Vector3i(targetedBlock, 2), new Vector3i(0), data.highlightWallColor, true);

            dynamicRenderer.draw(List.of(aabb));
        }
    }

    public void addSelection(Vector3i pos) {
        addSelection(pos, data.digRange);
    }

    public void addSelection(Vector3i pos, Vector3i range) {
        addSelection(List.of(new AABBShape(pos, range, data.rangeWallColor, true)));
    }

    public void addSelection(Collection<AABBShape> shapes) {
        boolean change = data.selections.addAll(shapes);

        ranges.add(shapes.stream().map(s -> (VoxelShape)s).toList());
        selections.add(shapes.stream().map(s -> (VoxelShape)new AABBShape(s.center(), new Vector3i(0), data.selectionWallColor, false)).toList());

        drawStatic();
        if (change) Save.save();
    }

    public void removeTargetedBlock(Vec3d cameraPos, Vec3d cameraDir) {
        AABBShape closest = null;
        float closestDist = Float.MAX_VALUE;

        Vec3d closeEnd = cameraPos.subtract(cameraDir);
        Vec3d farEnd = cameraPos.add(cameraDir.multiply(1000));

        for (AABBShape bounds : data.selections) {
            if (!bounds.intersectsCenter(closeEnd, farEnd)) continue;

            float distance = new Vector3f(bounds.center()).add(0.5f,0.5f,0.5f).distance(cameraPos.toVector3f());
            if (distance >= closestDist) continue;

            closest = bounds;
            closestDist = distance;
        }

        if (closest == null) return;

        data.selections.remove(closest);
        ranges.remove(closest);
        selections.removeAt(closest.center());

        drawStatic();
    }

    private void drawStatic() {
        List<VoxelShape> shapes = List.of(ranges, selections);

        if (!data.render)
            shapes = List.of();
        staticRenderer.draw(shapes);
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> commands() {
        return literal("veins")
            .then(literal("digRange").then(argument("x", integer(1, 11)).then(argument("y", integer(1, 11)).then(argument("z", integer(1, 11))
                    .executes(andRespondWith(this::onDigRange))))))
            .then(literal("digRadius").then(argument("radius", integer(1, 11))
                    .executes(andRespondWith(this::onDigRadius))))
            .then(literal("clearAll")
                    .executes(andRespondWith(this::clear)))
            .then(literal("toggleRenderer")
                    .executes(andRespondWith(this::toggleRenderer)))
            .then(literal("changeAll")
                    .then(literal("digRadius").then(argument("radius", integer(1, 11))
                            .executes(andRespondWith(this::onChangeAllDigRange))))
            );
    }

    @Override
    public boolean commandsAlias() {
        return true;
    }
}
