package gvlfm78.plugin.OldCombatMechanics.utilities.damage;

import gvlfm78.plugin.OldCombatMechanics.utilities.potions.PotionEffects;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class OCMEntityDamageByEntityEvent extends Event implements Cancellable {

    private boolean cancelled;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers(){
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }

    private Entity damager, damagee;
    private DamageCause cause;
    private double rawDamage;

    private ItemStack weapon;
    private int sharpnessLevel;

    private double baseDamage = 0, mobEnchantmentsDamage = 0, sharpnessDamage = 0, criticalMultiplier = 1;
    private double strengthModifier = 0, weaknessModifier = 0;

    // In 1.9 strength modifier is an addend, in 1.8 it is a multiplier and addend (+130%)

    public OCMEntityDamageByEntityEvent(Entity damager, Entity damagee, DamageCause cause, double rawDamage){

        this.damager = damager;
        this.damagee = damagee;
        this.cause = cause;
        this.rawDamage = rawDamage;

        if (!(damager instanceof LivingEntity)){
            setCancelled(true);
            return;
        }

        LivingEntity le = (LivingEntity) damager;

        EntityEquipment equipment = le.getEquipment();
        weapon = equipment.getItemInMainHand();
        // Yay paper. Why do you need to return null here?
        if (weapon == null) {
            weapon = new ItemStack(Material.AIR);
        }

        mobEnchantmentsDamage = MobDamage.applyEntityBasedDamage(damagee.getType(), weapon);
        sharpnessLevel = weapon.getEnchantmentLevel(Enchantment.DAMAGE_ALL);
        sharpnessDamage = DamageUtils.getNewSharpnessDamage(sharpnessLevel);

        //Check if it's a critical hit
        if (le instanceof Player){
            Player player = (Player) le;
            if (DamageUtils.isCriticalHit(player)){
                criticalMultiplier = 1.5;
            }
        }

        //amplifier 0 = Strength I    amplifier 1 = Strength II
        strengthModifier = (PotionEffects.get(le, PotionEffectType.INCREASE_DAMAGE)
                .map(PotionEffect::getAmplifier)
                .orElse(-1) + 1) * 3;

        weaknessModifier = (PotionEffects.get(le, PotionEffectType.WEAKNESS)
                .map(PotionEffect::getAmplifier)
                .orElse(-1) + 1) * -4;

        baseDamage = 1;
    }

    public Entity getDamager(){
        return damager;
    }

    public Entity getDamagee(){
        return damagee;
    }

    public DamageCause getCause(){
        return cause;
    }

    public double getRawDamage(){
        return rawDamage;
    }

    public ItemStack getWeapon(){
        return weapon;
    }

    public int getSharpnessLevel(){
        return sharpnessLevel;
    }

    public double getStrengthModifier(){
        return strengthModifier;
    }

    public double getWeaknessModifier(){
        return weaknessModifier;
    }

    public void setWeaknessModifier(double weaknessModifier){
        this.weaknessModifier = weaknessModifier;
    }

    public double getBaseDamage(){
        return baseDamage;
    }

    public void setBaseDamage(double baseDamage){
        this.baseDamage = baseDamage;
    }

    public double getMobEnchantmentsDamage(){
        return mobEnchantmentsDamage;
    }

    public void setMobEnchantmentsDamage(double mobEnchantmentsDamage){
        this.mobEnchantmentsDamage = mobEnchantmentsDamage;
    }

    public double getSharpnessDamage(){
        return sharpnessDamage;
    }

    public void setSharpnessDamage(double sharpnessDamage){
        this.sharpnessDamage = sharpnessDamage;
    }

    public double getCriticalMultiplier(){
        return criticalMultiplier;
    }

    public void setCriticalMultiplier(double criticalMultiplier){
        this.criticalMultiplier = criticalMultiplier;
    }

    @Override
    public boolean isCancelled(){
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled){
        this.cancelled = cancelled;
    }
}
