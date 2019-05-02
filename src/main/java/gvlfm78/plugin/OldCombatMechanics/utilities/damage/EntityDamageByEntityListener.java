package gvlfm78.plugin.OldCombatMechanics.utilities.damage;

import gvlfm78.plugin.OldCombatMechanics.OCMMain;
import gvlfm78.plugin.OldCombatMechanics.module.Module;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageByEntityListener extends Module {

    private static EntityDamageByEntityListener INSTANCE;
    private boolean enabled;

    public EntityDamageByEntityListener(OCMMain plugin){
        super(plugin, "entity-damage-listener");
        INSTANCE = this;
    }

    public static EntityDamageByEntityListener getINSTANCE(){
        return INSTANCE;
    }

    @Override
    public boolean isEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event){
        Entity damager = event.getDamager();
        OCMEntityDamageByEntityEvent e = new OCMEntityDamageByEntityEvent
                (damager, event.getEntity(), event.getCause(), event.getDamage());

        plugin.getServer().getPluginManager().callEvent(e);

        if (e.isCancelled()) {
            return;
        }

        //Re-calculate modified damage and set it back to original event
        // Damage order: base + potion effects + critical hit + enchantments + armour effects
        double newDamage = e.getBaseDamage();

        //Weakness potion
        newDamage += e.getWeaknessModifier();

        //Strength potion
        newDamage += e.getStrengthModifier();

        //Critical hit
        newDamage *= e.getCriticalMultiplier();

        //Enchantments
        newDamage += e.getMobEnchantmentsDamage() + e.getSharpnessDamage();

        if (newDamage < 0) {
            newDamage = 0;
        }

        event.setDamage(newDamage);
    }
}
