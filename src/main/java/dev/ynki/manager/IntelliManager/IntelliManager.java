package dev.ynki.manager.IntelliManager;

import net.minecraft.client.MinecraftClient;
import dev.ynki.manager.IMinecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IntelliManager implements IMinecraft {

    private final File file = new File(MinecraftClient.getInstance().runDirectory, "files/smart.ew");

    private final List<String> behaviors = new ArrayList<>();
    private String activeBehavior;

    public void init() {
        behaviors.clear();

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    behaviors.add(line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!behaviors.isEmpty()) {
            activeBehavior = behaviors.get(0);
        }
    }

    public void saveBehaviors() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (String behavior : behaviors) {
                    writer.write(behavior);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBehavior(String behavior) {
        if (behavior != null && !behavior.trim().isEmpty() && !behaviors.contains(behavior)) {
            behaviors.add(behavior.trim());
            saveBehaviors();
        }
    }

    public void removeBehavior(String behavior) {
        if (behaviors.remove(behavior)) {
            if (activeBehavior != null && activeBehavior.equals(behavior)) {
                activeBehavior = behaviors.isEmpty() ? null : behaviors.get(0);
            }
            saveBehaviors();
        }
    }

    public List<String> getBehaviors() {
        return new ArrayList<>(behaviors);
    }

    public void setActiveBehavior(String behavior) {
        if (behaviors.contains(behavior)) {
            this.activeBehavior = behavior;
        }
    }

    public String getActiveBehavior() {
        return activeBehavior;
    }
}