package com.civbuddy.veins.listeners;

import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.config.VeinConfig;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import com.civbuddy.veins.geo.shapes.AABBShape;
import com.civbuddy.veins.geo.shapes.VoxelShape;
import com.civbuddy.veins.render.ShapeRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.sql.SQLException;
import java.util.Set;

import static com.civbuddy.veins.VeinClient.config;

public final class RightClickListener {
    private static ShapeRenderer highlightRenderer;
    private static int selectionTicks = 0;

    private RightClickListener() {}

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(RightClickListener::onTick);
        highlightRenderer = new ShapeRenderer();
    }

    public static void onTick(MinecraftClient client) {
        try {
            checkAction(client);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkAction(MinecraftClient client) throws SQLException {
        VeinConfig config = config();
        if (null == client.player) return;
        if (null == client.mouse) return;
        if (null == client.world) return;
        Item item = client.player.getInventory().getSelectedStack().getItem();

        boolean isHoldingPickaxe = item.getName().toString().contains("pickaxe");
        boolean isHolding = client.options.useKey.isPressed();
        boolean released = !isHolding && selectionTicks > 0;
        boolean isCharged = selectionTicks > config.placeDelayTicks;

        int chargeTime = Math.clamp(selectionTicks - config.placeDelayTicks, 0, config.getMaxTicks());
        if (!isHoldingPickaxe || released) {
            selectionTicks = 0;
            highlightRenderer.setInnerShapes(Set.of());
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
                .mul(config.placeMoveSpeed)
                .mul(chargeTime)
                .add(playerPos.toVector3f())
                .floor();
        if (released && isCharged) {
            addSelection(new Vector3i(targetedBlock, 2));
        }
        if (isHolding && isCharged) {
            AABBShape aabb = AABBShape.of(new Vector3i(targetedBlock, 2), new Vector3i(0));

            highlightRenderer.setStyle(config.highlightWallColor, config.highlightHasGrid);
            highlightRenderer.setInnerShapes(Set.of(aabb));
        }
    }

    private static void addSelection(Vector3i pos) throws SQLException {
        VeinMarkingRow row = new VeinMarkingRow(VeinClient.getActiveVeinId(), pos, config().markRange);
        VeinMarkingDao.upsert(row);
        VeinClient.notifyChange();
    }

    private static void removeTargetedBlock(Vec3d cameraPos, Vec3d cameraDir) throws SQLException {
        AABBShape closest = null;
        float closestDist = Float.MAX_VALUE;

        Vec3d closeEnd = cameraPos.subtract(cameraDir);
        Vec3d farEnd = cameraPos.add(cameraDir.multiply(1000));

        for (VoxelShape shape : VeinClient.getInstance().getCurrentMarkings()) {
            if (!(shape instanceof AABBShape bounds)) continue;
            if (!bounds.intersectsCenter(closeEnd, farEnd)) continue;

            float distance = new Vector3f(bounds.center()).add(0.5f,0.5f,0.5f).distance(cameraPos.toVector3f());
            if (distance >= closestDist) continue;

            closest = bounds;
            closestDist = distance;
        }

        if (closest == null) return;

        VeinMarkingDao.delete(VeinClient.getActiveVeinId(), closest.center());
        VeinClient.notifyChange();
    }
}
