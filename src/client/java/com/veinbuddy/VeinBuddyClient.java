package com.veinbuddy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;
import java.util.*;

public class VeinBuddyClient implements ClientModInitializer {

  private final static MinecraftClient mc = MinecraftClient.getInstance();
  private final static int defaultDigRange = 7;
  private final static float speed = 0.2f;
  private final static float placeRange = 6.0f;
  private final static int maxTicks = (int) (placeRange / speed);
  private final static int delay = 5;

  private Vector3i digRange = new Vector3i(defaultDigRange, defaultDigRange, defaultDigRange);

  private int selectionTicks = 0;
  private Vec3d pos = null;
  private Vec3i posBlock = null;

  private SimpleRenderer staticRenderer;
  private SimpleRenderer dynamicRenderer;

  private final DigShape digShape = new DigShape();
  private final DigShape selected = new DigShape();
  private final DigShape highLighted = new DigShape();

  private Vector4fc rangeColor = new Vector4f(1,0,0,0.2f);
  private Vector4fc rangeGridColor = new Vector4f(0,0,0,1);
  private Vector4fc selectionColor = new Vector4f(0,1,0,0.2f);
  private Vector4fc selectionGridColor = new Vector4f(0);
  private Vector4fc highlightGridColor = new Vector4f(0,0,0,1);

  @Override
  public void onInitializeClient() {
    SharedConstants.isDevelopment = true;

    //ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onStart(client));
    ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    //ClientTickEvents.END_CLIENT_TICK.register(this::saveSelections);

    dynamicRenderer = new SimpleRenderer(true, false);
    staticRenderer = new SimpleRenderer();

    digShape.setColors(rangeColor, rangeGridColor);
    selected.setColors(selectionColor, selectionGridColor);
    highLighted.setColors(new Vector4f(), highlightGridColor);

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

  private int debug(CommandContext<FabricClientCommandSource> ctx) {
    digShape.add(new DigShape.Range(new Vec3i(0,10,0), new Vec3i(5,5,5)));
    digShape.add(new DigShape.Range(new Vec3i(3,7,0), new Vec3i(3,3,3)));

    redrawStatic();
    return 1;
  }

  private File getConfigFile(MinecraftClient client) {
    return new File(client.runDirectory, "config/veinbuddy.txt");
  }

  private File getSaveFile(MinecraftClient client) {
    ServerInfo serverInfo = client.getCurrentServerEntry();
    if (null == serverInfo)
      return null;
    String address = serverInfo.address;
    return new File(client.runDirectory, "data/veinbuddy/" + address + ".txt");
  }

  private int onDigRange(CommandContext<FabricClientCommandSource> ctx) {
    int x = IntegerArgumentType.getInteger(ctx, "x");
    int y = IntegerArgumentType.getInteger(ctx, "y");
    int z = IntegerArgumentType.getInteger(ctx, "z");
    digRange = new Vector3i(x, y, z);
    try {
      File configFile = getConfigFile(mc);
      FileWriter fileWriter = new FileWriter(configFile, false);
      fileWriter.write(x + " " + y + " " + z + "\n");
    } catch (IOException e) {
      System.out.println("Egad!");
    }

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

    int chargeTime = Math.max(selectionTicks - delay, 0);
    if (!isHoldingPickaxe || released) {
      selectionTicks = 0;
      dynamicRenderer.clear();
    }
    if (isHolding) selectionTicks++;
    if (!isHoldingPickaxe || (!isHolding && !released) || (isHolding && !isCharged)) return;

    Vec3d playerPos = client.player.getEyePos();
    Vec3d playerDir = client.player.getRotationVector();


    if (released && !isCharged) {
      digShape.removeFirst(playerPos, playerDir);
      redrawStatic();
      return;
    }
    Vector3f targetedBlock = playerDir.toVector3f();
    targetedBlock = targetedBlock
            .mul(speed)
            .mul(chargeTime)
            .div(Math.max(targetedBlock.length() / placeRange, 1))
            .add(playerPos.toVector3f())
            .floor();
    if (released && isCharged) {
      digShape.add(new DigShape.Range(new Vec3i((int)targetedBlock.x, (int)targetedBlock.y, (int)targetedBlock.z), new Vec3i(digRange.x, digRange.y, digRange.z)));
      redrawStatic();
    }
    if (isHolding && isCharged) {
      //Wall w = new Wall((int)targetedBlock.x, (int)targetedBlock.y, (int)targetedBlock.z, new Vector4f(0), highlightGridColor);
      //dynamicRenderer.draw(List.of(w));
    }
  }

  private void redrawStatic() {
    staticRenderer.draw(List.of(digShape, selected));
  }
}
