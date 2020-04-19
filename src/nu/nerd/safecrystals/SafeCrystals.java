package nu.nerd.safecrystals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
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
    /**
     * Singleton-like reference to this plugin.
     */
    public static SafeCrystals PLUGIN;

    /**
     * Configuration instance.
     */
    public static Configuration CONFIG = new Configuration();

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;
        saveDefaultConfig();
        CONFIG.reload();

        _worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent Ender Crystals from exploding, except in the case of those on
     * bedrock in the end.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.ENDER_CRYSTAL && !isDragonFightCrystal(entity.getLocation())) {
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
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.ENDER_CRYSTAL) {
            if (isDragonFightCrystal(entity.getLocation())) {
                // Vanilla handlilng.
                return;
            }

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
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getMaterial() == Material.END_CRYSTAL && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block destination = event.getClickedBlock().getRelative(BlockFace.UP);
            if (!canBuild(event.getPlayer(), destination.getLocation())) {
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
        if (canBuild(player, loc)) {
            crystal.remove();
            String suppressed;
            if (isDragonSpawningCrystal(loc)) {
                suppressed = " - drop suppressed because dragon may spawn";
            } else {
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.END_CRYSTAL));
                suppressed = "";
            }
            getLogger().info(player.getName() + " broke an Ender Crystal at " +
                             loc.getWorld().getName() + ", " +
                             loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + suppressed);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the crystal is in a position that can be used to summon
     * the dragon.
     *
     * When summoning the dragon, players can break the crystals on the frame
     * before the dragon CreatureSpawnEvent (reason DEFAULT) occurs, and
     * potentially recover them, if they don't burn up in the fire underneath.
     * This method is used to detect that situation and prevent recovery of the
     * crystals.
     *
     * Getting delicate about the exact coordinates of the crystal won't work
     * because a determined player will move the crystals with pistons
     * (verified). Any crystals too close to the portal would normally be blown
     * up by the explosion when the dragon spawns, anyway.
     *
     * @param loc the location of the crystal.
     * @return true if the crystal is in a position that can be used to summon
     *         the dragon.
     */
    protected boolean isDragonSpawningCrystal(Location loc) {
        return loc.getWorld().equals(CONFIG.END_PORTAL_LOCATION.getWorld()) &&
               loc.distance(CONFIG.END_PORTAL_LOCATION) < CONFIG.END_PORTAL_RADIUS;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the given player can build at the given location.
     * Effectively replaces the lost functionality of WorldGuardPlugin#canBuild.
     *
     * @param player the player.
     * @param location the location.
     * @return true if the given player can build at the given location.
     */
    private boolean canBuild(Player player, Location location) {
        com.sk89q.worldedit.util.Location wrappedLocation = BukkitAdapter.adapt(location);
        LocalPlayer localPlayer = _worldGuard.wrapPlayer(player);
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().testBuild(wrappedLocation, localPlayer);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if the crystal is associated with the dragon fight.
     * 
     * For this purpose, any crystal on bedrock in the end is assumed to be part
     * of the dragon fight. These crystals are not protected (they will behave
     * as in vanilla).
     * 
     * @param loc the location of the end crystal.
     * @return true if the crystal is associated with the dragon fight.
     */
    private static boolean isDragonFightCrystal(Location loc) {
        Block blockUnder = loc.getBlock().getRelative(0, -1, 0);
        return loc.getWorld().getEnvironment() == Environment.THE_END &&
               blockUnder != null && blockUnder.getType() == Material.BEDROCK;
    }

    // ------------------------------------------------------------------------
    /**
     * Reference to WorldGuard.
     */
    protected WorldGuardPlugin _worldGuard;
} // class SafeCrystals