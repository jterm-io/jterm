package io.jterm.window;

import io.jterm.core.TerminalSize;
import io.jterm.screen.DefaultScreen;
import io.jterm.widget.Button;
import io.jterm.widget.Label;
import io.jterm.layout.LinearLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowImplTest {
    @Test
    void windowContentsPreferredSize() {
        var window = new WindowImpl("Test");
        window.getContents().setLayoutManager(new LinearLayout(LinearLayout.Direction.VERTICAL));
        window.getContents().addComponent(new Label("Hello"));
        var ps = window.getContents().getPreferredSize();
        assertTrue(ps.columns() >= 5);
    }
}
