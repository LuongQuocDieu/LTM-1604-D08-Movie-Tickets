package utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom message dialog with modern styling
 */
public class MessageDialog {
    
    public static void showInfo(Component parent, String message) {
        showCustomDialog(parent, message, "Thông Báo", AppTheme.PRIMARY_BLUE, "ℹ");
    }
    
    public static void showSuccess(Component parent, String message) {
        showCustomDialog(parent, message, "Thành Công", AppTheme.SUCCESS_GREEN, "✓");
    }
    
    public static void showError(Component parent, String message) {
        showCustomDialog(parent, message, "Lỗi", AppTheme.ERROR_RED, "✕");
    }
    
    public static void showWarning(Component parent, String message) {
        showCustomDialog(parent, message, "Cảnh Báo", new Color(255, 152, 0), "!");
    }
    
    private static void showCustomDialog(Component parent, String message, String title, Color iconColor, String iconChar) {
        JDialog dialog = new JDialog((Frame) null, title, true);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(450, 180);
        dialog.setLocationRelativeTo(parent);
        
        // Main panel with rounded corners
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);
                
                // Draw background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                
                // Draw border
                g2.setColor(new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
            }
        };
        mainPanel.setLayout(null);
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setOpaque(false);
        
        // Icon panel
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw circle
                g2.setColor(iconColor);
                g2.fillOval(5, 5, 70, 70);
                
                // Draw icon text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
                FontMetrics fm = g2.getFontMetrics();
                String icon = iconChar;
                int x = (80 - fm.stringWidth(icon)) / 2;
                int y = ((80 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(icon, x, y);
            }
        };
        iconPanel.setBackground(Color.WHITE);
        iconPanel.setOpaque(true);
        iconPanel.setBounds(20, 20, 80, 80);
        
        // Message label
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(AppTheme.FONT_LABEL_REGULAR);
        msgLabel.setForeground(new Color(50, 50, 50));
        msgLabel.setBounds(120, 30, 310, 60);
        msgLabel.setVerticalAlignment(SwingConstants.TOP);
        
        // OK button
        JButton btnOK = new JButton("OK");
        btnOK.setFont(AppTheme.FONT_BUTTON);
        btnOK.setBackground(iconColor);
        btnOK.setForeground(Color.WHITE);
        btnOK.setBorderPainted(false);
        btnOK.setFocusPainted(false);
        btnOK.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOK.setBounds(350, 130, 80, 35);
        btnOK.addActionListener(e -> dialog.dispose());
        
        mainPanel.add(iconPanel);
        mainPanel.add(msgLabel);
        mainPanel.add(btnOK);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    public static int showConfirm(Component parent, String message) {
        JDialog dialog = new JDialog((Frame) null, "Xác Nhận", true);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(450, 180);
        dialog.setLocationRelativeTo(parent);
        
        // Main panel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 15, 15);
                
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                
                g2.setColor(new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
            }
        };
        mainPanel.setLayout(null);
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setOpaque(false);
        
        // Icon panel
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(AppTheme.PRIMARY_BLUE);
                g2.fillOval(5, 5, 70, 70);
                
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
                FontMetrics fm = g2.getFontMetrics();
                String icon = "?";
                int x = (80 - fm.stringWidth(icon)) / 2;
                int y = ((80 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(icon, x, y);
            }
        };
        iconPanel.setBackground(Color.WHITE);
        iconPanel.setOpaque(true);
        iconPanel.setBounds(20, 20, 80, 80);
        
        // Message label
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(AppTheme.FONT_LABEL_REGULAR);
        msgLabel.setForeground(new Color(50, 50, 50));
        msgLabel.setBounds(120, 30, 310, 60);
        msgLabel.setVerticalAlignment(SwingConstants.TOP);
        
        // Buttons
        int[] result = {JOptionPane.CANCEL_OPTION};
        
        JButton btnYes = new JButton("Có");
        btnYes.setFont(AppTheme.FONT_BUTTON);
        btnYes.setBackground(AppTheme.SUCCESS_GREEN);
        btnYes.setForeground(Color.WHITE);
        btnYes.setBorderPainted(false);
        btnYes.setFocusPainted(false);
        btnYes.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnYes.setBounds(270, 130, 70, 35);
        btnYes.addActionListener(e -> {
            result[0] = JOptionPane.YES_OPTION;
            dialog.dispose();
        });
        
        JButton btnNo = new JButton("Không");
        btnNo.setFont(AppTheme.FONT_BUTTON);
        btnNo.setBackground(AppTheme.ERROR_RED);
        btnNo.setForeground(Color.WHITE);
        btnNo.setBorderPainted(false);
        btnNo.setFocusPainted(false);
        btnNo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNo.setBounds(360, 130, 70, 35);
        btnNo.addActionListener(e -> {
            result[0] = JOptionPane.NO_OPTION;
            dialog.dispose();
        });
        
        mainPanel.add(iconPanel);
        mainPanel.add(msgLabel);
        mainPanel.add(btnYes);
        mainPanel.add(btnNo);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
        
        return result[0];
    }
}
