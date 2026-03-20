package dev.nova.client.module;

import dev.nova.client.module.modules.combat.*;
import dev.nova.client.module.modules.movement.*;
import dev.nova.client.module.modules.render.*;
import dev.nova.client.module.modules.misc.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // ── Combat ────────────────────────────────────────────────────────────
        register(new AutoCrystal());
        register(new CrystalAura());
        register(new AnchorMacro());
        register(new AimAssist());
        register(new TriggerBot());
        register(new Hitboxes());
        register(new AutoTotem());
        register(new AutoDoubleHand());
        register(new AutoInventoryTotem());
        register(new Surround());
        register(new SurroundBreaker());
        register(new HoleFiller());
        register(new BurrowBot());
        register(new CevBreaker());
        register(new PopCounter());
        register(new AntiBot());

        // ── Movement ──────────────────────────────────────────────────────────
        register(new AutoSprint());
        register(new NoFall());
        register(new Freecam());
        register(new Velocity());
        register(new Scaffold());

        // ── Render ────────────────────────────────────────────────────────────
        register(new ESP());
        register(new Tracers());
        register(new CrystalESP());
        register(new HoleESP());
        register(new NameTags());
        register(new NoBounce());
        register(new Fullbright());
        register(new NoRender());

        // ── Misc ──────────────────────────────────────────────────────────────
        register(new AutoEXP());
        register(new NoBreakDelay());
        register(new AntiCrash());
    }

    private void register(Module m) { modules.add(m); }

    public List<Module> getModules()             { return modules; }
    public List<Module> getEnabled()             { return modules.stream().filter(Module::isEnabled).collect(Collectors.toList()); }
    public List<Module> getByCategory(Category c){ return modules.stream().filter(m -> m.getCategory() == c).collect(Collectors.toList()); }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> cls) {
        return (T) modules.stream().filter(cls::isInstance).findFirst().orElse(null);
    }

    public Module get(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
