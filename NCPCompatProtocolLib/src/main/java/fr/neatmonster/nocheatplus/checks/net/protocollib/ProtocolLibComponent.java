package fr.neatmonster.nocheatplus.checks.net.protocollib;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.components.DisableListener;
import fr.neatmonster.nocheatplus.components.INotifyReload;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.StringUtil;

/**
 * Quick and dirty ProtocolLib setup.
 * @author dev1mc
 *
 */
public class ProtocolLibComponent implements DisableListener, INotifyReload {

    // TODO: Static reference is problematic (needs a static and accessible Counters instance?). 
    public static final int idNullPlayer = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class).registerKey("packet.flying.nullplayer");

    private final List<PacketAdapter> registeredPacketAdapters = new LinkedList<PacketAdapter>();

    public ProtocolLibComponent(Plugin plugin) {
        register(plugin);
    }

    private void register(Plugin plugin) {
        StaticLog.logInfo("Adding packet level hooks for ProtocolLib (MC " + ProtocolLibrary.getProtocolManager().getMinecraftVersion().getVersion() + ")...");
        if (ConfigManager.isTrueForAnyConfig(ConfPaths.NET + ConfPaths.SUB_DEBUG) || ConfigManager.isTrueForAnyConfig(ConfPaths.CHECKS_DEBUG) ) {
            // (Debug logging. Only activates if debug is set for checks or checks.net, not on the fly.)
            register("fr.neatmonster.nocheatplus.checks.net.protocollib.DebugAdapter", plugin);
        }
        if (ConfigManager.isTrueForAnyConfig(ConfPaths.NET_FLYINGFREQUENCY_ACTIVE)) {
            // (Also sets lastKeepAliveTime, if enabled.)
            register("fr.neatmonster.nocheatplus.checks.net.protocollib.FlyingFrequency", plugin);
        }
        if (ConfigManager.isTrueForAnyConfig(ConfPaths.NET_KEEPALIVEFREQUENCY_ACTIVE) || ConfigManager.isTrueForAnyConfig(ConfPaths.FIGHT_GODMODE_CHECK)) {
            // (Set lastKeepAlive if this or fight.godmode is enabled.)
            register("fr.neatmonster.nocheatplus.checks.net.protocollib.KeepAliveFrequency", plugin);
        }
        if (ConfigManager.isTrueForAnyConfig(ConfPaths.NET_SOUNDDISTANCE_ACTIVE)) {
            register("fr.neatmonster.nocheatplus.checks.net.protocollib.SoundDistance", plugin);
        }
        if (!registeredPacketAdapters.isEmpty()) {
            List<String> names = new ArrayList<String>(registeredPacketAdapters.size());
            for (PacketAdapter adapter : registeredPacketAdapters) {
                names.add(adapter.getClass().getSimpleName());
            }
            StaticLog.logInfo("[NoCheatPlus] Available (and activated) packet level hooks: " + StringUtil.join(names, " | "));
            NCPAPIProvider.getNoCheatPlusAPI().addFeatureTags("checks", names);
        } else {
            StaticLog.logInfo("[NoCheatPlus] No packet level hooks activated.");
        }
    }

    @SuppressWarnings("unchecked")
    private void register(String name, Plugin plugin) {
        Throwable t = null;
        try {
            Class<?> clazz = Class.forName(name);
            register((Class<? extends PacketAdapter>) clazz, plugin);
            return;
        } catch (ClassNotFoundException e) {
            t = e;
        } catch (ClassCastException e) {
            t = e;
        }
        StaticLog.logWarning("[NoCheatPlus] Could not register packet level hook: " + name);
        StaticLog.logWarning(t);
    }

    private void register(Class<? extends PacketAdapter> clazz, Plugin plugin) {
        try {
            // Construct a new instance using reflection.
            PacketAdapter adapter = clazz.getDeclaredConstructor(Plugin.class).newInstance(plugin);
            ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
            registeredPacketAdapters.add(adapter);
        } catch (Throwable t) {
            StaticLog.logWarning("[NoCheatPlus] Could not register packet level hook: " + clazz.getSimpleName());
            StaticLog.logWarning(t);
        }
    }

    @Override
    public void onDisable() {
        unregister();
    }

    @Override
    public void onReload() {
        unregister();
        CheckType.NET.getDataFactory().removeAllData(); // Currently needed for FlyingFrequency.
        register(Bukkit.getPluginManager().getPlugin("NoCheatPlus")); // Store instead ?
    }

    private void unregister() {
        final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        for (PacketAdapter adapter : registeredPacketAdapters) {
            try {
                protocolManager.removePacketListener(adapter);
                api.removeComponent(adapter); // Bit heavy, but consistent.
            } catch (Throwable t) {
                StaticLog.logWarning("[NoCheatPlus] Failed to unregister packet level hook: " + adapter.getClass().getName());
            }
        }
        registeredPacketAdapters.clear();
    }

}
