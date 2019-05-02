package gvlfm78.plugin.OldCombatMechanics.module;

import gvlfm78.plugin.OldCombatMechanics.OCMMain;
import gvlfm78.plugin.OldCombatMechanics.utilities.damage.DamageUtils;
import gvlfm78.plugin.OldCombatMechanics.utilities.damage.OCMEntityDamageByEntityEvent;
import gvlfm78.plugin.OldCombatMechanics.utilities.damage.WeaponDamages;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;

import java.util.Locale;

/**
 * Restores old tool damage.
 */
public class ModuleOldToolDamage extends Module {

    private final String[] WEAPONS = {"sword", "axe", "pickaxe", "spade", "hoe"};

    public ModuleOldToolDamage(OCMMain plugin){
        super(plugin, "old-tool-damage");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamaged(OCMEntityDamageByEntityEvent event){
        Material weaponMaterial = event.getWeapon().getType();

        if (!isTool(weaponMaterial)) {
            return;
        }

        double weaponDamage = WeaponDamages.getDamage(weaponMaterial);
        if (weaponDamage <= 0) {
            weaponDamage = 1;
        }

        event.setBaseDamage(weaponDamage);

        // Set sharpness to 1.8 damage value
        //event.setSharpnessDamage(DamageUtils.getOldSharpnessDamage(event.getSharpnessLevel()));
    }

    private boolean isTool(Material material){
        for(String type : WEAPONS) {
            if (isOfType(material, type)) {
                return true;
            }
        }

        return false;
    }

    private boolean isOfType(Material mat, String type){
        return mat.toString().endsWith("_" + type.toUpperCase(Locale.ROOT));
    }
}
