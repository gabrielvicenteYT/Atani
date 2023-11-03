package tech.atani.client.feature.module.impl.movement;

import cn.muyang.nativeobfuscator.Native;
import net.minecraft.network.play.client.C03PacketPlayer;
import tech.atani.client.feature.module.Module;
import tech.atani.client.feature.module.data.ModuleData;
import tech.atani.client.feature.module.data.enums.Category;
import tech.atani.client.feature.value.impl.StringBoxValue;
import tech.atani.client.listener.event.minecraft.input.MoveButtonEvent;
import tech.atani.client.listener.event.minecraft.network.PacketEvent;
import tech.atani.client.listener.event.minecraft.player.movement.UpdateEvent;
import tech.atani.client.listener.radbus.Listen;
import tech.atani.client.utility.interfaces.Methods;
import tech.atani.client.utility.player.movement.MoveUtil;

@Native
@ModuleData(name = "Freeze", description = "Freezes You", category = Category.MOVEMENT)
public class Freeze extends Module {
    private final StringBoxValue mode = new StringBoxValue("Mode", "Which mode will the module use?", this, new String[]{"Normal", "Intave"});

    private boolean blink;

    @Override
    public String getSuffix() {
    	return mode.getValue();
    }
    
    @Listen
    public final void onUpdate(UpdateEvent updateEvent) {
        switch (mode.getValue()) {
            case "Normal":
                MoveUtil.setMoveSpeed(mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0);
                break;
            case "Intave":
                if (mc.thePlayer.ticksExisted % 2 == 0)
                    blink = !blink;

                if (blink) {
                    mc.thePlayer.motionY = 1.4;
                    mc.gameSettings.keyBindForward.pressed = false;
                } else {
                    mc.thePlayer.motionY = 0;
                    mc.gameSettings.keyBindForward.pressed = true;
                }
                break;
        }
    }
    public void onPacket(PacketEvent packetEvent) {
        if (Methods.mc.thePlayer == null || Methods.mc.theWorld == null)
            return;

        switch (mode.getValue()) {
            case "Test":
                if (packetEvent.getPacket() instanceof C03PacketPlayer && blink) {
                    packetEvent.setCancelled(true);
                } else if (!blink && packetEvent.getPacket() instanceof C03PacketPlayer) {
                    final double rotation = Math.toRadians(mc.thePlayer.rotationYaw);
                    final double x = Math.sin(rotation);
                    final double z = Math.cos(rotation);
                    ((C03PacketPlayer) packetEvent.getPacket()).setX(mc.thePlayer.posX - x * 0.5);
                    ((C03PacketPlayer) packetEvent.getPacket()).setZ(mc.thePlayer.posZ + z * 0.5);
                    mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX - x * 0.5, mc.thePlayer.posY, mc.thePlayer.posZ + z * 0.5);
                }
                break;
        }
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}