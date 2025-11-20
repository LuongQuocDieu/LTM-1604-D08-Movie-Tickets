package web;

import mysql.CSDL;
import utils.AppTheme;
import utils.MessageDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.sql.*;

public class CartUI extends JFrame {
    private int userId;
    private String userName;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel lblTotalPrice;

    public CartUI(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;

        setTitle("CinemaX - Giỏ Hàng (" + userName + ")");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        
        // Apply theme colors
        Color primaryColor = new Color(25, 103, 210);
        Color accentColor = new Color(52, 168, 83);
        Color bgColor = new Color(248, 249, 250);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Tiêu đề
        JLabel lblTitle = new JLabel("Giỏ Hàng - Vé Chưa Thanh Toán");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(primaryColor);
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Bảng giỏ hàng
        cartModel = new DefaultTableModel(
            new String[]{"ID Vé", "Phim", "Phòng", "Ghế", "Ngày", "Giờ", "Giá Vé", "Combo", "Tổng Tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cartTable.setRowHeight(28);
        cartTable.getTableHeader().setBackground(primaryColor);
        cartTable.getTableHeader().setForeground(Color.WHITE);
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        cartTable.setSelectionBackground(new Color(200, 220, 255));
        
        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel thông tin
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        infoPanel.setBackground(bgColor);
        lblTotalPrice = new JLabel("Tổng Cộng: 0 VND");
        lblTotalPrice.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalPrice.setForeground(primaryColor);
        infoPanel.add(lblTotalPrice);

        // Panel nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(bgColor);
        
        RoundedButton btnDelete = new RoundedButton("Xóa Vé");
        btnDelete.setBackground(new Color(244, 67, 54));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDelete.setPreferredSize(new Dimension(120, 36));
        btnDelete.addActionListener(e -> deleteSelectedItem());
        
        RoundedButton btnRefresh = new RoundedButton("Làm mới");
        btnRefresh.setBackground(new Color(255, 152, 0));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setPreferredSize(new Dimension(120, 36));
        btnRefresh.addActionListener(e -> loadCart());
        
        RoundedButton btnPayment = new RoundedButton("Thanh Toán Ngay");
        btnPayment.setBackground(accentColor);
        btnPayment.setForeground(Color.WHITE);
        btnPayment.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPayment.setPreferredSize(new Dimension(160, 36));
        btnPayment.addActionListener(e -> openPaymentDialog());
        
        RoundedButton btnCancel = new RoundedButton("Đóng");
        btnCancel.setBackground(new Color(158, 158, 158));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancel.setPreferredSize(new Dimension(100, 36));
        btnCancel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> this.dispose());
        
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnPayment);
        buttonPanel.add(btnCancel);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBackground(bgColor);
        southPanel.add(infoPanel, BorderLayout.NORTH);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        add(mainPanel);
        loadCart();
    }

    private void loadCart() {
        cartModel.setRowCount(0);
        long totalPrice = 0;

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT v.id, p.ten_phim, pc.ten_phong, g.ma_ghe, x.ngay, x.gio, " +
                "x.gia_ve, COALESCE(c.ten_combo, 'Không'), v.tong_tien " +
                "FROM ve v " +
                "JOIN xuat_chieu x ON v.xuat_chieu_id = x.id " +
                "JOIN phim p ON x.phim_id = p.id " +
                "JOIN phong_chieu pc ON x.phong_id = pc.id " +
                "JOIN ghe g ON v.ghe_id = g.id " +
                "LEFT JOIN combo c ON v.combo_id = c.id " +
                "WHERE v.nguoi_dung_id = ? AND v.trang_thai = 'chua_thanh_toan' " +
                "ORDER BY x.ngay, x.gio")) {
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                int veId = rs.getInt("id");
                String tenPhim = rs.getString("ten_phim");
                String tenPhong = rs.getString("ten_phong");
                String maGhe = rs.getString("ma_ghe");
                String ngayChieu = rs.getString("ngay");
                String gioChieu = rs.getString("gio");
                int giaVe = rs.getInt("gia_ve");
                String combo = rs.getString(8);  // COALESCE(c.ten_combo, 'Không')
                long tongTien = rs.getLong("tong_tien");
                
                cartModel.addRow(new Object[]{
                    veId,
                    tenPhim,
                    tenPhong,
                    maGhe,
                    ngayChieu,
                    gioChieu,
                    giaVe + " VND",
                    combo,
                    tongTien + " VND"
                });
                
                totalPrice += tongTien;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        lblTotalPrice.setText("Tổng Cộng: " + String.format("%,d", totalPrice) + " VND");
    }

    private void deleteSelectedItem() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow < 0) {
            MessageDialog.showWarning(this, "Vui lòng chọn vé để xóa!");
            return;
        }

        int veId = (int) cartModel.getValueAt(selectedRow, 0);
        
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM ve WHERE id = ?")) {
            ps.setInt(1, veId);
            ps.executeUpdate();
            MessageDialog.showSuccess(this, "Đã xóa vé khỏi giỏ hàng!");
            loadCart();
        } catch (SQLException e) {
            e.printStackTrace();
            MessageDialog.showError(this, "Lỗi xóa vé: " + e.getMessage());
        }
    }

    private void openPaymentDialog() {
        if (cartModel.getRowCount() == 0) {
            MessageDialog.showWarning(this, "Không có vé để thanh toán!");
            return;
        }

        // Tạo QR code payment dialog
        JDialog paymentDialog = new JDialog(this, "Thanh Toán QR", true);
        paymentDialog.setSize(400, 500);
        paymentDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // QR Code (mô phỏng)
        JLabel qrLabel = new JLabel();
        qrLabel.setIcon(new ImageIcon(generateQRCodeImage()));
        qrLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(qrLabel, BorderLayout.NORTH);

        // Hướng dẫn
        JTextArea instructionArea = new JTextArea(
            "Hướng dẫn thanh toán:\n\n" +
            "1. Mở ứng dụng ngân hàng/ví điện tử\n" +
            "2. Quét mã QR bên trên\n" +
            "3. Xác nhận thanh toán\n" +
            "4. Nhấn 'Đã thanh toán' khi hoàn tất\n\n" +
            "Tổng tiền: " + getTotalPriceFromCart() + " VND"
        );
        instructionArea.setEditable(false);
        instructionArea.setLineWrap(true);
        instructionArea.setWrapStyleWord(true);
        instructionArea.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(new JScrollPane(instructionArea), BorderLayout.CENTER);

        // Nút
        JPanel buttonPanel = new JPanel();
        JButton btnConfirmPayment = new JButton("Đã Thanh Toán");
        btnConfirmPayment.setBackground(new Color(34, 139, 34));
        btnConfirmPayment.setForeground(Color.WHITE);
        btnConfirmPayment.addActionListener(e -> {
            confirmPayment();
            paymentDialog.dispose();
            this.dispose();
        });

        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(e -> paymentDialog.dispose());

        buttonPanel.add(btnConfirmPayment);
        buttonPanel.add(btnCancel);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        paymentDialog.add(panel);
        paymentDialog.setVisible(true);
    }

    private BufferedImage generateQRCodeImage() {
        // Tạo hình ảnh QR code đơn giản (mô phỏng)
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        java.util.Random rand = new java.util.Random();
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 200; x++) {
                image.setRGB(x, y, rand.nextBoolean() ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return image;
    }

    private long getTotalPriceFromCart() {
        long total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String priceStr = (String) cartModel.getValueAt(i, 8);
            long price = Long.parseLong(priceStr.replace(" VND", "").replace(",", ""));
            total += price;
        }
        return total;
    }

    private void confirmPayment() {
        try (Connection conn = CSDL.getConnection()) {
            conn.setAutoCommit(false);

            // Update ticket status to "da_thanh_toan"
            try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE ve SET trang_thai = 'da_thanh_toan' WHERE nguoi_dung_id = ? AND trang_thai = 'chua_thanh_toan'")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);

            showPaymentSuccessDialog();

        } catch (SQLException e) {
            e.printStackTrace();
            MessageDialog.showInfo(this, "Lỗi: " + e.getMessage());
        }
    }

    private void showPaymentSuccessDialog() {
        JDialog dlg = new JDialog(this, "Thanh Toán Thành Công", true);
        dlg.setSize(500, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(AppTheme.BG_LIGHT);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Icon panel with checkmark circle
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int x = getWidth() / 2;
                int y = getHeight() / 2;
                int radius = 55;
                
                // Draw circle background
                g2.setColor(AppTheme.SUCCESS_GREEN);
                g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                
                // Draw checkmark
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int[] xPoints = {x - 25, x - 8, x + 30};
                int[] yPoints = {y + 5, y + 25, y - 20};
                g2.drawPolyline(xPoints, yPoints, 3);
            }
        };
        iconPanel.setBackground(AppTheme.BG_LIGHT);
        iconPanel.setPreferredSize(new Dimension(500, 140));
        mainPanel.add(iconPanel, BorderLayout.NORTH);
        
        // Message panel
        JPanel msgPanel = new JPanel();
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
        msgPanel.setBackground(AppTheme.BG_LIGHT);
        
        JLabel lblTitle = new JLabel("Thanh Toán Thành Công!");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(AppTheme.TEXT_DARK);
        lblTitle.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        JLabel lblMsg = new JLabel("Vé đã được lưu vào tài khoản của bạn.");
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblMsg.setForeground(new Color(100, 100, 100));
        lblMsg.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        msgPanel.add(lblTitle);
        msgPanel.add(Box.createVerticalStrut(12));
        msgPanel.add(lblMsg);
        
        mainPanel.add(msgPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(AppTheme.BG_LIGHT);
        
        JButton btnOK = new JButton("Xác Nhận");
        btnOK.setBackground(AppTheme.SUCCESS_GREEN);
        btnOK.setForeground(Color.WHITE);
        btnOK.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnOK.setFocusPainted(false);
        btnOK.setBorderPainted(false);
        btnOK.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOK.setPreferredSize(new Dimension(180, 50));
        btnOK.addActionListener(e -> {
            dlg.dispose();
            loadCart();
        });
        
        btnPanel.add(btnOK);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);
        
        dlg.add(mainPanel);
        dlg.setVisible(true);
    }

    // ============= Custom Rounded Button =============
    static class RoundedButton extends JButton {
        private static final int CORNER_RADIUS = 8;

        public RoundedButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw rounded background
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);

            // Draw text
            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(text, x, y);
        }

        @Override
        protected void paintBorder(Graphics g) {
            // No border needed
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CartUI cart = new CartUI(1, "User Test");
            cart.setVisible(true);
        });
    }
}
