package plus.crates.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import plus.crates.Crates.Crate;
import plus.crates.Crates.KeyCrate;
import plus.crates.Crates.MysteryCrate;
import plus.crates.CratesPlus;
import plus.crates.Handlers.MessageHandler;
import plus.crates.Opener.Opener;
import plus.crates.Utils.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class CrateCommand implements CommandExecutor {
    private final CratesPlus cratesPlus;

    public CrateCommand(CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String string, String[] args) {

        if (sender instanceof Player && !sender.hasPermission("cratesplus.admin")) {
            if (args.length == 0 || (args.length > 0 && args[0].equalsIgnoreCase("claim"))) {
                // 假设是玩家，显示"领取" GUI
                doClaim((Player) sender);
                return true;
            }
            sender.sendMessage(cratesPlus.getPluginPrefix() + MessageHandler.getMessage("&c你没有正确的权限来执行此命令", (Player) sender, null, null));
            return false;
        }

        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {
                default:
                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "未知参数");
                    break;
                case "testmessages":
                    MessageHandler.testMessages = !MessageHandler.testMessages;
                    sender.sendMessage(ChatColor.GREEN + "测试消息 " + (MessageHandler.testMessages ? "已启用" : "已禁用"));
                    break;
                case "testeggs":
                    Player player = null;
                    if (sender instanceof Player)
                        player = (Player) sender;

                    sender.sendMessage(ChatColor.AQUA + "正在创建苦力怕刷怪蛋...");
                    ItemStack itemStack = cratesPlus.getVersion_util().getSpawnEgg(EntityType.CREEPER, 1);
                    sender.sendMessage(ChatColor.AQUA + "正在测试苦力怕刷怪蛋...");
                    SpawnEggNBT spawnEggNBT = SpawnEggNBT.fromItemStack(itemStack);
                    if (spawnEggNBT.getSpawnedType().equals(EntityType.CREEPER)) {
                        sender.sendMessage(ChatColor.GREEN + "苦力怕刷怪蛋测试成功");
                        if (player != null)
                            player.getInventory().addItem(itemStack);
                    } else {
                        sender.sendMessage(ChatColor.RED + "苦力怕刷怪蛋测试失败，请在 GitHub 上提交控制台信息");
                    }

                    sender.sendMessage(ChatColor.AQUA + "正在创建蜘蛛刷怪蛋...");
                    itemStack = cratesPlus.getVersion_util().getSpawnEgg(EntityType.SPIDER, 2);
                    sender.sendMessage(ChatColor.AQUA + "正在测试蜘蛛刷怪蛋...");
                    spawnEggNBT = SpawnEggNBT.fromItemStack(itemStack);
                    if (spawnEggNBT.getSpawnedType().equals(EntityType.SPIDER)) {
                        sender.sendMessage(ChatColor.GREEN + "蜘蛛刷怪蛋测试成功");
                        if (player != null)
                            player.getInventory().addItem(itemStack);
                    } else {
                        sender.sendMessage(ChatColor.RED + "蜘蛛刷怪蛋测试失败，请在 GitHub 上提交控制台信息");
                    }

                    sender.sendMessage(ChatColor.AQUA + "正在创建蠹虫刷怪蛋...");
                    itemStack = cratesPlus.getVersion_util().getSpawnEgg(EntityType.SILVERFISH, 3);
                    sender.sendMessage(ChatColor.AQUA + "正在测试蠹虫刷怪蛋...");
                    spawnEggNBT = SpawnEggNBT.fromItemStack(itemStack);
                    if (spawnEggNBT.getSpawnedType().equals(EntityType.SILVERFISH)) {
                        sender.sendMessage(ChatColor.GREEN + "蠹虫刷怪蛋测试成功");
                        if (player != null)
                            player.getInventory().addItem(itemStack);
                    } else {
                        sender.sendMessage(ChatColor.RED + "蠹虫刷怪蛋测试失败，请在 GitHub 上提交控制台信息");
                    }
                    break;
                case "claim":
                    if (sender instanceof Player) {
                        doClaim((Player) sender);
                    }
                    break;
                case "debug":
                    sender.sendMessage(ChatColor.AQUA + "正在收集调试数据...");

                    Bukkit.getScheduler().runTaskAsynchronously(cratesPlus, () -> {
                        sender.sendMessage(ChatColor.AQUA + "正在上传 config.yml...");
                        String configLink = cratesPlus.uploadConfig();
                        sender.sendMessage(ChatColor.AQUA + "已上传 config.yml");

                        sender.sendMessage(ChatColor.AQUA + "正在上传 data.yml...");
                        String dataLink = cratesPlus.uploadData();
                        sender.sendMessage(ChatColor.AQUA + "已上传 data.yml");

                        sender.sendMessage(ChatColor.AQUA + "正在上传 messages.yml...");
                        String messagesLink = cratesPlus.uploadMessages();
                        sender.sendMessage(ChatColor.AQUA + "已上传 messages.yml");

                        sender.sendMessage(ChatColor.AQUA + "正在生成插件列表...");
                        String plugins = "";
                        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                            plugins += plugin.getName() + " - 版本: " + plugin.getDescription().getVersion() + "\n";
                        }
                        sender.sendMessage(ChatColor.AQUA + "插件列表生成完成");

                        sender.sendMessage(ChatColor.AQUA + "正在上传插件列表...");
                        String pluginsLink = MCDebug.paste("plugins.txt", plugins);
                        sender.sendMessage(ChatColor.AQUA + "已上传插件列表");

                        sender.sendMessage(ChatColor.AQUA + "正在上传数据到 MC Debug...");
                        String finalLinks = uploadDebugData(configLink, dataLink, messagesLink, pluginsLink);
                        String[] links = null;
                        if (finalLinks != null) {
                            links = finalLinks.split("\\|");
                        }

                        sender.sendMessage(ChatColor.GREEN + "调试数据上传完成！");
                        if (links != null && links.length == 2) {
                            sender.sendMessage(ChatColor.GREEN + "您可以使用以下链接管理数据 " + ChatColor.GOLD + links[1]);
                            sender.sendMessage(ChatColor.GREEN + "您可以使用以下链接分享数据 " + ChatColor.GOLD + links[0]);
                        } else {
                            sender.sendMessage(ChatColor.GREEN + "您可以使用以下链接分享数据 " + ChatColor.GOLD + finalLinks);
                        }

                    });
                    break;
                case "opener":
                case "openers":
                    if (args.length > 1) {
                        if (args.length < 3) {
                            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "正确用法: /" + string + " " + args[0] + " <宝箱名称> <开启方式>");
                        } else {
                            if (CratesPlus.getOpenHandler().openerExist(args[2])) {
                                Opener opener = CratesPlus.getOpenHandler().getOpener(args[2]);
                                if (cratesPlus.getConfigHandler().getCrate(args[1].toLowerCase()) == null) {
                                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "该名称的宝箱不存在");
                                } else if (!cratesPlus.getConfigHandler().getCrate(args[1].toLowerCase()).supportsOpener(opener)) {
                                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "开启方式不支持此宝箱类型");
                                } else {
//									cratesPlus.getConfigHandler().getCrate(args[1].toLowerCase()).setOpener(args[2]);
                                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + "已将开启方式设置为 " + args[2]);
                                }
                            } else {
                                sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "该名称的开启方式未注册");
                            }
                        }

                    } else {
                        sender.sendMessage(ChatColor.GOLD + "已注册的开启方式:");
                        sender.sendMessage(ChatColor.AQUA + "名称" + ChatColor.GRAY + " | " + ChatColor.YELLOW + "插件");
                        sender.sendMessage(ChatColor.AQUA + "");
                        for (Map.Entry<String, Opener> map : CratesPlus.getOpenHandler().getRegistered().entrySet()) {
                            sender.sendMessage(ChatColor.AQUA + map.getKey() + ChatColor.GRAY + " | " + ChatColor.YELLOW + map.getValue().getPlugin().getDescription().getName());
                        }
                    }
                    break;
                case "reload":
                    cratesPlus.reloadPlugin();
                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + "CratesPlus 已重新加载 - 此功能未完全支持，可能无法正常工作");
                    break;
                case "settings":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "此命令必须以玩家身份执行");
                        return false;
                    }
                    cratesPlus.getSettingsHandler().openSettings((Player) sender);
                    break;
                case "create":
                    // TODO 处理不同的宝箱类型，目前默认为 KeyCrate
                    if (sender instanceof Player && args.length < 2) {
                        // 尝试打开牌子来输入名称！
                        player = (Player) sender;

                        cratesPlus.addCreating(player.getUniqueId());
                        try {
                            // 发送虚假牌子（1.13+）
                            player.sendBlockChange(player.getLocation(), Material.valueOf("SIGN"), (byte) 0);

                            Constructor signConstructor = ReflectionUtil.getNMSClass("PacketPlayOutOpenSignEditor").getConstructor(ReflectionUtil.getNMSClass("BlockPosition"));
                            Object packet = signConstructor.newInstance(ReflectionUtil.getBlockPosition(player));
                            SignInputHandler.injectNetty(player);
                            ReflectionUtil.sendPacket(player, packet);

                            player.sendBlockChange(player.getLocation(), player.getLocation().getBlock().getType(), player.getLocation().getBlock().getData());
                        } catch (Exception e) {
                            e.printStackTrace();
                            cratesPlus.removeCreating(player.getUniqueId());
                        }
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "正确用法: /crate create <名称>");
                        return false;
                    }

                    String name = args[1];
                    FileConfiguration config = cratesPlus.getConfig();
                    if (config.isSet("Crates." + name)) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + name + " 宝箱已存在");
                        return false;
                    }

                    // 设置示例物品
                    config.set("Crates." + name + ".Winnings.1.Type", "ITEM");
                    config.set("Crates." + name + ".Winnings.1.Item Type", "IRON_SWORD");
                    config.set("Crates." + name + ".Winnings.1.Item Data", 0);
                    config.set("Crates." + name + ".Winnings.1.Percentage", 0);
                    config.set("Crates." + name + ".Winnings.1.Name", "&6&l示例剑");
                    config.set("Crates." + name + ".Winnings.1.Amount", 1);

                    // 设置默认钥匙
                    config.set("Crates." + name + ".Key.Item", "TRIPWIRE_HOOK");
                    config.set("Crates." + name + ".Key.Name", "%type% 宝箱钥匙");
                    config.set("Crates." + name + ".Key.Enchanted", true);

                    config.set("Crates." + name + ".Knockback", 0.0);
                    config.set("Crates." + name + ".Broadcast", false);
                    config.set("Crates." + name + ".Firework", false);
                    config.set("Crates." + name + ".Preview", true);
                    config.set("Crates." + name + ".Block", "CHEST");
                    config.set("Crates." + name + ".Color", "WHITE");
                    config.set("Crates." + name + ".Type", "KeyCrate");
                    cratesPlus.saveConfig();
                    cratesPlus.reloadConfig();

                    cratesPlus.getConfigHandler().registerCrate(cratesPlus, config, name);
                    cratesPlus.getSettingsHandler().setupCratesInventory();

                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + name + " 宝箱已创建");
                    break;
                case "rename":
                    if (args.length < 3) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "正确用法: /crate rename <旧名称> <新名称>");
                        return false;
                    }

                    String oldName = args[1];
                    String newName = args[2];

                    if (!cratesPlus.getConfigHandler().getCrates().containsKey(oldName.toLowerCase())) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + oldName + " 宝箱未找到");
                        return false;
                    }
                    Crate crate = cratesPlus.getConfigHandler().getCrates().get(oldName.toLowerCase());

                    config = cratesPlus.getConfig();
                    if (config.isSet("Crates." + newName)) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + newName + " 宝箱已存在");
                        return false;
                    }

                    LinfootUtil.copyConfigSection(config, "Crates." + crate.getName(), "Crates." + newName);

                    config.set("Crates." + crate.getName(), null);
                    cratesPlus.saveConfig();
                    cratesPlus.reloadConfig();

                    cratesPlus.getConfigHandler().getCrates().remove(oldName.toLowerCase());
                    cratesPlus.getConfigHandler().registerCrate(cratesPlus, config, newName);
                    cratesPlus.getSettingsHandler().setupCratesInventory();

                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + oldName + " 已重命名为 " + newName);
                    break;
                case "delete":
                    if (args.length < 2) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "正确用法: /crate delete <名称>");
                        return false;
                    }

                    name = args[1];
                    config = cratesPlus.getConfig();
                    if (!config.isSet("Crates." + name)) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + name + " 宝箱不存在");
                        return false;
                    }

                    config.set("Crates." + name, null);
                    cratesPlus.saveConfig();
                    cratesPlus.reloadConfig();
                    cratesPlus.getConfigHandler().getCrates().remove(name.toLowerCase());
                    cratesPlus.getSettingsHandler().setupCratesInventory();

                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + name + " 宝箱已删除");
                    break;
                case "mysterygui":
                    if (args.length < 2) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "正确用法: /crate mysterygui <宝箱名称>");
                        return false;
                    }

                    String crateType = args[1];

                    crate = cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase());
                    if (crate == null) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "未找到该宝箱");
                        return false;
                    }

                    if (!(crate instanceof MysteryCrate) || !(sender instanceof Player)) { // 懒得分开写消息了
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "该宝箱不是神秘宝箱！");
                        return false;
                    }

                    ((MysteryCrate) crate).openGUI((Player) sender);
                    break;
                case "key":
                    cratesPlus.getLogger().warning("\"/crate key\" 已从版本 5 弃用，请改用 \"give\"。");
                    if (sender instanceof Player) {
                        sender.sendMessage("\"/crate key\" 已从版本 5 弃用，请改用 \"give\"。");
                    }
                case "give":
                    if (args.length < 3) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "正确用法: /crate give <玩家名称/all/alloffline> <宝箱名称> [数量]");
                        return false;
                    }

                    Integer amount = 1;
                    if (args.length > 3) {
                        try {
                            amount = Integer.parseInt(args[3]);
                        } catch (Exception ignored) {
                            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "无效的数量");
                            return false;
                        }
                    }

                    OfflinePlayer offlinePlayer = null;
                    if (!args[1].equalsIgnoreCase("all") && !args[1].equalsIgnoreCase("alloffline")) {
                        offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                        if (offlinePlayer == null || (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline())) { // 检查玩家是否在线，因为"hasPlayedBefore"直到他们断开连接才起作用？
                            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "玩家 " + args[1] + " 未找到");
                            return false;
                        }
                    }

                    crateType = args[2];

                    crate = cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase());
                    if (crate == null) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "未找到该宝箱");
                        return false;
                    }

                    if (offlinePlayer == null) {
                        if (args[1].equalsIgnoreCase("all")) {
                            crate.giveAll(amount);
                            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + "已给予所有在线玩家宝箱/钥匙");
                        } else if (args[1].equalsIgnoreCase("alloffline")) {
                            /**
                             * TODO 测试这个，并在他们执行 `/crate give` 时提供更好的说明？
                             */
                            crate.giveAllOffline(amount);
                            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + "已给予所有在线和离线玩家宝箱/钥匙");
                        }
                    } else {
                        if (crate.give(offlinePlayer, amount))
                            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + "已给予 " + offlinePlayer.getName() + " 宝箱/钥匙");
                        else
                            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "给予宝箱/钥匙失败");
                    }

                    break;
                case "crate":
                case "keycrate":
                    if (args.length == 1) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "正确用法: /crate crate <类型> [玩家]");
                        return false;
                    }

                    if (args.length == 3) {
                        player = Bukkit.getPlayer(args[2]);
                    } else if (sender instanceof Player) {
                        player = (Player) sender;
                    } else {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "正确用法: /crate crate <类型> [玩家]");
                        return false;
                    }

                    if (player == null) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "玩家 " + args[2] + " 未找到");
                        return false;
                    }

                    try {
                        crateType = args[1];
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "请指定一个有效的宝箱类型");
                        return false;
                    }

                    if (cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase()) == null || !(cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase()) instanceof KeyCrate)) {
                        sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "未找到该类型的钥匙宝箱");
                        return false;
                    }

                    cratesPlus.getCrateHandler().giveCrate(player, crateType);

                    sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + "已给予 " + player.getDisplayName() + ChatColor.RESET + ChatColor.GREEN + " 一个宝箱");
                    break;
            }
        } else {

            // 帮助信息
            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "----- CratePlus v" + cratesPlus.getDescription().getVersion() + " 帮助 -----");
            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate reload " + ChatColor.YELLOW + "重新加载 CratesPlus 配置（实验性）");
//			sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate settings " + ChatColor.YELLOW + "编辑 CratesPlus 设置和宝箱奖励");
            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate create <名称> " + ChatColor.YELLOW + "创建一个新宝箱");
            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate rename <旧名称> <新名称> " + ChatColor.YELLOW + "重命名一个宝箱");
            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate delete <名称> " + ChatColor.YELLOW + "删除一个宝箱");
            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate give <玩家/all> [宝箱] [数量] " + ChatColor.YELLOW + "给予玩家宝箱/钥匙，未指定宝箱则随机");
            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate crate <类型> [玩家] " + ChatColor.YELLOW + "给予玩家一个可放置的宝箱，供管理员使用");
            sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate debug " + ChatColor.YELLOW + "生成调试链接，用于发送服务器和配置信息");
//			sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate claim " + ChatColor.YELLOW + "领取等待您的钥匙");


            //			sender.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate opener <名称/类型> <开启方式> " + ChatColor.YELLOW + "- 更改特定宝箱或宝箱类型的开启方式");
        }

        return true;
    }

    private void doClaim(Player player) {
        if (!cratesPlus.getCrateHandler().hasPendingKeys(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "您目前没有任何钥匙可以领取");
            return;
        }
        GUI gui = new GUI("领取宝箱钥匙");
        Integer i = 0;
        for (Map.Entry<String, Integer> map : cratesPlus.getCrateHandler().getPendingKey(player.getUniqueId()).entrySet()) {
            final String crateName = map.getKey();
            final KeyCrate crate = (KeyCrate) cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
            if (crate == null)
                return; // Crate must have been removed?
            ItemStack keyItem = crate.getKey().getKeyItem(1);
            if (map.getValue() > 1) {
                ItemMeta itemMeta = keyItem.getItemMeta();
                itemMeta.setDisplayName(itemMeta.getDisplayName() + " x" + map.getValue());
                keyItem.setItemMeta(itemMeta);
            }
            gui.setItem(i, keyItem, new GUI.ClickHandler() {
                @Override
                public void doClick(Player player, GUI gui) {
                    cratesPlus.getCrateHandler().claimKey(player.getUniqueId(), crateName);
                    if (cratesPlus.getCrateHandler().hasPendingKeys(player.getUniqueId())) {
                        GUI.ignoreClosing.add(player.getUniqueId());
                        doClaim(player);
                    } else {
                        player.closeInventory();
                    }
                }
            });
            i++;
        }
        gui.open(player);
    }

    private String uploadDebugData(String configLink, String dataLink, String messagesLink, String pluginsLink) {
        String urlStr = "http://mcdebug.xyz/api/v2/submit/?plugin=cratesplus&config=" + configLink + "&data=" + dataLink + "&messages=" + messagesLink + "&plugins=" + pluginsLink + "&bukkitVer=" + cratesPlus.getBukkitVersion();

        HttpURLConnection connection;
        try {
            //Create connection
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
//			connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.flush();
            wr.close();

            //Get Response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            JSONParser jsonParser = new JSONParser();
            JSONObject obj = (JSONObject) jsonParser.parse(rd.readLine());
            return "https://mcdebug.xyz/cratesplus/share/" + obj.get("id") + "|" + "https://mcdebug.xyz/cratesplus/admin/" + obj.get("adminid");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
