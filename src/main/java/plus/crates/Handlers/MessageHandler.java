package plus.crates.Handlers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import plus.crates.Crates.Crate;
import plus.crates.Crates.Winning;
import plus.crates.CratesPlus;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MessageHandler {
    private static CratesPlus cratesPlus;
    private static YamlConfiguration config;
    private static File file;
    private static HashMap<String, String> messages = new HashMap<>();
    public static boolean testMessages = false;

    public static void loadMessageConfiguration(CratesPlus cratesPlus, YamlConfiguration config, File file) {
        MessageHandler.cratesPlus = cratesPlus;
        MessageHandler.config = config;
        MessageHandler.file = file;

        handleConversion();

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleConversion() {
        if (cratesPlus == null || config == null || file == null) {
            return;
        }

        if (!config.isSet("Messages Version")) {
            config.set("Messages Version", 2);

            HashMap<String, String> oldKeys = new HashMap<>();
            oldKeys.put("Command No Permission", "&c您没有正确的权限来执行此命令");
            oldKeys.put("Crate No Permission", "&c您没有正确的权限来使用此宝箱");
            oldKeys.put("Crate Open Without Key", "&c您必须手持 %crate% &c钥匙才能打开此宝箱");
            oldKeys.put("Key Given", "&a您已获得一个 %crate% &a宝箱钥匙");
            oldKeys.put("Broadcast", "&d%displayname% &d打开了一个 %crate% &d宝箱");
            oldKeys.put("Cant Place", "&c您不能放置宝箱钥匙");
            oldKeys.put("Cant Drop", "&c您不能丢弃宝箱钥匙");
            oldKeys.put("Chance Message", "&d%percentage%% 概率");
            oldKeys.put("Inventory Full Claim", "&a您的背包已满，您可以稍后使用 /crate 来领取钥匙");
            oldKeys.put("Claim Join", "&a您目前有待领取的钥匙，使用 /crate 来领取");
            oldKeys.put("Possible Wins Title", "可能获得的奖励：");

            oldKeys.forEach((key, value) -> {
                if (config.isSet(key)) {
                    config.set(value, config.getString(key));
                    config.set(key, null);
                }
            });

            cratesPlus.getConfig().set("Prefix", config.getString("Prefix", "&7[&bCratesPlus&7]"));
            config.set("Prefix", null);
            cratesPlus.getConfig().set("Chance Message Gap", config.getBoolean("Chance Message Gap", true));
            config.set("Chance Message Gap", null);
            cratesPlus.saveConfig();

            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getMessageFromConfig(String message) {
        if (testMessages) {
            return "TRANSLATED(" + message + ChatColor.RESET + ") ";
        }
        if (messages.containsKey(message))
            return message;
        // TODO Insert into config reeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee
        return message;
    }

    public static String convertPlaceholders(String message, Player player, Crate crate, Winning winning) {
        message = ChatColor.translateAlternateColorCodes('&', message);
        if (player != null)
            message = message.replaceAll("%name%", player.getName()).replaceAll("%displayname%", player.getDisplayName()).replaceAll("%uuid%", player.getUniqueId().toString());

        if (crate != null)
            message = message.replaceAll("%crate%", crate.getName(true) + ChatColor.RESET);

        if (winning != null) {
            String name;
            if (winning.getWinningItemStack().hasItemMeta() && winning.getWinningItemStack().getItemMeta().hasDisplayName()) {
                name = winning.getWinningItemStack().getItemMeta().getDisplayName();
            } else {
                name = winning.getWinningItemStack().getType().name();
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            message = message.replaceAll("%prize%", name + ChatColor.RESET).replaceAll("%winning%", name + ChatColor.RESET).replaceAll("%percentage%", String.valueOf(winning.getPercentage()));
        }
        return message;
    }

    public static String getMessage(String message, Player player, Crate crate, Winning winning) {
        message = getMessageFromConfig(message);
        return ChatColor.translateAlternateColorCodes('&', convertPlaceholders(message, player, crate, winning));
    }

    public static void sendMessage(Player player, String message, Crate crate, Winning winning) {
        player.sendMessage(cratesPlus.getPluginPrefix() + getMessage(message, player, crate, winning));
    }

}
