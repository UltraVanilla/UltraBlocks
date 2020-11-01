package world.ultravanilla.ultrablocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class UltraBlock implements ConfigurationSerializable {

    private String id;
    private Location location;
    private String command;
    private Hand hand;
    private SenderType type;

    public UltraBlock(String id, Location location, String command, SenderType type, Hand hand) {
        this.id = id;
        this.location = location;
        this.command = command;
        this.type = type;
        this.hand = hand;
    }

    public static UltraBlock deserialize(Map<String, Object> args) {
        return new UltraBlock(
                args.get("id").toString(),
                (Location) args.get("location"),
                args.get("command").toString(),
                SenderType.valueOf(args.get("type").toString().toUpperCase()),
                Hand.valueOf(args.get("hand").toString().toUpperCase())
        );
    }

    public Hand getHand() {
        return hand;
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public SenderType getType() {
        return type;
    }

    public void setType(SenderType type) {
        this.type = type;
    }

    @Override
    public Map<String, Object> serialize() {
        var map = new HashMap<String, Object>();
        map.put("id", id);
        map.put("location", location);
        map.put("command", command);
        map.put("type", type.toString());
        map.put("hand", hand.toString());
        return map;
    }

    public void onActivate(Player player) {
        if (type == SenderType.SELF) {
            player.performCommand(command);
        } else if (type == SenderType.CONSOLE) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                    .replaceAll("@p", player.getName()
                            //TODO: More selectors etc
                    ));
        }
    }
}
