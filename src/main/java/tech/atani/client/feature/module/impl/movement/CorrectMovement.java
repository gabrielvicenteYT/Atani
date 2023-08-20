package tech.atani.client.feature.module.impl.movement;

import tech.atani.client.listener.event.events.minecraft.game.RunTickEvent;
import tech.atani.client.listener.radbus.Listen;
import tech.atani.client.feature.module.Module;
import tech.atani.client.feature.module.data.ModuleData;
import tech.atani.client.feature.module.data.enums.Category;
import tech.atani.client.utility.player.PlayerHandler;
import tech.atani.client.feature.module.value.impl.CheckBoxValue;

@ModuleData(name = "CorrectMovement", description = "Corrects your movement according to your yaw", category = Category.MOVEMENT, alwaysEnabled = true)
public class CorrectMovement extends Module {

    public CheckBoxValue moveFixSilent = new CheckBoxValue("Silent", "Silently fix your movement?", this, true);

    @Listen
    public final void onTick(RunTickEvent runTickEvent) {
        PlayerHandler.moveFix = this.isEnabled();
        PlayerHandler.moveFixSilent = this.isEnabled() && moveFixSilent.getValue();
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

}