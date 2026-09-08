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
     *
     * @param label  display label shown in the dropdown
     * @param action action to run when this item is activated; may be null
     */
    public MenuItem(String label, Runnable action) {
        this(label, label.isEmpty() ? '\0' : Character.toLowerCase(label.charAt(0)), action);
    }

    /**
     * Creates a menu item with an explicit mnemonic character.
     *
     * @param label     display label shown in the dropdown
     * @param mnemonic  mnemonic character for keyboard activation (Alt+mnemonic)
     * @param action    action to run when this item is activated; may be null
     */
    public MenuItem(String label, char mnemonic, Runnable action) {
        this.label = label;
        this.mnemonic = mnemonic;
        this.action = action;
    }

    /**
     * Returns the display label shown in the dropdown.
     *
     * @return the display label
     */
    public String getLabel() { return label; }

    /**
     * Returns the mnemonic character for keyboard activation. Case-insensitive —
     * the user presses Alt+mnemonic to activate this item.
     *
     * @return the mnemonic character
     */
    public char getMnemonic() { return mnemonic; }

    /**
     * Returns the action to run when this item is activated.
     *
     * @return the action; may be null
     */
    public Runnable getAction() { return action; }

    /** Fires the action if one is set. */
    public void activate() {
        if (action != null) action.run();
    }
}