package think.rpgitems.power;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.BlockIterator;

import think.rpgitems.Plugin;
import think.rpgitems.commands.Commands;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGValue;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;

public class PowerTeleport extends Power {

    private int distance = 5;
    private long cd = 20;

    @Override
    public void rightClick(Player player) {
        long cooldown;
        RPGValue value = RPGValue.get(player, item, "teleport.cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "teleport.cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cd);
            // float dist = 0;
            World world = player.getWorld();
            Location start = player.getLocation();
            start.setY(start.getY() + 1.6);
            //Location current = new Location(world, 0, 0, 0);
            Block lastSafe = world.getBlockAt(start);
            //Keeping the old method because BlockIterator could get removed (irc)
            // double dir = Math.toRadians(start.getYaw()) + (Math.PI / 2d);
            // double dirY = Math.toRadians(start.getPitch()) + (Math.PI / 2d);
            BlockIterator bi = new BlockIterator(player, distance);
            // while (dist < distance) {
            while (bi.hasNext()) {
                // current.setX(start.getX() + dist * Math.cos(dir) *
                // Math.sin(dirY));
                // current.setY(start.getY() + dist * Math.cos(dirY));
                // current.setZ(start.getZ() + dist * Math.sin(dir) *
                // Math.sin(dirY));
                Block block = bi.next();// world.getBlockAt(current);
                if (!block.getType().isSolid() || (block.getType() == Material.AIR)) {
                    lastSafe = block;
                } else {
                    break;
                }
                // dist+= 0.5;
            }
            Location newLoc = lastSafe.getLocation();
            newLoc.setPitch(start.getPitch());
            newLoc.setYaw(start.getYaw());
            player.teleport(newLoc);
            world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            world.playSound(newLoc, Sound.ENDERMAN_TELEPORT, 1.0f, 0.3f);
        } else {
            player.sendMessage(ChatColor.AQUA + String.format(Locale.get("MESSAGE_COOLDOWN"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
        }
    }

    @Override
    public void projectileHit(Player player, Projectile p) {
        long cooldown;
        RPGValue value = RPGValue.get(player, item, "teleport.cooldown");
        if (value == null) {
            cooldown = System.currentTimeMillis() / 50;
            value = new RPGValue(player, item, "teleport.cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= System.currentTimeMillis() / 50) {
            value.set(System.currentTimeMillis() / 50 + cd);
            World world = player.getWorld();
            Location start = player.getLocation();
            Location newLoc = p.getLocation();
            if (start.distanceSquared(newLoc) >= distance * distance) {
                player.sendMessage(ChatColor.AQUA + Locale.get("MESSAGE_TOO_FAR"));
                return;
            }
            newLoc.setPitch(start.getPitch());
            ;
            newLoc.setYaw(start.getYaw());
            player.teleport(newLoc);
            world.playEffect(newLoc, Effect.ENDER_SIGNAL, 0);
            world.playSound(newLoc, Sound.ENDERMAN_TELEPORT, 1.0f, 0.3f);
        } else {
            player.sendMessage(ChatColor.AQUA + String.format(Locale.get("MESSAGE_COOLDOWN"), ((double) (cooldown - System.currentTimeMillis() / 50)) / 20d));
        }
    }

    @Override
    public void init(ConfigurationSection s) {
        cd = s.getLong("cooldown");
        distance = s.getInt("distance");
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldown", cd);
        s.set("distance", distance);
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get("POWER_TELEPORT"), distance, (double) cd / 20d);
    }

    static {
        Commands.add("rpgitem $n[] power teleport", new Commands() {

            @Override
            public String getDocs() {
                return Locale.get("COMMAND_RPGITEM_TELEPORT");
            }

            @Override
            public void command(CommandSender sender, Object[] args) {
                RPGItem item = (RPGItem) args[0];
                PowerTeleport pow = new PowerTeleport();
                pow.item = item;
                pow.cd = 20;
                pow.distance = 5;
                item.addPower(pow);
                ItemManager.save(Plugin.plugin);
                sender.sendMessage(ChatColor.AQUA + Locale.get("MESSAGE_POWER_OK"));
            }
        });
        Commands.add("rpgitem $n[] power teleport $COOLDOWN:i[] $DISTANCE:i[]", new Commands() {

            @Override
            public String getDocs() {
                return Locale.get("COMMAND_RPGITEM_TELEPORT_FULL");
            }

            @Override
            public void command(CommandSender sender, Object[] args) {
                RPGItem item = (RPGItem) args[0];
                PowerTeleport pow = new PowerTeleport();
                pow.item = item;
                pow.cd = (Integer) args[1];
                pow.distance = (Integer) args[2];
                item.addPower(pow);
                ItemManager.save(Plugin.plugin);
                sender.sendMessage(ChatColor.AQUA + Locale.get("MESSAGE_POWER_OK"));
            }
        });
    }

}