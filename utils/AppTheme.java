package utils;

import java.awt.*;
import javax.swing.*;

/**
 * Centralized Theme Manager for entire application
 * Ensures consistent UI design across all screens
 */
public class AppTheme {
    
    // Primary Colors
    public static final Color PRIMARY_BLUE = new Color(25, 103, 210);      // #1967d2
    public static final Color PRIMARY_BLUE_LIGHT = new Color(66, 133, 244); // #4285f4
    public static final Color PRIMARY_BLUE_DARK = new Color(15, 75, 180);  // #0f4bb4
    
    // Secondary Colors
    public static final Color SUCCESS_GREEN = new Color(52, 168, 83);      // #34a853
    public static final Color SUCCESS_GREEN_LIGHT = new Color(67, 190, 102);
    public static final Color SUCCESS_GREEN_DARK = new Color(40, 145, 64);
    
    public static final Color ERROR_RED = new Color(244, 67, 54);          // #f44336
    public static final Color ERROR_RED_LIGHT = new Color(255, 100, 87);
    public static final Color ERROR_RED_DARK = new Color(211, 47, 47);
    
    public static final Color WARNING_ORANGE = new Color(255, 152, 0);     // #ff9800
    public static final Color WARNING_ORANGE_LIGHT = new Color(255, 175, 50);
    public static final Color WARNING_ORANGE_DARK = new Color(230, 124, 0);
    
    // Neutral Colors
    public static final Color TEXT_DARK = new Color(33, 33, 33);           // #212121
    public static final Color TEXT_MEDIUM = new Color(66, 66, 66);         // #424242
    public static final Color TEXT_LIGHT = new Color(117, 117, 117);       // #757575
    
    public static final Color BG_LIGHT = new Color(245, 245, 245);         // #f5f5f5
    public static final Color BG_PANEL = new Color(252, 253, 255);         // #fcfdff
    public static final Color BG_WHITE = Color.WHITE;
    
    public static final Color BORDER_GRAY = new Color(200, 200, 200);      // #c8c8c8
    public static final Color BORDER_LIGHT = new Color(230, 230, 230);     // #e6e6e6
    public static final Color BORDER_LIGHTER = new Color(240, 240, 240);   // #f0f0f0
    
    public static final Color SELECTION_BG = new Color(230, 240, 255);     // #e6f0ff
    
    // Fonts
    public static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_HEADER_LARGE = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font FONT_HEADER_MEDIUM = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font FONT_HEADER_SMALL = new Font(FONT_FAMILY, Font.BOLD, 14);
    
    public static final Font FONT_LABEL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_LABEL_REGULAR = new Font(FONT_FAMILY, Font.PLAIN, 12);
    
    public static final Font FONT_BUTTON = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_TABLE_HEADER = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_TABLE_CELL = new Font(FONT_FAMILY, Font.PLAIN, 11);
    
    public static final Font FONT_INPUT = new Font(FONT_FAMILY, Font.PLAIN, 11);
    
    // Sizes
    public static final int BUTTON_WIDTH = 130;
    public static final int BUTTON_HEIGHT = 36;
    public static final int BUTTON_SMALL_WIDTH = 110;
    public static final int BUTTON_SMALL_HEIGHT = 32;
    
    public static final int PADDING_LARGE = 15;
    public static final int PADDING_MEDIUM = 12;
    public static final int PADDING_SMALL = 8;
    
    public static final int BORDER_RADIUS = 8;
    public static final int BORDER_WIDTH = 1;
    
    public static final int HEADER_HEIGHT = 70;
    public static final int TAB_HEIGHT = 28;
    
    // Apply consistent Look & Feel
    public static void initializeTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Increase default fonts
            Font defaultFont = new Font(FONT_FAMILY, Font.PLAIN, 11);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof Font) {
                    UIManager.put(key, defaultFont);
                }
            }
        } catch (Exception ignored) {}
    }
}
