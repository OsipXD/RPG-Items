/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import think.rpgitems.Plugin;

public class Locale extends BukkitRunnable {
    
    private static Method getHandle;
    private static Method getLocale;
    private static Field language;
    private static boolean canLocale = true;
    private static boolean firstTime = true;
    
    private static HashMap<String, HashMap<String, String>> localeStrings = new HashMap<String, HashMap<String,String>>();
    
    private Plugin plugin;
    private long lastUpdate = 0;
    private File dataFolder;
    private String version;
    private Locale(Plugin plugin) {
        this.plugin = plugin;
        lastUpdate = plugin.getConfig().getLong("lastLocaleUpdate", 0);
        dataFolder = plugin.getDataFolder();
        version = plugin.getDescription().getVersion();
        reloadLocales(plugin);
    }
    
    private final static String localeUpdateURL = "http://thinkofdeath.planetofhosting.net/index.php?page=localeget&lastupdate=";
    private final static String localeDownloadURL = "http://thinkofdeath.planetofhosting.net/locale/%s/%s.lang";

    @Override
    public void run() {
        try {
            URL updateURL = new URL(localeUpdateURL + lastUpdate);
            lastUpdate = System.currentTimeMillis();
            URLConnection conn = updateURL.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));     
            ArrayList<String> locales = new ArrayList<String>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                locales.add(line);
            }
            reader.close();
            File localesFolder = new File(dataFolder, "locale/");
            localesFolder.mkdirs();
            for (String locale : locales) {
                URL downloadURL = new URL(String.format(localeDownloadURL, version, locale));
                File outFile = new File(dataFolder, "locale/" + locale + ".lang");
                InputStream in = downloadURL.openStream();
                FileOutputStream out = new FileOutputStream(outFile);
                byte []buf = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buf)) != -1) {
                    out.write(buf, 0, bytesRead);
                }
                in.close();
                out.close();
            }
        } catch (Exception e) {
            return;
        }
        (new BukkitRunnable() {            
            @Override
            public void run() {
                ConfigurationSection config = plugin.getConfig();
                config.set("lastLocaleUpdate", lastUpdate);
                plugin.saveConfig();
                reloadLocales(plugin);
            }
        }).runTask(plugin);
    }
    
    public static void reloadLocales(Plugin plugin) {
        localeStrings.clear();
        localeStrings.put("en_GB", loadLocaleStream(plugin.getResource("locale/en_GB.lang")));

        File localesFolder = new File(plugin.getDataFolder(), "locale/");
        localesFolder.mkdirs();
        
        for (File file : localesFolder.listFiles()) {
            if (!file.isDirectory() && file.getName().endsWith(".lang")) {

                FileInputStream in = null;
                try {
                    String locale = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    HashMap<String, String> map = localeStrings.get(locale);
                    map = map == null ? new HashMap<String, String>() : map;
                    in = new FileInputStream(file);
                    localeStrings.put(locale, loadLocaleStream(in, map));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                
            }
        }
    }
    
    private static HashMap<String, String> loadLocaleStream(InputStream in, HashMap<String, String> map) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = null;
            while((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] args = line.split("=");
                map.put(args[0].trim(), args[1].trim());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static HashMap<String, String> loadLocaleStream(InputStream in) {
        return loadLocaleStream(in, new HashMap<String, String>());
    }
    
    public static String getPlayerLocale(Player player) {
        if (firstTime) {
            try {
                getHandle = player.getClass().getMethod("getHandle", (Class<?>[]) null);
                getLocale = getHandle.getReturnType().getMethod("getLocale", (Class<?>[]) null);
                language = getLocale.getReturnType().getDeclaredField("e");
                if (!language.getType().equals(String.class)) {
                    language.setAccessible(true);
                    canLocale = false;
                }
            } catch (Exception e) {
                Plugin.plugin.getLogger().warning("Failed to get player locale");
                canLocale = false;
            }
            firstTime = false;
        }
        if (!canLocale) {
            return "en_GB";
        }
        try {
            Object minePlayer = getHandle.invoke(player,(Object[]) null);
            Object locale = getLocale.invoke(minePlayer, (Object[]) null);
            return (String) language.get(locale);
        } catch (Exception e) {
            canLocale = false;
        } 
        //Any error default to en_GB
        return "en_GB";
    }

    private static HashMap<String, String> strings = new HashMap<String, String>();

    public static void init(Plugin plugin) {
        (new Locale(plugin)).runTaskTimerAsynchronously(plugin, 0, 24l * 60l * 60l * 20l);
        try {
            InputStream defs = plugin.getResource("default.lang");
            load(defs);
            defs.close();
            File altFile = new File(plugin.getDataFolder(), "default.lang");
            if (altFile.exists()) {
                FileInputStream alts = new FileInputStream(altFile);
                load(alts);
                alts.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void load(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("-") || line.length() == 0)
                continue;
            String[] args = line.split("=");
            args[0] = args[0].trim();
            args[1] = args[1].trim();
            args[1] = args[1].substring(1, args[1].length() - 1);
            strings.put(args[0], args[1]);
        }
    }

    public static String get(String name) {
        if (strings.containsKey(name))
            return strings.get(name);
        return "Locale error: " + name;
    }
}
