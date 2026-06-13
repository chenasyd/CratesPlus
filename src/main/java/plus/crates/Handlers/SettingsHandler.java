package plus.crates.Handlers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plus.crates.Crates.Crate;
import plus.crates.Crates.Winning;
import plus.crates.CratesPlus;
import plus.crates.Events.PlayerInputEvent;
import plus.crates.Utils.GUI;
import plus.crates.Utils.LegacyMaterial;
import plus.crates.Utils.ReflectionUtil;
import plus.crates.Utils.SignInputHandler;

import java.lang.reflect.Constructor;
import java.util.*;

public class SettingsHandler implements Listener {
    private HashMap<UUID, String> renaming = new HashMap<>();
    private CratesPlus cratesPlus;
    private GUI settings;
    private GUI crates;
    private HashMap<String, String> lastCrateEditing = new HashMap<>();

    public SettingsHandler(CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
        Bukkit.getPluginManager().registerEvents(this, cratesPlus);
        setupSettingsInventory();
        setupCratesInventory();
    }

    public void setupSettingsInventory() {
        settings = new GUI("CratesPlus 设置");

        ItemStack itemStack;
        ItemMeta itemMeta;
        List<String> lore;

        itemStack = new ItemStack(Material.CHEST);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GREEN + "编辑宝箱");
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        settings.setItem(1, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrates(player);
            }
        });

        Material material;
        try {
            material = Material.valueOf("BARRIER");
        } catch (Exception i) {
            material = LegacyMaterial.REDSTONE_TORCH_ON.getMaterial();
        }

        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GREEN + "重新加载配置");
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        settings.setItem(5, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                cratesPlus.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "配置已重新加载");
            }
        });
    }

    public void setupCratesInventory() {
        crates = new GUI("宝箱列表");

        ItemStack itemStack;
        ItemMeta itemMeta;

        for (Map.Entry<String, Crate> entry : cratesPlus.getConfigHandler().getCrates().entrySet()) {
            Crate crate = entry.getValue();

            itemStack = new ItemStack(Material.CHEST);
            itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(crate.getName(true));
            itemStack.setItemMeta(itemMeta);
            final String crateName = crate.getName();
            crates.addItem(itemStack, new GUI.ClickHandler() {
                @Override
                public void doClick(Player player, GUI gui) {
                    GUI.ignoreClosing.add(player.getUniqueId());
                    openCrate(player, crateName);
                }
            });
        }
    }

    public void openSettings(final Player player) {
        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> settings.open(player), 1L);
    }

    public void openCrates(final Player player) {
        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> crates.open(player), 1L);
    }

    public void openCrateWinnings(final Player player, String crateName) {
        Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
        if (crate == null) {
            player.sendMessage(ChatColor.RED + "无法找到 " + crateName + " 宝箱");
            return;
        }

        if (crate.containsCommandItem()) {
            player.sendMessage(ChatColor.RED + "您当前无法在 GUI 中编辑含有指令物品的宝箱");
            player.closeInventory();
            return;
        }

        final GUI gui = new GUI("编辑 " + crate.getName(false) + " 宝箱奖励");

        for (Winning winning : crate.getWinnings()) {
            gui.addItem(winning.getWinningItemStack());
        }

        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> gui.open(player), 1L);

    }

    public void openCrate(final Player player, final String crateName) {
        Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
        if (crate == null) {
            return; // TODO Error handling here
        }

        final GUI gui = new GUI("编辑 " + crate.getName(false) + " 宝箱");

        ItemStack itemStack;
        ItemMeta itemMeta;
        List<String> lore;


        // 重命名宝箱

        itemStack = new ItemStack(Material.NAME_TAG);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GREEN + "重命名宝箱");
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        gui.setItem(0, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                renaming.put(player.getUniqueId(), crateName);
                try {
                    // 发送虚假牌子（1.13+）
                    player.sendBlockChange(player.getLocation(), Material.valueOf("SIGN"), (byte) 0);

                    Constructor signConstructor = ReflectionUtil.getNMSClass("PacketPlayOutOpenSignEditor").getConstructor(ReflectionUtil.getNMSClass("BlockPosition"));
                    Object packet = signConstructor.newInstance(ReflectionUtil.getBlockPosition(player));
                    SignInputHandler.injectNetty(player);
                    ReflectionUtil.sendPacket(player, packet);

                    player.sendBlockChange(player.getLocation(), player.getLocation().getBlock().getType(), player.getLocation().getBlock().getData());
                } catch (Exception e) {
                    player.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "请使用 /crate rename <旧名称> <新名称>");
                    renaming.remove(player.getUniqueId());
                }
            }
        });


        // 编辑宝箱奖励

        itemStack = new ItemStack(Material.DIAMOND);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RED + "编辑宝箱奖励");
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        gui.setItem(2, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.sendMessage(ChatColor.RED + "此功能目前已被禁用！");
//                GUI.ignoreClosing.add(player.getUniqueId());
//                openCrateWinnings(player, crateName);
            }
        });


        // 编辑宝箱颜色

        itemStack = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 3);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GREEN + "编辑宝箱颜色");
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        gui.setItem(4, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrateColor(player, crate);
            }
        });


        // 删除宝箱

        Material material;

        try {
            material = Material.valueOf("BARRIER");
        } catch (Exception i) {
            material = LegacyMaterial.REDSTONE_TORCH_ON.getMaterial();
        }

        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GREEN + "删除宝箱");
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        gui.setItem(6, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                confirmDelete(player, crate);
            }
        });

        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> gui.open(player), 1L);

    }

    private void openCrateColor(final Player player, final Crate crate) {
        GUI gui = new GUI("编辑宝箱颜色");

        ItemStack aqua = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 3);
        ItemMeta aquaMeta = aqua.getItemMeta();
        aquaMeta.setDisplayName(ChatColor.AQUA + "天蓝");
        aqua.setItemMeta(aquaMeta);
        gui.addItem(aqua, getColorClickHandler(crate, ChatColor.AQUA));

        ItemStack black = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 15);
        ItemMeta blackMeta = black.getItemMeta();
        blackMeta.setDisplayName(ChatColor.BLACK + "黑色");
        black.setItemMeta(blackMeta);
        gui.addItem(black, getColorClickHandler(crate, ChatColor.BLACK));

        ItemStack blue = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 9);
        ItemMeta blueMeta = blue.getItemMeta();
        blueMeta.setDisplayName(ChatColor.BLUE + "蓝色");
        blue.setItemMeta(blueMeta);
        gui.addItem(blue, getColorClickHandler(crate, ChatColor.BLUE));

        ItemStack darkAqua = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 3);
        ItemMeta darkAquaMeta = darkAqua.getItemMeta();
        darkAquaMeta.setDisplayName(ChatColor.DARK_AQUA + "深青");
        darkAqua.setItemMeta(darkAquaMeta);
        gui.addItem(darkAqua, getColorClickHandler(crate, ChatColor.DARK_AQUA));

        ItemStack darkBlue = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 11);
        ItemMeta darkBlueMeta = darkBlue.getItemMeta();
        darkBlueMeta.setDisplayName(ChatColor.DARK_BLUE + "深蓝");
        darkBlue.setItemMeta(darkBlueMeta);
        gui.addItem(darkBlue, getColorClickHandler(crate, ChatColor.DARK_BLUE));

        ItemStack darkGray = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 7);
        ItemMeta darkGrayMeta = darkGray.getItemMeta();
        darkGrayMeta.setDisplayName(ChatColor.DARK_GRAY + "深灰");
        darkGray.setItemMeta(darkGrayMeta);
        gui.addItem(darkGray, getColorClickHandler(crate, ChatColor.DARK_GRAY));

        ItemStack darkGreen = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 13);
        ItemMeta darkGreenMeta = darkGreen.getItemMeta();
        darkGreenMeta.setDisplayName(ChatColor.DARK_GREEN + "深绿");
        darkGreen.setItemMeta(darkGreenMeta);
        gui.addItem(darkGreen, getColorClickHandler(crate, ChatColor.DARK_GREEN));

        ItemStack darkPurple = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 10);
        ItemMeta darkPurpleMeta = darkPurple.getItemMeta();
        darkPurpleMeta.setDisplayName(ChatColor.DARK_PURPLE + "深紫");
        darkPurple.setItemMeta(darkPurpleMeta);
        gui.addItem(darkPurple, getColorClickHandler(crate, ChatColor.DARK_PURPLE));

        ItemStack darkRed = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 14);
        ItemMeta darkRedMeta = darkRed.getItemMeta();
        darkRedMeta.setDisplayName(ChatColor.DARK_RED + "深红");
        darkRed.setItemMeta(darkRedMeta);
        gui.addItem(darkRed, getColorClickHandler(crate, ChatColor.DARK_RED));

        ItemStack gold = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 1);
        ItemMeta goldMeta = gold.getItemMeta();
        goldMeta.setDisplayName(ChatColor.GOLD + "金色");
        gold.setItemMeta(goldMeta);
        gui.addItem(gold, getColorClickHandler(crate, ChatColor.GOLD));

        ItemStack gray = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 8);
        ItemMeta grayMeta = gray.getItemMeta();
        grayMeta.setDisplayName(ChatColor.GRAY + "灰色");
        gray.setItemMeta(grayMeta);
        gui.addItem(gray, getColorClickHandler(crate, ChatColor.GRAY));

        ItemStack green = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 5);
        ItemMeta greenMeta = gray.getItemMeta();
        greenMeta.setDisplayName(ChatColor.GREEN + "绿色");
        green.setItemMeta(greenMeta);
        gui.addItem(green, getColorClickHandler(crate, ChatColor.GREEN));

        ItemStack lightPurple = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 2);
        ItemMeta lightPurpleMeta = lightPurple.getItemMeta();
        lightPurpleMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "浅紫");
        lightPurple.setItemMeta(lightPurpleMeta);
        gui.addItem(lightPurple, getColorClickHandler(crate, ChatColor.LIGHT_PURPLE));

        ItemStack red = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 14);
        ItemMeta redMeta = red.getItemMeta();
        redMeta.setDisplayName(ChatColor.RED + "红色");
        red.setItemMeta(redMeta);
        gui.addItem(red, getColorClickHandler(crate, ChatColor.RED));

        ItemStack white = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 0);
        ItemMeta whiteMeta = white.getItemMeta();
        whiteMeta.setDisplayName(ChatColor.WHITE + "白色");
        white.setItemMeta(whiteMeta);
        gui.addItem(white, getColorClickHandler(crate, ChatColor.WHITE));

        ItemStack yellow = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 4);
        ItemMeta yellowMeta = yellow.getItemMeta();
        yellowMeta.setDisplayName(ChatColor.YELLOW + "黄色");
        yellow.setItemMeta(yellowMeta);
        gui.addItem(yellow, getColorClickHandler(crate, ChatColor.YELLOW));

        gui.open(player);
    }

    private GUI.ClickHandler getColorClickHandler(Crate crate, ChatColor color) {
        return new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                crate.setColor(color);
                player.sendMessage("颜色已设置为 " + color + color.name());
                openCrate(player, crate.getName());
            }
        };
    }

    private void confirmDelete(final Player player, final Crate crate) {
        final GUI gui = new GUI("确认删除 \"" + crate.getName(false) + "\"");

        ItemStack crateItem = new ItemStack(crate.getBlock(), 1, (short) crate.getBlockData());
        ItemMeta crateMeta = crateItem.getItemMeta();
        crateMeta.setDisplayName(crate.getName());
        crateItem.setItemMeta(crateMeta);
        gui.setItem(3, crateItem);

        ItemStack cancel = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 14);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "取消");
        cancel.setItemMeta(cancelMeta);
        gui.setItem(16, cancel, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrate(player, crate.getName(false));
            }
        });

        ItemStack confirm = new ItemStack(LegacyMaterial.WOOL.getMaterial(), 1, (short) 5);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "确认");
        confirm.setItemMeta(confirmMeta);
        gui.setItem(18, confirm, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                player.sendMessage("即将删除");
            }
        });

        gui.open(player);
    }

    public HashMap<String, String> getLastCrateEditing() {
        return lastCrateEditing;
    }

    @EventHandler
    public void onPlayerInput(final PlayerInputEvent event) {
        if (renaming.containsKey(event.getPlayer().getUniqueId())) {
            String name = renaming.get(event.getPlayer().getUniqueId());
            renaming.remove(event.getPlayer().getUniqueId());
            String newName = "";
            for (String line : event.getLines()) {
                newName += line;
            }
            if (!name.isEmpty() && !newName.isEmpty())
                Bukkit.dispatchCommand(event.getPlayer(), "crate rename " + name + " " + newName);
            cratesPlus.getSettingsHandler().openCrate(event.getPlayer(), newName);
        } else if (cratesPlus.isCreating(event.getPlayer().getUniqueId())) {
            cratesPlus.removeCreating(event.getPlayer().getUniqueId());
            String name = "";
            for (String line : event.getLines()) {
                name += line;
            }
            if (!name.isEmpty()) {
                final String finalName = name;
                Bukkit.getScheduler().runTask(cratesPlus, () -> Bukkit.dispatchCommand(event.getPlayer(), "crate create " + finalName));
            }
        }
    }

}
