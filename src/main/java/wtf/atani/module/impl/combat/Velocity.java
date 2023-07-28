package wtf.atani.module.impl.combat;

import com.google.common.base.Supplier;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import wtf.atani.event.events.PacketEvent;
import wtf.atani.event.events.SilentMoveEvent;
import wtf.atani.event.events.UpdateEvent;
import wtf.atani.event.radbus.Listen;
import wtf.atani.module.Module;
import wtf.atani.module.data.ModuleInfo;
import wtf.atani.module.data.enums.Category;
import wtf.atani.module.storage.ModuleStorage;
import wtf.atani.utils.math.time.TimeHelper;
import wtf.atani.value.impl.SliderValue;
import wtf.atani.value.impl.StringBoxValue;

@ModuleInfo(name = "Velocity", description = "Modifies your velocity", category = Category.COMBAT)
public class Velocity extends Module {

    public StringBoxValue mode = new StringBoxValue("Mode", "Which mode will the module use?", this, new String[] {"Simple", "Intave", "Old Grim", "Grim Flag", "Vulcan", "AAC v4", "AAC v5 Packet", "AAC v5.2.0"});
    public SliderValue<Integer> horizontal = new SliderValue<>("Horizontal %", "How much horizontal velocity will you take?", this, 100, 0, 100, 0, new Supplier[] {() -> mode.getValue().equalsIgnoreCase("Simple")});
    public SliderValue<Integer> vertical = new SliderValue<>("Vertical %", "How much vertical velocity will you take?", this, 100, 0, 100, 0, new Supplier[] {() -> mode.getValue().equalsIgnoreCase("Simple")});
    public SliderValue<Float> aacv4Reduce = new SliderValue<>("Reduce", "How much motion will be reduced?", this, 0.62F,0F,1F, 1, new Supplier[] {() -> mode.getValue().equalsIgnoreCase("AAC v4")});

    private KillAura killAura;

    private double packetX = 0;
    private double packetY = 0;
    private double packetZ = 0;
    private boolean receivedVelocity = false;

    // AAC v5.2.0
    private TimeHelper aacTimer = new TimeHelper();

    // Intave
    private int counter;

    // Grim
    int grimCancel = 0;
    int updates = 0;

    // Grim Flag
    private boolean grimFlag;

    @Listen
    public final void onUpdate(UpdateEvent updateEvent) {
        switch (mode.getValue()) {
            case "Grim Flag":
                if (mc.thePlayer.hurtTime != 0)
                    mc.thePlayer.setPosition(mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY, mc.thePlayer.lastTickPosZ);
                break;
            case "AAC v5.2.0":
                if (mc.thePlayer.hurtTime> 0 && this.receivedVelocity) {
                    this.receivedVelocity = false;
                    mc.thePlayer.motionX = 0.0;
                    mc.thePlayer.motionZ = 0.0;
                    mc.thePlayer.motionY = 0.0;
                    mc.thePlayer.jumpMovementFactor = -0.002f;
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, 1.7976931348623157E+308, mc.thePlayer.posZ, true));
                }
                if (aacTimer.hasReached(80L) && this.receivedVelocity) {
                    this.receivedVelocity = false;
                    mc.thePlayer.motionX = packetX / 8000.0;
                    mc.thePlayer.motionZ = packetZ / 8000.0;
                    mc.thePlayer.motionY = packetY / 8000.0;
                    mc.thePlayer.jumpMovementFactor = -0.002f;
                }
                break;
            case "AAC v4":
                if (mc.thePlayer.hurtTime > 0 && !mc.thePlayer.onGround){
                    mc.thePlayer.motionX *= aacv4Reduce.getValue().floatValue();
                    mc.thePlayer.motionZ *= aacv4Reduce.getValue().floatValue();
                }
                break;
            case "Old Grim":
                updates++;

                if (updates >= 0 || updates >= 10) {
                    updates = 0;
                    if (grimCancel > 0){
                        grimCancel--;
                    }
                }
                break;
        }
    }

    @Listen
    public final void onPacket(PacketEvent packetEvent) {
        if(packetEvent.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) packetEvent.getPacket();
            if(packet.getEntityID() == mc.thePlayer.getEntityId()) {
                this.packetX = packet.getMotionX();
                this.packetY = packet.getMotionY();
                this.packetZ = packet.getMotionZ();
                receivedVelocity = true;
                aacTimer.reset();
            }
        }
        switch (mode.getValue()) {
            case "Grim Flag":
                if(packetEvent.getType() == PacketEvent.Type.INCOMING) {
                    Packet p = packetEvent.getPacket();
                    if (p instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity)p).getEntityID() == mc.thePlayer.getEntityId()) {
                        packetEvent.setCancelled(true);
                        mc.thePlayer.motionX += 0.1D;
                        mc.thePlayer.motionY += 0.1D;
                        mc.thePlayer.motionZ += 0.1D;
                    }
                } else if(packetEvent.getType() == PacketEvent.Type.OUTGOING) {
                    if (mc.thePlayer.hurtTime != 0)
                        this.grimFlag = true;
                    if (mc.thePlayer.onGround)
                        this.grimFlag = false;
                    if (this.grimFlag && packetEvent.getPacket() instanceof C03PacketPlayer) {
                        ((C03PacketPlayer)packetEvent.getPacket()).setX(mc.thePlayer.posX + 210.0D);
                        ((C03PacketPlayer)packetEvent.getPacket()).setZ(mc.thePlayer.posZ + 210.0D);
                    }
                }
                break;
            case "Vulcan":
                if(mc.thePlayer != null && mc.theWorld != null) {
                    if (mc.thePlayer.hurtTime > 0 && packetEvent.getPacket() instanceof C0FPacketConfirmTransaction) {
                        packetEvent.setCancelled(true);
                    }
                }
                if(packetEvent.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity) packetEvent.getPacket();
                    if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                        packetEvent.setCancelled(true);
                    }
                }
                break;
            case "Simple":
                if(packetEvent.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity) packetEvent.getPacket();
                    if(packet.getEntityID() == mc.thePlayer.getEntityId()) {
                        if(horizontal.getValue() == 0 && vertical.getValue() == 0)
                            packetEvent.setCancelled(true);
                        packet.setMotionX((int) (packet.getMotionX() * (horizontal.getValue().doubleValue() / 100D)));
                        packet.setMotionY((int) (packet.getMotionY() * (vertical.getValue().doubleValue() / 100D)));
                        packet.setMotionZ((int) (packet.getMotionZ() * (horizontal.getValue().doubleValue() / 100D)));
                    }
                }
                break;
            case "Old Grim":
                if(packetEvent.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity) packetEvent.getPacket();
                    if(packet.getEntityID() == mc.thePlayer.getEntityId()) {
                        packetEvent.setCancelled(true);
                        grimCancel = 6;
                    }
                }
                if(packetEvent.getPacket() instanceof S32PacketConfirmTransaction && grimCancel > 0) {
                    packetEvent.setCancelled(true);
                    grimCancel--;
                }
                break;
            case "AAC v5 Packet": {
                if(packetEvent.getPacket() instanceof S12PacketEntityVelocity) {
                    S12PacketEntityVelocity packet = (S12PacketEntityVelocity) packetEvent.getPacket();
                    if(packet.getEntityID() == mc.thePlayer.getEntityId()) {
                        sendPacketUnlogged(
                                new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, 1.7976931348623157E+308, mc.thePlayer.posZ, true
                                )
                        );
                        packetEvent.setCancelled(true);
                    }
                }
            }
        }
    }

    @Listen
    public final void onSilent(SilentMoveEvent silentMoveEvent) {
        switch(this.mode.getValue()) {
            case "Intave":
                if (Velocity.mc.thePlayer.hurtTime == 9 && Velocity.mc.thePlayer.onGround && this.counter++ % 2 == 0) {
                    Velocity.mc.thePlayer.movementInput.jump = true;
                    break;
                }
        }
    }

    @Override
    public void onEnable() {
        this.killAura = ModuleStorage.getInstance().getByClass(KillAura.class);
        grimCancel = 0;
        packetX = 0;
        packetY = 0;
        packetZ = 0;
        receivedVelocity = false;
        aacTimer.reset();
    }

    @Override
    public void onDisable() {

    }
}
