package com.civbuddy.veins.listeners;

import com.civbuddy.veins.VeinClient;
import com.civbuddy.veins.config.VeinConfig;
import com.civbuddy.veins.data.markings.VeinMarkingDao;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import com.civbuddy.veins.geo.shapes.AABBShape;
import com.civbuddy.veins.geo.shapes.VoxelShape;
import com.civbuddy.veins.render.ShapeRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3i;
import java.sql.SQLException;
import java.util.Set;

import static com.civbuddy.CivBuddyClient.WORKER;
import static com.civbuddy.veins.VeinClient.config;

public final class RightClickListener {
    private static ShapeRenderer highlightRenderer;
    private static int selectionTicks = 0;

    private RightClickListener() {}

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(RightClickListener::onTick);
        highlightRenderer = new ShapeRenderer();
    }

    public static void onTick(Minecraft client) {
        checkAction(client);
    }

    private static void checkAction(Minecraft client) {
        VeinConfig config = config();
        if (null == client.player) return;
        if (null == client.mouseHandler) return;
        if (null == client.level) return;
        if (!config.doRender) return;
        Item item = client.player.getInventory().getSelectedItem().getItem();

        boolean isHoldingPickaxe = item.getName().toString().contains("pickaxe");
        boolean isHolding = client.options.keyUse.isDown();
        boolean released = !isHolding && selectionTicks > 0;
        boolean isCharged = selectionTicks > config.placeDelayTicks;

        int chargeTime = Math.clamp(selectionTicks - config.placeDelayTicks, 0, config.getMaxTicks());
        if (!isHoldingPickaxe || released) {
            selectionTicks = 0;
            highlightRenderer.setInnerShapes(Set.of());
        }
        if (isHolding) selectionTicks++;
        if (!isHoldingPickaxe || (!isHolding && !released) || (isHolding && !isCharged)) return;

        Vec3 playerPos = client.player.getEyePosition();
        Vec3 playerDir = client.player.getLookAngle();

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

    private static void addSelection(Vector3i pos) {
        WORKER.submit(() -> {
            try {
                VeinMarkingRow row = new VeinMarkingRow(VeinClient.getActiveVeinId(), pos, VeinClient.config().markRange);
                VeinMarkingDao.upsert(row);
                VeinClient.notifyChange();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void removeTargetedBlock(Vec3 cameraPos, Vec3 cameraDir) {
        WORKER.submit(() -> {
            try {
                AABBShape closest = null;
                float closestDist = Float.MAX_VALUE;

                Vec3 closeEnd = cameraPos.subtract(cameraDir);
                Vec3 farEnd = cameraPos.add(cameraDir.scale(1000));

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
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
