package plus.crates;

import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import plus.crates.Commands.CrateCommand;
import plus.crates.Crates.Crate;
import plus.crates.Crates.KeyCrate;
import plus.crates.Handlers.*;
import plus.crates.Listeners.BlockListeners;
import plus.crates.Listeners.GUIListeners;
import plus.crates.Listeners.PlayerInteract;
import plus.crates.Listeners.PlayerJoin;
import plus.crates.Utils.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CratesPlus extends JavaPlugin implements Listener {
    private String pluginPrefix = "";
    private String updateMessage = "";
    private String configBackup = null;
    private boolean updateAvailable = false;
    private ConfigHandler configHandler;
    private CrateHandler crateHandler;
    private SettingsHandler settingsHandler;
    private HologramHandler hologramHandler;
    private StorageHandler storageHandler;
    private String bukkitVersion = "0.0";
    private Version_Util version_util;
    private static OpenHandler openHandler;
    private ArrayList<UUID> creatingCrate = new ArrayList<>();

    public void onEnable() {
        Server server = getServer();
        Pattern pattern = Pattern.compile("(^[^\\-]*)");
        Matcher matcher = pattern.matcher(server.getBukkitVersion());
        if (!matcher.find()) {
            getLogger().severe("无法找到 Bukkit 版本... 正在禁用插件");
            setEnabled(false);
            return;
        }
        bukkitVersion = matcher.group(1);

        if (getConfig().isSet("Bukkit Version"))
            bukkitVersion = getConfig().getString("Bukkit Version");

        if (LinfootUtil.versionCompare(bukkitVersion, "1.14.2") > 0) {
            // This means the plugin is using something newer than the latest tested build... we'll show a warning but carry on as usual
            getLogger().warning("CratesPlus 尚未在 Bukkit " + bukkitVersion + " 上进行官方测试，但应该仍然可以正常使用");
        }

        if (LinfootUtil.versionCompare(bukkitVersion, "1.9") > -1) {
            // Use 1.9+ Util
            version_util = new Version_1_9(this);
        } else if (LinfootUtil.versionCompare(bukkitVersion, "1.8") > -1) {
            // Use 1.8 Util
            version_util = new Version_1_8(this);
        } else if (LinfootUtil.versionCompare(bukkitVersion, "1.7") > -1) {
            // Use Default Util
            getLogger().warning("CratesPlus 不完全支持 Bukkit 1.7，如果您遇到问题请报告，但可能不会修复");
            version_util = new Version_Util(this);
        } else {
            getLogger().severe("CratesPlus 不支持 Bukkit " + bukkitVersion + "，如果您认为这是错误请联系我");
            if (!getConfig().isSet("Ignore Version") || !getConfig().getBoolean("Ignore Version")) { // People should only ignore this in the case of an error, doing an ignore on a unsupported version could break something
                setEnabled(false);
                return;
            }
            version_util = new Version_Util(this); // Use the 1.7 util? Probably has a lower chance of breaking
        }

        final ConsoleCommandSender console = server.getConsoleSender();
        getConfig().options().copyDefaults(true);
        saveConfig();

        hologramHandler = new HologramHandler();

        StorageHandler.StorageType storageType = StorageHandler.StorageType.FLAT;
        try {
            storageType = StorageHandler.StorageType.valueOf(getConfig().getString("Storage Type", "FLAT").toUpperCase());
        } catch (Exception e) {
            getLogger().warning(getConfig().getString("Storage Type", "FLAT") + " 不是有效的存储类型！将回退到 FLAT！");
        }
        storageHandler = new StorageHandler(this, storageType);

        // Load new messages.yml
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
                InputStream inputStream = getResource("messages.yml");
                OutputStream outputStream = new FileOutputStream(messagesFile);
                ByteStreams.copy(inputStream, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        MessageHandler.loadMessageConfiguration(this, messagesConfig, messagesFile);

        configHandler = new ConfigHandler(getConfig(), this);

        if (getConfig().getBoolean("Metrics")) {
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();

                MetricsCustom metricsCustom = new MetricsCustom(this);
                metricsCustom.start();
            } catch (IOException e) {
                // Failed to submit the stats :-(
            }
        }

        // Load the crate handler
        crateHandler = new CrateHandler(this);

        // Do Prefix
        pluginPrefix = ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("Prefix", "&7[&bCratesPlus&7]")) + " " + ChatColor.RESET;

        // Register /crate command
        Bukkit.getPluginCommand("crate").setExecutor(new CrateCommand(this));

        // Register Events
        Bukkit.getPluginManager().registerEvents(new BlockListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoin(this), this);
        Bukkit.getPluginManager().registerEvents(new GUIListeners(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteract(this), this);

        openHandler = new OpenHandler(this);

        settingsHandler = new SettingsHandler(this);

        loadMetaData();

        console.sendMessage(ChatColor.AQUA + getDescription().getName() + " 版本 " + getDescription().getVersion());
        if (getDescription().getVersion().contains("SNAPSHOT")) { // 添加这个是因为有些人不太理解什么是"snapshot"...
            console.sendMessage(ChatColor.RED + "警告：您正在运行 CratesPlus 的快照版本");
            console.sendMessage(ChatColor.RED + "建议您不要在正式服务器上运行此版本！");
        }

        switch (getHologramHandler().getHologramPlugin()) {
            default:
            case NONE:
                console.sendMessage(ChatColor.RED + "未找到兼容的全息插件，全息显示将无法使用！");
                break;
            case HOLOGRAPHIC_DISPLAYS:
                console.sendMessage(ChatColor.GREEN + "已找到 HolographicDisplays，正在接入！");
                break;
            case INDIVIDUAL_HOLOGRAMS:
                console.sendMessage(ChatColor.GREEN + "已找到 IndividualHolograms，正在接入！");
                break;
        }

        if (configBackup != null && Bukkit.getOnlinePlayers().size() > 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("cratesplus.admin")) {
                    player.sendMessage(pluginPrefix + ChatColor.GREEN + "您的配置已更新。旧配置已备份至 " + configBackup);
                    configBackup = null;
                }
            }
        }

        if (getConfig().getBoolean("Update Checks", true)) {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> checkUpdate(console), 10L);
        }
    }

    public void onDisable() {
        getConfigHandler().getCrates().forEach((key, crate) -> crate.onDisable());
    }

    public String uploadConfig() {
        return uploadFile("config.yml");
    }

    public String uploadData() {
        return uploadFile("data.yml");
    }

    public String uploadMessages() {
        return uploadFile("messages.yml");
    }

    public String uploadFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists())
            return null;
        LineIterator it;
        String lines = "";
        try {
            it = FileUtils.lineIterator(file, "UTF-8");
            try {
                while (it.hasNext()) {
                    String line = it.nextLine();
                    lines += line + "\n";
                }
            } finally {
                it.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return MCDebug.paste(fileName, lines);
    }

    private void checkUpdate(final ConsoleCommandSender console) {
        String updateBranch = getConfig().getString("Update Branch");

        if (getDescription().getVersion().contains("SNAPSHOT"))
            updateBranch = "snapshot";//Force snapshot branch on snapshot builds

        String branch = updateBranch.toLowerCase();

        if (branch.equalsIgnoreCase("snapshot")) {
            console.sendMessage(ChatColor.RED + "警告：不建议在正式服务器上使用快照版本更新");
        }
        console.sendMessage(ChatColor.GREEN + "正在通过 " + branch + " 分支检查更新...");
        final LinfootUpdater updater = new LinfootUpdater(this, branch);
        final LinfootUpdater.UpdateResult snapShotResult = updater.getResult();
        switch (snapShotResult) {
            default:
            case FAILED:
                updateAvailable = false;
                updateMessage = pluginPrefix + "检查更新失败。稍后将会重试。";
                getServer().getScheduler().runTaskLaterAsynchronously(this, () -> checkUpdate(console), 60 * (60 * 20L)); // 一小时后再次检查
                break;
            case NO_UPDATE:
                updateAvailable = false;
                updateMessage = pluginPrefix + "未找到更新，您正在运行最新版本。稍后将会再次检查。";
                getServer().getScheduler().runTaskLaterAsynchronously(this, () -> checkUpdate(console), 60 * (60 * 20L)); // Checks again an hour later
                break;
            case SNAPSHOT_UPDATE_AVAILABLE:
                updateAvailable = true;
                updateMessage = pluginPrefix + "CratesPlus 的快照更新可用，新版本为 " + updater.getVersion() + "。您当前的版本为 " + getDescription().getVersion() + "。\n请更新到最新版本 :)";
                break;
            case UPDATE_AVAILABLE:
                updateAvailable = true;
                updateMessage = pluginPrefix + "CratesPlus 的更新可用，新版本为 " + updater.getVersion() + "。您当前的版本为 " + getDescription().getVersion() + "。\n请更新到最新版本 :)";
                break;
        }

        if (updateMessage != null)
            console.sendMessage(updateMessage);

    }

    public void reloadPlugin() {
        reloadConfig();

        // Do Prefix
        pluginPrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Prefix", "&7[&bCratesPlus&7]")) + " " + ChatColor.RESET;

        // Reload Configuration
        configHandler = new ConfigHandler(getConfig(), this);

        // Settings Handler
        settingsHandler = new SettingsHandler(this);

    }

    private void loadMetaData() {
        if (!getStorageHandler().getFlatConfig().isSet("Crate Locations"))
            return;
        for (String name : getStorageHandler().getFlatConfig().getConfigurationSection("Crate Locations").getKeys(false)) {
            final Crate crate = configHandler.getCrate(name.toLowerCase());
            if (crate == null)
                continue;
            if (!(crate instanceof KeyCrate))
                continue;
            KeyCrate keyCrate = (KeyCrate) crate;
            String path = "Crate Locations." + name;
            List<String> locations = getStorageHandler().getFlatConfig().getStringList(path);

            for (String location : locations) {
                List<String> strings = Arrays.asList(location.split("\\|"));
                if (strings.size() < 4)
                    continue; // Somethings broke?
                if (strings.size() > 4) {
                    // Somethings broke? But we'll try and fix it!
                    for (int i = 0; i < strings.size(); i++) {
                        if (strings.get(i).isEmpty() || strings.get(i).equals("")) {
                            strings.remove(i);
                        }
                    }
                }
                Location locationObj;
                try {
                    locationObj = new Location(Bukkit.getWorld(strings.get(0)), Double.parseDouble(strings.get(1)), Double.parseDouble(strings.get(2)), Double.parseDouble(strings.get(3)));
                    Block block = locationObj.getBlock();
                    if (block == null || block.getType().equals(Material.AIR)) {
                        getLogger().warning("在 " + location + " 处未找到方块，正在从 data.yml 中移除");
                        keyCrate.removeFromConfig(locationObj);
                        continue;
                    }
                    Location location1 = locationObj.getBlock().getLocation().add(0.5, 0.5, 0.5);
                    keyCrate.loadHolograms(location1);
                    final CratesPlus cratesPlus = this;
                    block.setMetadata("CrateType", new MetadataValue() {
                        @Override
                        public Object value() {
                            return crate.getName(false);
                        }

                        @Override
                        public int asInt() {
                            return 0;
                        }

                        @Override
                        public float asFloat() {
                            return 0;
                        }

                        @Override
                        public double asDouble() {
                            return 0;
                        }

                        @Override
                        public long asLong() {
                            return 0;
                        }

                        @Override
                        public short asShort() {
                            return 0;
                        }

                        @Override
                        public byte asByte() {
                            return 0;
                        }

                        @Override
                        public boolean asBoolean() {
                            return false;
                        }

                        @Override
                        public String asString() {
                            return value().toString();
                        }

                        @Override
                        public Plugin getOwningPlugin() {
                            return cratesPlus;
                        }

                        @Override
                        public void invalidate() {

                        }
                    });
                } catch (Exception ignored) {
                }
            }


        }
    }

    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    public String getPluginPrefix() {
        return pluginPrefix;
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public HologramHandler getHologramHandler() {
        return hologramHandler;
    }

    public StorageHandler getStorageHandler() {
        return storageHandler;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public String getConfigBackup() {
        return configBackup;
    }

    public void setConfigBackup(String configBackup) {
        this.configBackup = configBackup;
    }

    public Version_Util getVersion_util() {
        return version_util;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public CrateHandler getCrateHandler() {
        return crateHandler;
    }

    public static OpenHandler getOpenHandler() {
        return openHandler;
    }

    public String getBukkitVersion() {
        return bukkitVersion;
    }

    public boolean isCreating(UUID uuid) {
        return creatingCrate.contains(uuid);
    }

    public void addCreating(UUID uuid) {
        creatingCrate.add(uuid);
    }

    public void removeCreating(UUID uuid) {
        creatingCrate.remove(uuid);
    }

}
