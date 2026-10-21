package com.metzclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class MetzClient implements ClientModInitializer {
    public static final String MOD_ID = "metzclient";
    public static final MinecraftClient MC = MinecraftClient.getInstance();
    public static final Map<String, Boolean> FEATURES = new LinkedHashMap<>();
    private static KeyBinding openKey;
    private static KeyBinding zoomKey;
    private static boolean zoomHeld;
    private static int oldFov = 70;
    private static long lastAttack = 0;
    private static int cps = 0;
    private static long cpsWindow = 0;
    private static final Identifier LOGO = Identifier.of(MOD_ID, "textures/gui/logo.png");
    private static final String[] NAMES = {
        "Coordinates", "FPS Counter", "Direction", "Biome", "In-game Clock", "Armor Status", "Potion Effects", "Item Durability", "Keystrokes", "CPS Counter",
        "Custom Crosshair", "Zoom", "Fullbright", "Auto Sprint", "Auto Tool", "Weather Hider", "Entity Radar", "Storage ESP", "Base Clue Scanner", "Ore Alerts",
        "Anti-AFK", "Reach Distance HUD", "Ping Display", "Chunk Border Hint", "Safe Walk Hint", "Held Item Name"
    };
    @Override public void onInitializeClient() {
        for (String n : NAMES) FEATURES.put(n, Set.of("Coordinates", "FPS Counter", "Direction", "Storage ESP", "Armor Status").contains(n));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.metzclient.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.metzclient"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.metzclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.metzclient"));
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> renderHud(ctx));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) client.setScreen(new MetzScreen());
            if (client.player == null || client.world == null) return;
            if (enabled("Auto Sprint") && client.player.forwardSpeed > 0 && !client.player.isSneaking()) client.player.setSprinting(true);
            if (enabled("Anti-AFK") && client.player.age % 80 == 0 && client.player.getVelocity().horizontalLengthSquared() < 0.0001) client.player.jump();
            if (enabled("Auto Tool") && client.options.attackKey.isPressed() && client.crosshairTarget != null && client.crosshairTarget.getType().name().equals("BLOCK")) {
                var state = client.world.getBlockState(((net.minecraft.util.hit.BlockHitResult)client.crosshairTarget).getBlockPos());
                int best = -1; float speed = 1f;
                for (int i=0;i<9;i++) { var stack = client.player.getInventory().getStack(i); float s = stack.getMiningSpeedMultiplier(state); if (s > speed) { speed=s; best=i; } }
                if (best >= 0) client.player.getInventory().selectedSlot = best;
            }
            boolean wantZoom = enabled("Zoom") && zoomKey.isPressed();
            if (wantZoom && !zoomHeld) { oldFov = client.options.getFov().getValue(); client.options.getFov().setValue(Math.max(30, (int) (oldFov / 3))); zoomHeld = true; }
            else if (!wantZoom && zoomHeld) { client.options.getFov().setValue(oldFov); zoomHeld = false; }
            if (enabled("Fullbright")) client.options.getGamma().setValue(16.0);
            else if (Math.abs(client.options.getGamma().getValue() - 16.0) < 0.01) client.options.getGamma().setValue(1.0);
            if (enabled("Weather Hider") && client.world.isRaining()) client.world.setRainGradient(0f);
            if (client.options.attackKey.isPressed() && System.currentTimeMillis() - lastAttack > 80) { cps++; lastAttack = System.currentTimeMillis(); }
            if (System.currentTimeMillis() - cpsWindow > 1000) { cpsWindow = System.currentTimeMillis(); cps = 0; }
        });
    }
    public static boolean enabled(String name) { return FEATURES.getOrDefault(name, false); }
    private static void line(DrawContext c, String s, int x, int y, int color) { c.drawTextWithShadow(MC.textRenderer, s, x, y, color); }
    private static void renderHud(DrawContext c) {
        if (MC.player == null || MC.world == null) return;
        int sw = MC.getWindow().getScaledWidth();
        // Brand mark is always visible while in-world, top-right as requested.
        c.drawTexture(LOGO, sw - 74, 8, 0, 0, 66, 54, 1387, 1134);
        int y = 8; int blue = 0xff12bfff;
        if (enabled("Coordinates")) { line(c, String.format("XYZ %.1f / %.1f / %.1f", MC.player.getX(), MC.player.getY(), MC.player.getZ()), 8, y, blue); y += 11; }
        if (enabled("FPS Counter")) { line(c, "FPS " + MC.getCurrentFps(), 8, y, blue); y += 11; }
        if (enabled("Direction")) { line(c, "Facing " + MC.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT), 8, y, blue); y += 11; }
        if (enabled("Biome")) { line(c, "Biome " + MC.world.getBiome(MC.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("unknown"), 8, y, blue); y += 11; }
        if (enabled("In-game Clock")) { line(c, "Time " + (MC.world.getTimeOfDay() % 24000), 8, y, blue); y += 11; }
        if (enabled("Armor Status")) { StringBuilder a = new StringBuilder("Armor "); for (var it : MC.player.getArmorItems()) if (!it.isEmpty()) a.append(it.getName().getString()).append(' '); line(c, a.toString(), 8, y, blue); y += 11; }
        if (enabled("Item Durability") && !MC.player.getMainHandStack().isEmpty()) { var s=MC.player.getMainHandStack(); line(c, "Durability " + (s.getMaxDamage()-s.getDamage()) + "/" + s.getMaxDamage(), 8, y, blue); y+=11; }
        if (enabled("Potion Effects")) { for (var e : MC.player.getStatusEffects()) { line(c, e.getEffectType().value().getName().getString() + " " + (e.getDuration()/20) + "s", 8, y, blue); y+=11; } }
        if (enabled("CPS Counter")) line(c, "CPS " + cps, 8, y, blue);
        if (enabled("Ping Display") && MC.getNetworkHandler()!=null) { var entry=MC.getNetworkHandler().getPlayerListEntry(MC.player.getUuid()); if(entry!=null) line(c,"Ping "+entry.getLatency()+"ms",8,y+11,blue); }
        if (enabled("Keystrokes")) { int kx=sw/2-24, ky=MC.getWindow().getScaledHeight()-48; c.fill(kx+16,ky,kx+32,ky+14,0xaa111a25); c.fill(kx,ky+16,kx+16,ky+30,0xaa111a25); c.fill(kx+16,ky+16,kx+32,ky+30,0xaa111a25); c.fill(kx+32,ky+16,kx+48,ky+30,0xaa111a25); line(c,"W",kx+21,ky+3,0xffeeeeee); line(c,"A",kx+5,ky+19,0xffeeeeee); line(c,"S",kx+21,ky+19,0xffeeeeee); line(c,"D",kx+37,ky+19,0xffeeeeee); }
        if (enabled("Custom Crosshair")) { int cx=sw/2,cy=MC.getWindow().getScaledHeight()/2; c.fill(cx-5,cy,cx+6,cy+1,blue); c.fill(cx,cy-5,cx+1,cy+6,blue); }
        if (enabled("Storage ESP")) renderStorageList(c, sw);
        if (enabled("Base Clue Scanner")) renderBaseClues(c, sw);
        if (enabled("Ore Alerts")) renderOreAlert(c);
        if (enabled("Entity Radar")) renderEntityRadar(c, sw);
        if (enabled("Chunk Border Hint")) line(c,"Chunk: "+(MC.player.getBlockX()>>4)+", "+(MC.player.getBlockZ()>>4),sw-150,70,blue);
        if (enabled("Reach Distance HUD") && MC.crosshairTarget!=null) line(c,String.format("Target %.1fm",Math.sqrt(MC.crosshairTarget.getPos().squaredDistanceTo(MC.player.getEyePos()))),sw-150,82,blue);
        if (enabled("Safe Walk Hint") && MC.player.isSneaking()) line(c,"Safe-walk: sneak edge protection active",8,MC.getWindow().getScaledHeight()-20,blue);
        if (enabled("Held Item Name") && !MC.player.getMainHandStack().isEmpty()) line(c, MC.player.getMainHandStack().getName().getString(), sw/2-40, MC.getWindow().getScaledHeight()/2+18, blue);
    }
    private static void renderStorageList(DrawContext c, int sw) {
        var p=MC.player.getBlockPos(); int found=0; String[] names={"chest","barrel","shulker_box","ender_chest","hopper","furnace","dispenser","dropper"};
        for(int x=-8;x<=8 && found<8;x++) for(int z=-8;z<=8 && found<8;z++) for(int yy=-3;yy<=3 && found<8;yy++) {
            var pos=p.add(x,yy,z); var id=Registries.BLOCK.getId(MC.world.getBlockState(pos).getBlock()).getPath();
            for(String n:names) if(id.contains(n)) { line(c,"[STORAGE] "+id+"  "+pos.getX()+" "+pos.getY()+" "+pos.getZ(),sw-240,64+found*11,0xff22d7ff); found++; break; }
        }
    }
    private static void renderBaseClues(DrawContext c,int sw) {
        int found=0; var p=MC.player.getBlockPos();
        for(int x=-12;x<=12 && found<4;x++) for(int z=-12;z<=12 && found<4;z++) { var pos=p.add(x,0,z); String id=Registries.BLOCK.getId(MC.world.getBlockState(pos).getBlock()).getPath(); if(id.contains("chest")||id.contains("barrel")||id.contains("furnace")||id.contains("crafting_table")||id.contains("bed")) { line(c,"[BASE CLUE] "+id+" nearby",sw-220,164+found*11,0xffffc857); found++; } }
    }
    private static void renderOreAlert(DrawContext c) { var p=MC.player.getBlockPos(); for(int x=-4;x<=4;x++) for(int y=-3;y<=3;y++) for(int z=-4;z<=4;z++) { String id=Registries.BLOCK.getId(MC.world.getBlockState(p.add(x,y,z)).getBlock()).getPath(); if(id.contains("diamond_ore")||id.contains("ancient_debris")) { line(c,"ORE NEARBY: "+id,8,MC.getWindow().getScaledHeight()-34,0xffffcc44); return; } } }
    private static void renderEntityRadar(DrawContext c,int sw) { int y=105; for(var e:MC.world.getEntities()) if(e!=MC.player && e instanceof PlayerEntity) { double d=Math.sqrt(e.squaredDistanceTo(MC.player)); line(c,String.format("Player %.0fm  %s",d,e.getName().getString()),sw-170,y,0xff71e5ff); y+=11; if(y>200) break; } }

    public static class MetzScreen extends Screen {
        private int scroll=0;
        protected MetzScreen() { super(Text.literal("MetzClient")); }
        @Override protected void init() { rebuild(); }
        private void rebuild() {
            clearChildren(); int left=12, top=28, colW=132, gap=7, rowH=18;
            String[][] groups={
                {"HUD", "Coordinates","FPS Counter","Direction","Biome","In-game Clock","Armor Status","Potion Effects","Item Durability","Keystrokes","CPS Counter"},
                {"VISUALS", "Custom Crosshair","Fullbright","Weather Hider","Entity Radar","Storage ESP","Base Clue Scanner","Ore Alerts","Chunk Border Hint","Held Item Name"},
                {"PLAYER", "Auto Sprint","Auto Tool","Anti-AFK","Safe Walk Hint","Zoom","Reach Distance HUD"},
                {"INFO", "Ping Display", "Held Item Name"}
            };
            for(int g=0;g<groups.length;g++) { int x=left+g*(colW+gap); int yy=top+16; for(int i=1;i<groups[g].length;i++) { final String name=groups[g][i]; int bx=x, by=yy+(i-1)*rowH-scroll; if(by<top+12 || by>height-28) continue; addDrawableChild(ButtonWidget.builder(Text.literal((enabled(name)?"§b● ":"§7○ ")+name), b->{FEATURES.put(name,!enabled(name)); b.setMessage(Text.literal((enabled(name)?"§b● ":"§7○ ")+name));}).dimensions(bx,by,colW,rowH-2).build()); } }
            addDrawableChild(ButtonWidget.builder(Text.literal("Close  [Right Shift]"), b->close()).dimensions(width-150,height-27,140,20).build());
        }
        @Override public void render(DrawContext c,int mouseX,int mouseY,float delta) {
            renderBackground(c,mouseX,mouseY,delta); c.fill(6,6,width-6,height-6,0xee090d14); c.fill(6,6,width-6,24,0xff101b2a); c.fill(6,23,width-6,25,0xff00bfff);
            c.drawTextWithShadow(textRenderer,"METZCLIENT",14,12,0xff10c5ff); c.drawTextWithShadow(textRenderer,"UTILITY  /  1.21.1",100,12,0xff9bb6c8);
            String[] labels={"HUD","VISUALS","PLAYER","INFO"}; for(int g=0;g<labels.length;g++){int x=12+g*139;c.fill(x,28,x+132,42,0xff111a26);c.drawTextWithShadow(textRenderer,labels[g],x+5,31,0xff12bfff);}
            super.render(c,mouseX,mouseY,delta);
        }
        @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical) { scroll=Math.max(0,Math.min(180,scroll-(int)(vertical*35))); rebuild(); return true; }
        @Override public boolean shouldPause(){return false;}
    }
}
