package wtf.atani.module.impl.movement;

import com.google.common.base.Supplier;
import net.minecraft.block.BlockAir;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;
import wtf.atani.event.events.UpdateMotionEvent;
import wtf.atani.event.radbus.Listen;
import wtf.atani.module.Module;
import wtf.atani.module.data.ModuleInfo;
import wtf.atani.module.data.enums.Category;
import wtf.atani.utils.math.time.TimeHelper;
import wtf.atani.value.impl.SliderValue;

@ModuleInfo(name = "Eagle", description = "Makes you shift on block edges.", category = Category.MOVEMENT)
public class Eagle extends Module {

    private final SliderValue<Integer> delay = new SliderValue<>("Sneak Delay", "How big will the delay be to sneak?", this, 30, 0, 300, 0);

    private final TimeHelper timer = new TimeHelper();

    @Listen
    public final void onMotion(UpdateMotionEvent updateMotionEvent) {
        if (updateMotionEvent.getType() == UpdateMotionEvent.Type.PRE) {
            if (mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)).getBlock() instanceof BlockAir && mc.thePlayer.onGround) {
                if(timer.hasReached(delay.getValue(), true)) {
                    mc.gameSettings.keyBindSneak.pressed = true;
                }
            } else {
                mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
            }
        }
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() { mc.gameSettings.keyBindSneak.pressed = false; }
}