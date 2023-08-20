package tech.atani.client.feature.module.impl.hud;

import com.google.common.base.Supplier;
import tech.atani.client.feature.module.Module;
import tech.atani.client.feature.module.data.ModuleData;
import tech.atani.client.feature.module.data.enums.Category;
import tech.atani.client.feature.module.value.impl.CheckBoxValue;
import tech.atani.client.feature.module.value.impl.StringBoxValue;

// Hooked in GuiChat class & GuiNewChat class
@ModuleData(name = "CustomChat", description = "Improves the minecraft default chat.", category = Category.HUD)
public class CustomChat extends Module {

    public final StringBoxValue background = new StringBoxValue("Background", "Which background should the chat box use?", this, new String[] {"Normal", "Adaptive", "Off"});
    public final CheckBoxValue unlimitedChat = new CheckBoxValue("Unlimited Chat", "Should the module remove chat box character limit??", this, false);
    public final CheckBoxValue customFont = new CheckBoxValue("Custom Font", "Should the chat have a custom font?", this, false);
    public final CheckBoxValue backgroundBlur = new CheckBoxValue("Blur Background", "Should the chat box have a blur?", this, false, new Supplier[]{() -> (background.is("Normal") || background.is("Adaptive"))});

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

}