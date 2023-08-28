package tech.atani.client.feature.module.impl.option;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import tech.atani.client.feature.combat.CombatManager;
import tech.atani.client.feature.combat.interfaces.IgnoreList;
import tech.atani.client.feature.module.Module;
import tech.atani.client.feature.module.data.ModuleData;
import tech.atani.client.feature.module.data.enums.Category;
import tech.atani.client.feature.module.storage.ModuleStorage;
import tech.atani.client.feature.value.impl.CheckBoxValue;
import tech.atani.client.feature.value.impl.StringBoxValue;
import tech.atani.client.listener.event.minecraft.player.movement.UpdateEvent;
import tech.atani.client.listener.radbus.Listen;
import tech.atani.client.utility.interfaces.Methods;
import tech.atani.client.utility.player.PlayerUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleData(name = "Teams", description = "Friendly fire will not be tolerated", category = Category.OPTIONS)
public class Teams extends Module implements IgnoreList {


    public CheckBoxValue vanilla = new CheckBoxValue("Vanilla", "Check vanilla teams?", this, false);
    public CheckBoxValue armorColor = new CheckBoxValue("Armor Color", "Check armor color?", this, false);
    public CheckBoxValue tabColor = new CheckBoxValue("Tab Color", "Check tab color?", this, true);

    private final ArrayList<Entity> bots = new ArrayList<>();

    public Teams() {
        CombatManager.getInstance().addIgnoreList(this);
    }

    @Listen
    public void onUpdate(UpdateEvent updateEvent) {
        Methods.mc.theWorld.playerEntities.forEach(player -> {
            if(shouldSkipPlayer(player)) {
                this.bots.add(player);
            } else {
                if(this.bots.contains(player))
                    this.bots.remove(player);
            }
        });
    }

    private boolean shouldSkipPlayer(EntityPlayer targetPlayer) {
        if (!(targetPlayer instanceof EntityPlayer))
            return false;

        boolean skip = false;

        if (armorColor.getValue()) {
            int targetArmorColor = PlayerUtil.getLeatherArmorColor(targetPlayer);
            int localPlayerArmorColor = PlayerUtil.getLeatherArmorColor(mc.thePlayer);
            if(targetArmorColor != -1 && localPlayerArmorColor != -1 && targetArmorColor == localPlayerArmorColor)
                skip = true;
        }
        if(tabColor.getValue()) {
            if(getTeamColor(targetPlayer).getRGB() == getTeamColor(mc.thePlayer).getRGB())
                skip = true;
        }
        if(vanilla.getValue()) {
            if(mc.thePlayer.getTeam() != null && targetPlayer.getTeam() != null && mc.thePlayer.getTeam().isSameTeam(targetPlayer.getTeam())) {
                skip = true;
            }
        }

        return skip;
    }

    public Color getTeamColor(EntityPlayer player) {
        ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam) player.getTeam();
        int i = 16777215;

        if (scoreplayerteam != null && scoreplayerteam.getColorPrefix() != null) {
            String s = FontRenderer.getFormatFromString(scoreplayerteam.getColorPrefix());
            if (s.length() >= 2) {
                if (mc.getRenderManager().getFontRenderer() != null && mc.getRenderManager().getFontRenderer().getColorCode(s.charAt(1)) != 0)
                    i = mc.getRenderManager().getFontRenderer().getColorCode(s.charAt(1));
            }
        }
        final float r = (float) (i >> 16 & 255) / 255.0F;
        final float g = (float) (i >> 8 & 255) / 255.0F;
        final float b = (float) (i & 255) / 255.0F;

        return new Color(r, g, b);
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    @Override
    public List<Entity> getIgnored() {
        return bots;
    }
}