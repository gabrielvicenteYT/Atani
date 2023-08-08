package wtf.atani.module.impl.movement;

import com.google.common.base.Supplier;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import wtf.atani.event.events.MoveEntityEvent;
import wtf.atani.event.events.PacketEvent;
import wtf.atani.event.events.UpdateMotionEvent;
import wtf.atani.event.radbus.Listen;
import wtf.atani.module.Module;
import wtf.atani.module.data.ModuleInfo;
import wtf.atani.module.data.enums.Category;
import wtf.atani.utils.math.time.TimeHelper;
import wtf.atani.utils.player.MoveUtil;
import wtf.atani.value.impl.SliderValue;
import wtf.atani.value.impl.StringBoxValue;

@ModuleInfo(name = "Flight", description = "Makes you fly", category = Category.MOVEMENT)
public class Flight extends Module {
    private final StringBoxValue mode = new StringBoxValue("Mode", "Which mode will the module use?", this, new String[]{"Vanilla", "Old NCP", "Collision", "Vulcan", "Grim", "Test"});
    private final StringBoxValue vulcanMode = new StringBoxValue("Vulcan Mode", "Which mode will the vulcan mode use?", this, new String[]{"Normal", "Clip & Glide", "Glide"}, new Supplier[]{() -> mode.getValue().equalsIgnoreCase("Vulcan")});
    private final StringBoxValue grimMode = new StringBoxValue("Grim Mode", "Which mode will the grim mode use?", this, new String[]{"Explosion", "Boat"}, new Supplier[]{() -> mode.getValue().equalsIgnoreCase("Grim")});
    private final SliderValue<Integer> time = new SliderValue<>("Time", "How long will the flight fly?", this, 10, 3, 15, 0, new Supplier[]{() -> mode.getValue().equalsIgnoreCase("Vulcan")});
    private final SliderValue<Float> timer = new SliderValue<>("Timer", "How high will be the timer when flying?", this, 0.2f, 0.1f, 0.5f, 1, new Supplier[]{() -> mode.getValue().equalsIgnoreCase("Vulcan")});
    private final SliderValue<Float> speed = new SliderValue<>("Speed", "How fast will the fly be?", this, 1.4f, 0f, 10f, 1, new Supplier[]{() -> mode.getValue().equalsIgnoreCase("Vulcan") || mode.getValue().equalsIgnoreCase("Vanilla")});

    // Old NCP
    private double moveSpeed;
    private boolean jumped;

    // Vulcan
    private double startY;
    private int stage;
    public int jumps;
    private final TimeHelper glideTime = new TimeHelper();

    // Grim
    boolean velo = false;

    @Override
    public String getSuffix() {
    	return mode.getValue();
    }
    
    @Listen
    public final void onUpdateMotion(UpdateMotionEvent updateMotionEvent) {
        switch (mode.getValue()) {
            case "Grim":
                if (updateMotionEvent.getType() == UpdateMotionEvent.Type.MID) {
                    switch (grimMode.getValue()) {
                        case "Explosion":
                            if (velo) {
                                if (mc.thePlayer.hurtTime != 0) {
                                    mc.thePlayer.posY -= 100;
                                    mc.thePlayer.posX += 15;
                                    mc.thePlayer.posZ += 15;
                                    mc.thePlayer.jump();
                                    mc.thePlayer.motionY *= 1.022;
                                    mc.thePlayer.onGround = true;
                                }
                                velo = false;
                            }
                            break;
                        case "Boat":
                            mc.thePlayer.motionY = 0;
                            mc.thePlayer.onGround = true;
                            break;
                    }
                }
                break;
            case "Vanilla":
                moveSpeed = speed.getValue();

                if (mc.gameSettings.keyBindJump.isKeyDown())
                    mc.thePlayer.motionY = moveSpeed / 2;

                else if (mc.gameSettings.keyBindSneak.isKeyDown())
                    mc.thePlayer.motionY = -moveSpeed / 2;
                else
                    mc.thePlayer.motionY = 0;

                MoveUtil.strafe(moveSpeed);
                break;
            case "Test":
                mc.thePlayer.motionY = 0;
                mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging());
                if(mc.gameSettings.keyBindJump.pressed) {
                    mc.timer.timerSpeed = 10;
                    mc.thePlayer.cameraPitch = 0.1F;
                    mc.thePlayer.cameraYaw = 0.1F;
                    mc.gameSettings.viewBobbing = true;
                } else {
                    mc.timer.timerSpeed = 0.2F;
                    MoveUtil.strafe(MoveUtil.getBaseMoveSpeed() * 5);
                }
                break;
            case "Old NCP":
                if (mc.thePlayer.onGround && !jumped) {
                    mc.thePlayer.jump();
                    jumped = true;
                }
                break;
            case "Vulcan":
                if (updateMotionEvent.getType() == UpdateMotionEvent.Type.PRE) {
                    switch (vulcanMode.getValue()) {
                        case "Clip & Glide":
                            if(mc.thePlayer.onGround) {
                                mc.thePlayer.jump();
                            } else if(mc.thePlayer.fallDistance > 0.25) {
                                if(jumps < 3) {
                                    this.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + (jumps == 0 ? 10 : 10), mc.thePlayer.posZ);
                                    jumps++;
                                } else {
                                    jumped = true;

                                    if(glideTime.hasReached(146, true)) {
                                        mc.thePlayer.motionY = -0.1476D;
                                    } else {
                                        mc.thePlayer.motionY = -0.0975D;
                                    }
                                }
                            }
                            break;
                        case "Normal":
                            stage++;

                            switch (stage) {
                                case 1:
                                    if (!mc.thePlayer.onGround) {
                                        sendMessage("You need to be on ground to do this!");
                                        toggle();
                                        return;
                                    }
                                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 2, mc.thePlayer.posZ);
                                    mc.timer.timerSpeed = (float) timer.getValue();
                                    break;
                                default:
                                    if (stage == 2 && mc.thePlayer.posY != startY) {
                                        mc.thePlayer.setPosition(mc.thePlayer.posX, startY, mc.thePlayer.posZ);
                                    }
                                    if (stage < time.getValue()) {
                                        mc.thePlayer.motionY = 0;
                                        MoveUtil.strafe(speed.getValue());
                                    }

                                    if (stage >= time.getValue()) {
                                        MoveUtil.strafe(0);
                                        setEnabled(false);
                                    }
                                    break;
                            }
                            break;
                        case "Glide":
                            if (!mc.thePlayer.isInWater()) {
                                if (!mc.thePlayer.onGround && mc.thePlayer.ticksExisted % 2 == 0) {
                                    mc.thePlayer.motionY = -0.1476D;
                                } else {
                                    mc.thePlayer.motionY = -0.0975D;
                                }
                            }
                            break;
                    }
                }
                break;
        }
    }

    @Listen
    public final void onPacket(PacketEvent packetEvent) {
        switch (mode.getValue()) {
            case "Grim":
                if(grimMode.compareValue("Explosion")) {
                    if (packetEvent.getPacket() instanceof S12PacketEntityVelocity) {
                        S12PacketEntityVelocity packet = (S12PacketEntityVelocity) packetEvent.getPacket();
                        if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                            velo = true;
                        }
                    }
                }
                break;
            case "Vulcan":
                if(this.vulcanMode.getValue().equalsIgnoreCase("Clip & Glide")) {
                    if(jumped && packetEvent.getPacket() instanceof C03PacketPlayer) {
                        C03PacketPlayer packet = (C03PacketPlayer) packetEvent.getPacket();

                        if(mc.thePlayer.ticksExisted % 11 == 0) {
                            packet.setOnGround(true);
                        }
                    }
                }
                break;
            case "Test":
                if(packetEvent.getPacket() instanceof C0FPacketConfirmTransaction) {
                    packetEvent.setCancelled(true);
                }

                if(packetEvent.getPacket() instanceof C03PacketPlayer) {
                    if(10 > Math.random() * 100) {
                    //    packetEvent.setCancelled(true);
                        for(int i=0; i<10;i++) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement());
                        }
                    }

                }
                break;
        }
    }


    @Listen
    public final void onMove(MoveEntityEvent moveEntityEvent) {
        switch (mode.getValue()) {
            case "Old NCP":
                if (!mc.thePlayer.onGround) {
                    moveEntityEvent.setY(mc.thePlayer.ticksExisted % 2 == 0 ? -1.0E-9 : 1.0E-9);
                    mc.thePlayer.motionY = 0;

                    if (moveSpeed >= MoveUtil.getBaseMoveSpeed()) {
                        moveSpeed -= moveSpeed / 102;
                    }

                    if (mc.thePlayer.isCollidedHorizontally || !this.isMoving()) {
                        moveSpeed = MoveUtil.getBaseMoveSpeed();
                    }

                    MoveUtil.strafe(moveSpeed);
                }
                break;
        }
    }


    @Override
    public void onEnable() {
        jumped = false;
        moveSpeed = speed.getValue();
        startY = mc.thePlayer.posY;
        stage = 0;
        jumped = false;
        jumps = 0;
    }

    @Override
    public void onDisable() {
        moveSpeed = 0;
        jumped = false;
        MoveUtil.strafe(0);
        mc.timer.timerSpeed = 1f;
        stage = 0;
        mc.thePlayer.speedInAir = 0.02F;
        jumped = false;
        jumps = 0;
    }
}
