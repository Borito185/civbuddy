package com.veinbuddy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.Item;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.*;
import java.io.File;
import java.lang.Math;
import java.util.*;

public class VeinBuddyClient implements ClientModInitializer {

  private final static MinecraftClient mc = MinecraftClient.getInstance();
  private final static float speed = 0.2f;
  private final static float placeRange = 6.0f;
  private final static int maxTicks = (int) (placeRange / speed);
  private final static int delay = 5;



  private int selectionTicks = 0;

  private SimpleRenderer staticRenderer;
  private SimpleRenderer dynamicRenderer;

  private SaveLoader.Save save = new SaveLoader.Save();
  private boolean isDirty;

  @Override
  public void onInitializeClient() {
    SharedConstants.isDevelopment = true;

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin(client));
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onLeave(client));
    ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    ClientTickEvents.END_CLIENT_TICK.register(this::save);

    dynamicRenderer = new SimpleRenderer(true, false);
    staticRenderer = new SimpleRenderer();

    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
      ClientCommandManager.literal("veinbuddy")
              .then(ClientCommandManager.literal("digRange")
                      .then(ClientCommandManager.argument("x", IntegerArgumentType.integer(1, 10))
                              .then(ClientCommandManager.argument("y", IntegerArgumentType.integer(1, 10))
                                      .then(ClientCommandManager.argument("z", IntegerArgumentType.integer(1, 10))
                                              .executes(this::onDigRange)))))
              .then(ClientCommandManager.literal("debug").executes(this::debug))
    ));
  }

  private void onJoin(MinecraftClient client) {
    // load save
    File saveFile = getSaveFile(client);
    save = SaveLoader.load(saveFile);

    redrawStatic();
  }

  private void onLeave(MinecraftClient client) {
    // can i get uuhhhhhh
  }

  private int debug(CommandContext<FabricClientCommandSource> ctx) {
    save.selections.add(new Bounds(new Vector3i(0,10,0), new Vector3i(5)));
    save.selections.add(new Bounds(new Vector3i(3,7,0), new Vector3i(3)));
    redrawStatic();
    return 1;
  }

  private File getSaveFile(MinecraftClient client) {
    ServerInfo serverInfo = client.getCurrentServerEntry();
    IntegratedServer server = client.getServer();

    String key = null;

    if (server != null) {
      key = server.getSaveProperties().getLevelName();
    }

    if (serverInfo != null) {
      key = serverInfo.address;
    }

    if (key == null)
      return null;

    return new File(client.runDirectory, "data/veinbuddy/" + key + ".gson");
  }

  private int onDigRange(CommandContext<FabricClientCommandSource> ctx) {
    int x = IntegerArgumentType.getInteger(ctx, "x");
    int y = IntegerArgumentType.getInteger(ctx, "y");
    int z = IntegerArgumentType.getInteger(ctx, "z");
    save.digRange = new Vector3i(x, y, z);
    isDirty = true;
    return 0;
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
      save.selections.add(new Bounds(new Vector3i(targetedBlock, 2), save.digRange));
      redrawStatic();
    }
    if (isHolding && isCharged) {
      Wall w = new Wall((int)targetedBlock.x, (int)targetedBlock.y, (int)targetedBlock.z, save.highlightWallColor, save.highlightGridColor);
      dynamicRenderer.draw(List.of(w));
    }
  }

  private void save(MinecraftClient client) {
    if (!isDirty) return;
    isDirty = false;

    File saveFile = getSaveFile(client);
    SaveLoader.Save(saveFile, save);
  }

  private void removeTargetedBlock(Vec3d cameraPos, Vec3d cameraDir) {
    Bounds closest = null;
    float closestDist = Float.MAX_VALUE;

    Vec3d closeEnd = cameraPos.subtract(cameraDir);
    Vec3d farEnd = cameraPos.add(cameraDir.multiply(1000));

    for (Bounds bounds : save.selections) {
      if (!bounds.intersects(closeEnd, farEnd)) continue;

      float distance = new Vector3f(bounds.center()).add(0.5f,0.5f,0.5f).distance(cameraPos.toVector3f());
      if (distance >= closestDist) continue;

      closest = bounds;
      closestDist = distance;
    }

    if (closest == null) return;
    save.selections.remove(closest);
    redrawStatic();
  }

  private void redrawStatic() {
    isDirty = true;

    HashSet<Wall> rangeBorder = new HashSet<>();

    // find boundaries
    for (Bounds selection : save.selections) {
      Wall.createWalls(rangeBorder, selection, save.rangeWallColor, save.rangeGridColor);
    }

    // mark overlapping
    Vector3i temp = new Vector3i();
    for (Wall wall : rangeBorder) {
      for (Bounds selection : save.selections) {
        wall.addSelection(selection, temp);
        if (!wall.isWall()) break;
      }
    }

    // remove non-walls
    rangeBorder.removeIf(Wall::isNotWall);

    ArrayList<Wall> borders = new ArrayList<>(rangeBorder);
    for (Bounds selection : save.selections) {
      Vector3ic center = selection.center();
      borders.add(new Wall(center.x(), center.y(), center.z(), save.selectionWallColor, save.selectionGridColor));
    }

    staticRenderer.draw(borders);
  }
}
