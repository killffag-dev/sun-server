package naryn.sun.systems.waypoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.game.MessageUtility;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class WayPointsManager {
    private final List<Waypoint> waypoints = new CopyOnWriteArrayList<>();
    private final File storageFile;

    public WayPointsManager() {
        this.storageFile = new File(FileManager.DIRECTORY, "waypoints.json");
    }

    public synchronized void load() {
        if (!this.storageFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(this.storageFile)) {
            JsonObject root = FileManager.GSON.fromJson(reader, JsonObject.class);
            if (root != null && root.has("waypoints")) {
                this.waypoints.clear();
                JsonArray array = root.getAsJsonArray("waypoints");
                for (JsonElement element : array) {
                    if (element.isJsonObject()) {
                        Waypoint wp = Waypoint.fromJson(element.getAsJsonObject());
                        if (wp != null) {
                            this.waypoints.add(wp);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading waypoints: " + e.getMessage());
        }
    }

    public synchronized void save() {
        try {
            if (!this.storageFile.getParentFile().exists()) {
                this.storageFile.getParentFile().mkdirs();
            }
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (Waypoint wp : this.waypoints) {
                array.add(wp.toJson());
            }
            root.add("waypoints", array);
            try (FileWriter writer = new FileWriter(this.storageFile)) {
                FileManager.GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            System.err.println("Error saving waypoints: " + e.getMessage());
        }
    }

    public boolean add(Waypoint waypoint) {
        if (this.contains(waypoint.getName())) {
            MessageUtility.error(Text.of(Localizator.translate("modules.waypoints.exists", waypoint.getName())));
            return false;
        }
        this.waypoints.add(waypoint);
        this.save();
        MessageUtility.info(Text.of(Localizator.translate(
            "modules.waypoints.added",
            waypoint.getName(),
            (int) waypoint.getX(),
            (int) waypoint.getY(),
            (int) waypoint.getZ()
        )));
        return true;
    }

    public boolean add(String name, double x, double y, double z, String dimension, ColorRGBA color) {
        Waypoint wp = new Waypoint(name, x, y, z, dimension, null, color);
        return this.add(wp);
    }

    public void add(String name, int x, int y, int z) {
        this.add(name, (double) x, (double) y, (double) z, "minecraft:overworld", new ColorRGBA(255, 120, 60, 255));
    }

    public boolean del(String name) {
        Waypoint target = this.get(name);
        if (target != null && this.waypoints.remove(target)) {
            this.save();
            MessageUtility.info(Text.of(Localizator.translate("modules.waypoints.deleted", name)));
            return true;
        } else {
            MessageUtility.info(Text.of(Localizator.translate("modules.waypoints.not_found", name)));
            return false;
        }
    }

    public void clear() {
        this.waypoints.clear();
        this.save();
        MessageUtility.info(Text.of(Localizator.translate("modules.waypoints.cleared")));
    }

    public boolean contains(String name) {
        return this.get(name) != null;
    }

    public Waypoint get(String name) {
        if (name == null) return null;
        for (Waypoint wp : this.waypoints) {
            if (wp.getName().equalsIgnoreCase(name.trim())) {
                return wp;
            }
        }
        return null;
    }

    public List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(this.waypoints);
    }

    public Set<Entry<String, Vec3d>> getEntries() {
        Set<Entry<String, Vec3d>> entries = new HashSet<>();
        for (Waypoint wp : this.waypoints) {
            entries.add(new AbstractMap.SimpleEntry<>(wp.getName(), wp.getPos()));
        }
        return entries;
    }

    public String generateDefaultName() {
        int index = 1;
        while (this.contains("Waypoint " + index)) {
            index++;
        }
        return "Waypoint " + index;
    }
}
