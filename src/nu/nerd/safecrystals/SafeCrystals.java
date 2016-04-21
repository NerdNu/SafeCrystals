package nu.nerd.safecrystals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

// ----------------------------------------------------------------------------
/**
 * Prevent Ender Crystals from exploding and breaking blocks or damaging
 * entities.
 *
 * Players who have permission to build in a WorldGuard region can break the
 * crystals into dropped items.
 *
 * WorldGuard build permission is checked before allowing a player to place an
 * Ender Crystal in a region.
 */
public class SafeCrystals extends JavaPlugin implements Listener {

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        _worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent Ender Crystals from exploding.
     *
     * Remove and drop as item when appropriate.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.ENDER_CRYSTAL) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent Ender Crystals from being damaged by other entities.
     *
     * If the damager is a player who can build, drop the crystal as an item.
     * Projectiles are handled the same as the player who shot them.
     */
    @EventHandler()
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.ENDER_CRYSTAL) {
            event.setCancelled(true);

            if (event.getDamager() instanceof Player) {
                tryBreakEnderCrystal(entity, (Player) event.getDamager());
            } else if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    tryBreakEnderCrystal(entity, (Player) projectile.getShooter());
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Check that a player can build before placing an Ender Crystal.
     *
     * Minecraft will place an Ender Crystal on top of obsidian or bedrock even
     * when the player clicks the sides or underside of the block. Therefore, we
     * always check build permissions <i>above</i> the clicked block.
     */
    @EventHandler()
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getMaterial() == Material.END_CRYSTAL && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block destination = event.getClickedBlock().getRelative(BlockFace.UP);
            if (!_worldGuard.canBuild(event.getPlayer(), destination.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle a specific player's attempt to can break an Ender Crystal.
     *
     * @param crystal the crystal.
     * @param player the player.
     */
    protected void tryBreakEnderCrystal(Entity crystal, Player player) {
        Location loc = crystal.getLocation();
        if (_worldGuard.canBuild(player, loc)) {
            crystal.remove();
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.END_CRYSTAL));
            getLogger().info(player.getName() + " broke an Ender Crystal at " +
                             loc.getWorld().getName() + ", " +
                             loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Reference to WorldGuard.
     */
    protected WorldGuardPlugin _worldGuard;
} // class SafeCrystals