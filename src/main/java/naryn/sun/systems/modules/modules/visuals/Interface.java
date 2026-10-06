package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.systems.theme.ClientAppearance;

/**
 * Compatibility facade for existing HUD renderers. Interface is no longer a
 * module; its controls live in the GUI Settings panel.
 */
@Deprecated
public final class Interface {
    private Interface() { }

    public static boolean glassSelected() { return ClientAppearance.isFrost(); }
    public static float glass() { return glassSelected() ? 1.0F : 0.0F; }
    public static float minimalizm() { return 1.0F - glass(); }
    public static boolean showGlass() { return glassSelected(); }
    public static boolean showMinimalizm() { return !glassSelected(); }
}
