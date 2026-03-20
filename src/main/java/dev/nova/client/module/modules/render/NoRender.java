package dev.nova.client.module.modules.render;
import dev.nova.client.module.Category;
import dev.nova.client.module.Module;
import dev.nova.client.module.setting.Setting.*;
public final class NoRender extends Module {
    public final BoolSetting noFire    =register(new BoolSetting("No Fire",    "Hide fire overlay",   true));
    public final BoolSetting noTotem   =register(new BoolSetting("No Totem",   "Hide totem animation",true));
    public final BoolSetting noPumpkin =register(new BoolSetting("No Pumpkin", "Hide pumpkin overlay",true));
    public final BoolSetting noPortal  =register(new BoolSetting("No Portal",  "Hide portal effect",  false));
    public NoRender(){super("No Render","Disables various unwanted overlays",Category.RENDER,-1);}
}
