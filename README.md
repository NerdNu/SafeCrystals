SafeCrystals
------------
A Bukkit plugin to prevent Ender Crystals from exploding and breaking blocks or
damaging entities.

Players who can build in a WorldGuard region can break the crystals, causing
them to drop as an item. That event is recorded in the server log. Projectiles
launched by the player are handled the same as if the player had attacked the
Ender Crystal directly.

Players are prevented from placing Ender Crystals in a region if they don't have
build permission.
