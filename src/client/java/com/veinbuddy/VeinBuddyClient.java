package com.veinbuddy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector4f;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class VeinBuddyClient implements ClientModInitializer {

  private final static MinecraftClient mc = MinecraftClient.getInstance();
  private final static int defaultDigRange = 7;
  private final static double speed = 0.2f;
  private final static double radius = 0.5;
  private final static double placeRange = 6.0;
  private final static int maxTicks = (int) (placeRange / speed);
  private final static int delay = 5;

  private Vec3i digRange = new Vec3i(defaultDigRange, defaultDigRange, defaultDigRange);

  private int selectionTicks = 0;
  private Vec3d pos = null;
  private Vec3i posBlock = null;
  private boolean change = true;
  private int saveNumber = 0;
  private int changeNumber = 0;

  private SimpleRenderer rangeRenderer;

  @Override
  public void onInitializeClient() {
    SharedConstants.isDevelopment = true;

    //ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onStart(client));
    ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    //ClientTickEvents.END_CLIENT_TICK.register(this::saveSelections);

    rangeRenderer = new SimpleRenderer();

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

  // todo remove debug stuff
  private int debug(CommandContext<FabricClientCommandSource> ctx) {
    rangeRenderer.buildMesh(List.of(new Bounds(new Vector3i(0,10,0), new Vector3i(5,5,5))));
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

//  private void saveSelections(MinecraftClient client) {
//    if (!(changeNumber > saveNumber)) return;
//    try {
//      File saveFile = getSaveFile(client);
//      if (null == saveFile)
//        return;
//      saveFile.getParentFile().mkdirs();
//      FileWriter fileWriter = new FileWriter(saveFile, false);
//      fileWriter.write("Version 2\n");
//      for (Vec3i selection : selections) {
//         Vec3i ranges = selectionRanges.get(selection);
//         fileWriter.write(selection.getX() + " " + selection.getY() + " " + selection.getZ() + " " + ranges.getX() + " " + ranges.getY() + " " + ranges.getZ() + "\n");
//      }
//      fileWriter.close();
//      saveNumber = changeNumber;
//    } catch (IOException e){
//      System.out.println("Sad!");
//    }
//  }

//  private void onStart(MinecraftClient client) {
//    File configFile = getConfigFile(client);
//    File saveFile = getSaveFile(client);
//    if (configFile.exists()) {
//      try {
//        Scanner sc = new Scanner(configFile);
//	if (null != sc.findInLine("\\d+ \\d+ \\d+")) {
//          int x = sc.nextInt();
//          int y = sc.nextInt();
//          int z = sc.nextInt();
//          digRange = new Vec3i(x, y, z);
//	}
//      } catch (IOException e) {
//        System.out.println("Mad!");
//      }
//    }
//    if (null == saveFile || !saveFile.exists())
//      return;
//    try {
//      Scanner sc = new Scanner(saveFile);
//      if (!sc.hasNext("Version")) { //Version 1
//        while (null != sc.findInLine("\\d+ \\d+ \\d+")) {
//          int x = sc.nextInt();
//          int y = sc.nextInt();
//          int z = sc.nextInt();
//          addSelection(new Vec3i(x, y, z), new Vec3i(defaultDigRange, defaultDigRange, defaultDigRange), true);
//          sc.nextLine();
//        }
//      }
//      else {
//	sc.next();
//        String found = sc.next("\\d+");
//        if (found.equals("2")) { // Version 2
//          sc.nextLine();
//	  while (sc.hasNext()) {
//            int x = sc.nextInt();
//            int y = sc.nextInt();
//            int z = sc.nextInt();
//            int xRange = sc.nextInt();
//            int yRange = sc.nextInt();
//            int zRange = sc.nextInt();
//            addSelection(new Vec3i(x, y, z), new Vec3i(xRange, yRange, zRange), true);
//            sc.nextLine();
//	  }
//        }
//      }
//    } catch (IOException e) {
//      System.out.println("Bad!");
//    }
//    updateWalls();
//    refreshBuffer();
//  }

  private int onDigRange(CommandContext<FabricClientCommandSource> ctx) {
    int x = IntegerArgumentType.getInteger(ctx, "x");
    int y = IntegerArgumentType.getInteger(ctx, "y");
    int z = IntegerArgumentType.getInteger(ctx, "z");
    digRange = new Vec3i(x, y, z);
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
    if (!item.getName().toString().contains("pickaxe")) {
      pos = null;
      posBlock = null;
      selectionTicks = 0;
      return;
    }
    boolean rightClick = client.mouse.wasRightButtonClicked();
    Vec3d playerPos = client.player.getPos().add(0.0f, 1.6f, 0.0f);
    Vec3d playerDir = client.player.getRotationVector();
    if (!rightClick && 0 != selectionTicks && 10 > selectionTicks) {
      //removeLookedAtSelection(playerPos, playerDir);
    }
    if (!rightClick && 10 <= selectionTicks) {
      //addSelection(posBlock, digRange, false);
    }
    if (!rightClick) {
      pos = null;
      posBlock = null;
    }
    selectionTicks = rightClick ? selectionTicks + 1 : 0;
    selectionTicks = Math.min(selectionTicks, maxTicks + delay);
    if (10 > selectionTicks) return;
    pos = playerPos.add(playerDir.multiply(speed).multiply(selectionTicks - delay));
    posBlock = new Vec3i((int)Math.floor(pos.getX()), 
                         (int)Math.floor(pos.getY()), 
                         (int)Math.floor(pos.getZ()));
  }
}
