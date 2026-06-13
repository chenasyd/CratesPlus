package plus.crates.Handlers.Holograms;

import org.bukkit.Location;
import plus.crates.Crates.Crate;

import java.util.ArrayList;

public class FallbackHologram implements Hologram {

    public void create(Location location, Crate crate, ArrayList<String> lines) {
        crate.getCratesPlus().getLogger().warning("调用了全息显示 #create 但未加载任何全息插件！");
    }

    public void remove(Location location, Crate crate) {
        crate.getCratesPlus().getLogger().warning("调用了全息显示 #remove 但未加载任何全息插件！");
    }

}
