package dev.nova.client;

import dev.nova.client.event.EventBus;
import dev.nova.client.gui.NovaClickGUI;
import dev.nova.client.gui.HudRenderer;
import dev.nova.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaClient implements ClientModInitializer {

    public static final String NAME    = "Nova";
    public static final String VERSION = "1.0.0";
    public static final Logger LOG     = LoggerFactory.getLogger(NAME);

    public static NovaClient INSTANCE;
    public static MinecraftClient MC = MinecraftClient.getInstance();

    public EventBus     eventBus;
    public ModuleManager moduleManager;
    public NovaClickGUI clickGui;
    public HudRenderer  hudRenderer;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        eventBus      = new EventBus();
        moduleManager = new ModuleManager();
        clickGui      = new NovaClickGUI();
        hudRenderer   = new HudRenderer();

        LOG.info("[Nova] Client loaded — {} modules registered",
                moduleManager.getModules().size());
    }
}
