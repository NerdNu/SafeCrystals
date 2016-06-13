package nu.nerd.safecrystals;

import org.bukkit.Location;

// ----------------------------------------------------------------------------
/**
 * Configuration wrapper.
 */
public class Configuration {
    /**
     * Radius around END_PORTAL_LOCATION where broken ender crystals don't drop.
     */
    public double END_PORTAL_RADIUS;

    /**
     * Location of the (assumed only) end-side end portal.
     */
    public Location END_PORTAL_LOCATION;

    // ------------------------------------------------------------------------
    /**
     * Reload the configuration.
     */
    public void reload() {
        SafeCrystals.PLUGIN.reloadConfig();

        END_PORTAL_RADIUS = SafeCrystals.PLUGIN.getConfig().getDouble("end-portal.radius");
        END_PORTAL_LOCATION = (Location) SafeCrystals.PLUGIN.getConfig().get("end-portal.location");
    }
} // class Configuration