package gvlfm78.plugin.OldCombatMechanics.module;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import gvlfm78.plugin.OldCombatMechanics.OCMMain;
import gvlfm78.plugin.OldCombatMechanics.utilities.reflection.MemoizingFeatureBranch;
import gvlfm78.plugin.OldCombatMechanics.utilities.reflection.Reflector;
import net.lastcraft.oldpvp.Hit;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Brings back the old fishing knockback.
 */
public class ModuleFishingKnockback extends Module {

    private MemoizingFeatureBranch<PlayerFishEvent, Entity> hookEntityFeature;

    public ModuleFishingKnockback(OCMMain plugin){
        super(plugin, "old-fishing-knockback");

        //noinspection Convert2MethodRef as the Method reference would error at initialization, not just when invoked
        hookEntityFeature = MemoizingFeatureBranch.onException(
                playerFishEvent -> playerFishEvent.getHook(),
                playerFishEvent -> playerFishEvent.getHook(),
                // fall back to reflection on 1.12 and suck up some performance penalty
                Reflector.memoizeMethodAndInvoke(PlayerFishEvent.class, "getHook")
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRodLand(ProjectileHitEvent e) {

        Entity hookEntity = e.getEntity();
        World world = hookEntity.getWorld();

        if (e.getEntityType() != EntityType.FISHING_HOOK) {
            return;
        }

        Entity hitEntity;

        try{
            hitEntity = e.getHitEntity();
        } catch(NoSuchMethodError e1){ //For older version that don't have such method
            hitEntity = world.getNearbyEntities(hookEntity.getLocation(), 0.25, 0.25, 0.25).stream()
                    .filter(entity -> entity instanceof Player)
                    .findFirst()
                    .orElse(null);
        }

        if(hitEntity == null) return;
        if(!(hitEntity instanceof Player)) return;

        // Do not move Citizens NPCs
        // See https://wiki.citizensnpcs.co/API#Checking_if_an_entity_is_a_Citizens_NPC
        if(hitEntity.hasMetadata("NPC")) return;

        FishHook hook = (FishHook) hookEntity;
        Player rodder = (Player) hook.getShooter();
        Player player = (Player) hitEntity;

        debug("You were hit by a fishing rod!", player);

        if(player.equals(rodder) || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        //Check if cooldown time has elapsed
        if(player.getNoDamageTicks() > player.getMaximumNoDamageTicks() / 2f){
            return;
        }

        double damage = module().getDouble("damage");
        if(damage < 0) damage = 0.2;

        disableFish.add(player.getName());

        EntityDamageEvent event = makeEvent(rodder, player, damage);
        Bukkit.getPluginManager().callEvent(event);

        disableFish.remove(player.getName());

        if (module().getBoolean("checkCancelled") && event.isCancelled()){

            if(plugin.getConfig().getBoolean("debug.enabled")){
                debug("You can't do that here!", rodder);
                HandlerList hl = event.getHandlers();

                // This is to check what plugins are listening to the event
                for(RegisteredListener rl : hl.getRegisteredListeners())
                    debug("Plugin Listening: " + rl.getPlugin().getName(), rodder);
            }

            return;
        }

        player.damage(damage);

        player.setVelocity(calculateKnockbackVelocity(player.getVelocity(), player.getLocation(), hook.getLocation()));
    }

    private double horizMultiplier = 0.23;
    private double vertMultiplier = 0.35;
    private double sprintMultiplierHoriz = 2.15;
    private double sprintMultiplierVert = 1.4;

    private final Map<String, Hit> damaged = new HashMap<>();
    private final Set<String> sprintKb = new HashSet<>();
    private final Set<String> disableFish = new HashSet<>();


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) {
            return;
        }

        Player attacker = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();

        if (disableFish.contains(victim.getName())) {
            return;
        }

        Vector direction = new Vector(victim.getLocation().getX() - attacker.getLocation().getX(), 0.0, victim.getLocation().getZ() - attacker.getLocation().getZ());
        int kbEnchantLevel = attacker.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK);
        Hit hit = new Hit(direction, this.sprintKb.contains(attacker.getName()) && attacker.isSprinting(), kbEnchantLevel);

        this.damaged.put(victim.getName(), hit);
        this.sprintKb.remove(attacker.getName());
    }

    @EventHandler
    public void velocityHandler(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        Hit hit = this.damaged.remove(player.getName());
        if (hit == null) {
            return;
        }

        event.setCancelled(true);

        Vector oldVector = hit.getDirection();
        oldVector.normalize();

        Vector vector = new Vector(oldVector.getX() * this.horizMultiplier, this.vertMultiplier, oldVector.getZ() * this.horizMultiplier);
        if (hit.isSprint()) {
            vector = new Vector(vector.getX() * this.sprintMultiplierHoriz, vector.getY() * this.sprintMultiplierVert, vector.getZ() * this.sprintMultiplierHoriz);
        }
        int enchantLevel = hit.getEnchantLevel();
        if (enchantLevel > 0) {
            double knockBack = enchantLevel == 1 ? 0.3 : 0.51;
            double distance = Math.sqrt(Math.pow(vector.getX(), 2.0) + Math.pow(vector.getZ(), 2.0));
            double ratioX = vector.getX() / distance;
            double ratioZ = vector.getZ() / distance;

            vector = new Vector(ratioX * knockBack + vector.getX(), vector.getY() + enchantLevel * 0.12, ratioZ * knockBack + vector.getZ());
        }

        player.setVelocity(new Vector(player.getVelocity().getX(), vector.getY(), player.getVelocity().getZ()));

        Vector vector2 = vector;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                player.setVelocity(new Vector(vector2.getX(), player.getVelocity().getY(), vector2.getZ())), 1L);
    }



    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (!player.isSprinting()) {
            this.sprintKb.add(player.getName());
        } else {
            this.sprintKb.remove(player.getName());
        }
    }

    @EventHandler
    public void quitHandler(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.damaged.remove(player.getName());
        this.sprintKb.remove(player.getName());
    }

    private Vector calculateKnockbackVelocity(Vector currentVelocity, Location player, Location hook){
        double xDistance = hook.getX() - player.getX();
        double zDistance = hook.getZ() - player.getZ();

        // ensure distance is not zero and randomise in that case (I guess?)
        while(xDistance * xDistance + zDistance * zDistance < 0.0001){
            xDistance = (Math.random() - Math.random()) * 0.01D;
            zDistance = (Math.random() - Math.random()) * 0.01D;
        }

        double distance = Math.sqrt(xDistance * xDistance + zDistance * zDistance);

        double y = currentVelocity.getY() / 2;
        double x = currentVelocity.getX() / 2;
        double z = currentVelocity.getZ() / 2;

        // Normalize distance to have similar knockback, no matter the distance
        x -= xDistance / distance * 0.4;

        // slow the fall or throw upwards
        y += 0.4;

        // Normalize distance to have similar knockback, no matter the distance
        z -= zDistance / distance * 0.4;

        // do not shoot too high up
        if(y >= 0.4){
            y = 0.4;
        }

        return new Vector(x, y, z);
    }

    /**
     * This is to cancel dragging the player closer when you reel in
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReelIn(PlayerFishEvent e){
        if(!isSettingEnabled("cancelDraggingIn") || e.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
        hookEntityFeature.apply(e).remove(); // Nuke the bobber and don't do anything else
        e.setCancelled(true);
    }

    @SuppressWarnings({"rawtypes", "deprecation"})
    private EntityDamageEvent makeEvent(Player rodder, Player player, double damage){
        if(module().getBoolean("useEntityDamageEvent")) {
            return new EntityDamageEvent(player,
                    EntityDamageEvent.DamageCause.PROJECTILE,
                    new EnumMap<>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, damage)),
                    new EnumMap<>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(damage))));
        }else{
                return new EntityDamageByEntityEvent(rodder, player,
                        EntityDamageEvent.DamageCause.PROJECTILE,
                        new EnumMap<>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, damage)),
                        new EnumMap<>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(damage))));
            }
    }
}