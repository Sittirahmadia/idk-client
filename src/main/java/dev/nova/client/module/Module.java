package dev.nova.client.module;

import dev.nova.client.NovaClient;
import dev.nova.client.module.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String   name;
    private final String   description;
    private final Category category;
    private       int      key;
    private       boolean  enabled;
    private       boolean  visible;

    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, int key) {
        this.name        = name;
        this.description = description;
        this.category    = category;
        this.key         = key;
        this.enabled     = false;
        this.visible     = true;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void onEnable() {
        NovaClient.INSTANCE.eventBus.subscribe(this);
    }

    public void onDisable() {
        NovaClient.INSTANCE.eventBus.unsubscribe(this);
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable();
        else         onDisable();
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    protected <T extends Setting<?>> T register(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() { return settings; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String   getName()        { return name; }
    public String   getDescription() { return description; }
    public Category getCategory()    { return category; }
    public int      getKey()         { return key; }
    public boolean  isEnabled()      { return enabled; }
    public boolean  isVisible()      { return visible; }

    public void setKey(int key)           { this.key = key; }
    public void setEnabled(boolean v)     { if (v != enabled) toggle(); }
    public void setVisible(boolean v)     { this.visible = v; }
}
