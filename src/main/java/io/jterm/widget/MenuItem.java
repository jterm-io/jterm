package io.jterm.widget;

/**
 * A selectable item in a {@link Menu}. Has a label, an optional mnemonic
 * character (for keyboard activation), and a Runnable action that fires on
 * activation (Enter key or click).
 */
public class MenuItem {
    private final String label;
    private final char mnemonic;
    private final Runnable action;

    /**
     * Creates a menu item with the given label and action. The mnemonic is
     * auto-detected from the label: the first character is used.
     */
    public MenuItem(String label, Runnable action) {
        this(label, label.isEmpty() ? '\0' : Character.toLowerCase(label.charAt(0)), action);
    }

    /**
     * Creates a menu item with an explicit mnemonic character.
     */
    public MenuItem(String label, char mnemonic, Runnable action) {
        this.label = label;
        this.mnemonic = mnemonic;
        this.action = action;
    }

    /** Display label shown in the dropdown. */
    public String getLabel() { return label; }

    /**
     * Mnemonic character for keyboard activation. Case-insensitive —
     * the user presses Alt+mnemonic to activate this item.
     */
    public char getMnemonic() { return mnemonic; }

    /** The action to run when this item is activated. May be null. */
    public Runnable getAction() { return action; }

    /** Fires the action if one is set. */
    public void activate() {
        if (action != null) action.run();
    }
}