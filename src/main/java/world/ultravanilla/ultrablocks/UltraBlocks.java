package world.ultravanilla.ultrablocks;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class UltraBlocks extends JavaPlugin implements Listener, TabExecutor {

    private final File storeFile = new File(getDataFolder(), "store.yml");
    private final ChatColor COLOR = ChatColor.of("#dad4db");
    private final ChatColor COLOR_GOOD = ChatColor.of("#58ddbe");
    private final ChatColor COLOR_BAD = ChatColor.of("#dd6158");
    private YamlConfiguration store;

    @Override
    public void onEnable() {
        // Plugin startup logic

        ConfigurationSerialization.registerClass(UltraBlock.class, "UltraBlock");
        init("config.yml", false);
        reloadConfig();

        store = new YamlConfiguration();
        getDataFolder().mkdir();
        if (!storeFile.exists()) {
            try {
                storeFile.createNewFile();
            } catch (IOException exception) {
                System.err.println("Couldn't create storage file.");
                exception.printStackTrace();
            }
        }
        loadStore();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ultrablock").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void loadStore() {
        try {
            store.load(storeFile);
        } catch (IOException | InvalidConfigurationException exception) {
            System.err.println("Couldn't load the storage file.");
            exception.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("delete")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Block block = player.getTargetBlock(3);
                    if (block != null) {
                        if (removeBlock(block.getLocation())) {
                            player.sendMessage(String.format("%sDeleted %sUltraBlock%s.", COLOR, COLOR_GOOD, COLOR));
                        } else {
                            player.sendMessage(COLOR_BAD + "You are not looking at an UltraBlock.");
                        }
                    } else {
                        player.sendMessage(COLOR_BAD + "You are not looking at a valid block.");
                    }
                } else {
                    sender.sendMessage("You must be a player to execute this command!");
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadStore();
                sender.sendMessage(COLOR + "Reloaded configs.");
            } else {
                return false;
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete")) {
                if (removeBlock(args[1])) {
                    sender.sendMessage(String.format("%sDeleted %sUltraBlock %s'%s'%s.", COLOR, COLOR_GOOD, COLOR_GOOD, args[1], COLOR));
                } else {
                    sender.sendMessage(COLOR_BAD + "You are not looking at an UltraBlock.");
                }
            } else {
                return false;
            }
        } else if (args.length >= 5) {
            if (args[0].equalsIgnoreCase("create")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Block block = player.getTargetBlock(3);
                    if (block != null) {
                        Location location = block.getLocation();
                        if (isBlock(block)) {
                            String id = args[1];
                            if (getBlock(id) != null) {
                                player.sendMessage(COLOR_BAD + "An UltraBlock with that id already exists!");
                                return true;
                            }
                            SenderType type = SenderType.valueOf(args[2].toUpperCase());
                            Hand hand = Hand.valueOf(args[3].toUpperCase());
                            StringBuilder cmd = new StringBuilder();
                            for (int i = 4; i < args.length; i++) {
                                cmd.append(args[i]).append(" ");
                            }
                            String cmdString = cmd.toString();
                            cmdString = cmdString.trim();
                            createBlock(id, location, cmdString, type, hand);
                            player.sendMessage(String.format(
                                    "%sCreated an %sUltraBlock %swith id %s'%s' %sat %s[%d, %d, %d]%s.",
                                    COLOR, COLOR_GOOD, COLOR,
                                    COLOR_GOOD, id,
                                    COLOR,
                                    COLOR_GOOD,
                                    (int) location.getX(),
                                    (int) location.getY(),
                                    (int) location.getZ(),
                                    COLOR
                            ));
                        } else {
                            player.sendMessage(COLOR_BAD + "You are not looking at a valid UltraBlock material.");
                        }
                    } else {
                        player.sendMessage(COLOR_BAD + "You are not looking at a valid block.");
                    }
                } else {
                    sender.sendMessage("You must be a player to execute this command!");
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        var suggestions = new ArrayList<String>();
        if (args.length == 1) {
            suggestions.add("create");
            suggestions.add("delete");
            suggestions.add("reload");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete")) {
                for (var block : getBlockList()) {
                    suggestions.add(block.getId());
                }
            }
        }
        return getSuggestions(suggestions, args);
    }

    public boolean isBlock(Block block) {
        for (String string : getConfig().getStringList("enabled_materials")) {
            if (block.getState().getType() == Material.valueOf(string.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && isBlock(block)) {
                Hand hand = Hand.ANY;
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    hand = Hand.RIGHT;
                } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    hand = Hand.LEFT;
                }
                UltraBlock ultraBlock = getBlock(block.getLocation());
                if (ultraBlock != null) {
                    if (ultraBlock.getHand() == Hand.ANY || ultraBlock.getHand() == hand) {
                        ultraBlock.onActivate(event.getPlayer());
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    public void createBlock(String id, Location location, String command, SenderType type, Hand hand) {
        var list = getBlockList();
        list.add(new UltraBlock(id, location, command, type, hand));
        store.set("blocks", list);
        save();
    }

    public List<UltraBlock> getBlockList() {
        var list = (List<UltraBlock>) store.getList("blocks");
        if (list == null) {
            list = new ArrayList<>();
            store.set("blocks", list);
            save();
        }
        return list;
    }

    public boolean removeBlock(String id) {
        var list = getBlockList();
        for (var ultraBlock : list) {
            if (ultraBlock.getId().equals(id)) {
                list.remove(ultraBlock);
                return true;
            }
        }
        return false;
    }

    public UltraBlock getBlock(String id) {
        for (var ultraBlock : getBlockList()) {
            if (ultraBlock.getId().equals(id)) {
                return (ultraBlock);
            }
        }
        return null;
    }

    public UltraBlock getBlock(Location location) {
        for (var ultraBlock : getBlockList()) {
            if (ultraBlock.getLocation().equals(location)) {
                return (ultraBlock);
            }
        }
        return null;
    }

    public boolean removeBlock(Location location) {
        var list = getBlockList();
        for (var ultraBlock : list) {
            if (ultraBlock.getLocation().equals(location)) {
                list.remove(ultraBlock);
                return true;
            }
        }
        return false;
    }

    public void save() {
        try {
            store.save(storeFile);
        } catch (IOException exception) {
            System.err.println("Couldn't save storage file.");
            exception.printStackTrace();
        }
    }

    private List<String> getSuggestions(List<String> suggestions, String[] args) {
        List<String> realSuggestions = new ArrayList<>();
        for (String s : suggestions) {
            if (args[args.length - 1].length() < args.length || s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                realSuggestions.add(s);
            }
        }
        return realSuggestions;
    }

    private boolean hasFlag(String[] args, String arg) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private String getArgFor(String[] args, String arg) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(arg)) {
                if (i + 1 < args.length) {
                    return args[i + 1];
                }
            }
        }
        return null;
    }

    private void init(String name, boolean overwrite) {
        File file = new File(this.getDataFolder(), name);
        if (!file.exists() || overwrite) {
            InputStream fis = getClass().getResourceAsStream("/" + name);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                int i;
                while ((i = fis.read(buf)) != -1) {
                    fos.write(buf, 0, i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
