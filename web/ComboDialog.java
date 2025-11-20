package web;

import mysql.CSDL;
import utils.AppTheme;
import utils.MessageDialog;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.*;

// ============= Custom Rounded Button =============
class RoundedComboButton extends JButton {
    private static final int CORNER_RADIUS = 8;

    public RoundedComboButton(String text) {
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

public class ComboDialog extends JDialog {
    private int userId;
    private int showId;
    private int roomId;
    private Set<String> selectedSeats;
    private int ticketPrice;
    private int selectedComboId = -1;
    private double comboPrice = 0;
    private DefaultListModel<String> comboListModel;

    public ComboDialog(int userId, int showId, int roomId, Set<String> seats, int ticketPrice, String movieName) {
        this.userId = userId;
        this.showId = showId;
        this.roomId = roomId;
        this.selectedSeats = seats;
        this.ticketPrice = ticketPrice;

        setTitle("Chọn Combo & Thanh Toán - " + movieName);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(520, 650);
        setLocationRelativeTo(null);
        setModal(true);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(248, 249, 250));

        // ===== TOP: HEADER =====
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(25, 103, 210));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel lblHeader = new JLabel("Bước 3: Chọn Combo & Thanh Toán");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHeader.setForeground(Color.WHITE);
        headerPanel.add(lblHeader);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ===== CENTER: COMBO LIST =====
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        JLabel lblComboTitle = new JLabel("Chọn Combo (Không bắt buộc):");
        lblComboTitle.setFont(new Font("Arial", Font.BOLD, 12));
        centerPanel.add(lblComboTitle, BorderLayout.NORTH);

        comboListModel = new DefaultListModel<>();
        JList<String> comboList = new JList<>(comboListModel);
        comboList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        comboList.addListSelectionListener(e -> {
            int idx = comboList.getSelectedIndex();
            if (idx >= 0) {
                loadComboInfo(idx);
            }
        });
        JScrollPane scrollPane = new JScrollPane(comboList);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // ===== SOUTH: PRICE + BUTTONS =====
        JPanel southPanel = new JPanel(new BorderLayout(10, 0));

        JPanel pricePanel = new JPanel(new GridLayout(3, 1, 0, 5));
        pricePanel.setBackground(new Color(250, 250, 250));
        pricePanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        long ticketTotal = (long) selectedSeats.size() * ticketPrice;
        JLabel lblTicketPrice = new JLabel(String.format("Vé (%d ghế × %,d VND): %,d VND", 
            selectedSeats.size(), ticketPrice, ticketTotal));
        lblTicketPrice.setFont(new Font("Arial", Font.PLAIN, 11));

        JLabel lblComboPrice = new JLabel("Combo: 0 VND");
        lblComboPrice.setFont(new Font("Arial", Font.PLAIN, 11));

        JLabel lblTotal = new JLabel(String.format("<html><b>Tổng cộng: %,d VND</b></html>", ticketTotal));
        lblTotal.setFont(new Font("Arial", Font.BOLD, 13));
        lblTotal.setForeground(new Color(231, 76, 60));

        pricePanel.add(lblTicketPrice);
        pricePanel.add(lblComboPrice);
        pricePanel.add(lblTotal);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        RoundedComboButton btnCancel = new RoundedComboButton("Hủy");
        btnCancel.setBackground(new Color(158, 158, 158));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancel.setPreferredSize(new Dimension(100, 40));
        btnCancel.addActionListener(e -> dispose());

        RoundedComboButton btnPay = new RoundedComboButton("Thanh Toán");
        btnPay.setBackground(new Color(52, 168, 83));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPay.setPreferredSize(new Dimension(140, 40));
        btnPay.addActionListener(e -> confirmPayment(ticketTotal));

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnPay);

        southPanel.add(pricePanel, BorderLayout.NORTH);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(southPanel, BorderLayout.SOUTH);
        add(mainPanel);

        // Load combo list
        loadCombos();

        // Update price when combo selected
        comboList.addListSelectionListener(e -> {
            long total = ticketTotal + (long)comboPrice;
            lblComboPrice.setText(String.format("Combo: %,d VND", (long)comboPrice));
            lblTotal.setText(String.format("<html><b>Tổng cộng: %,d VND</b></html>", total));
        });
    }

    private void loadCombos() {
        try (Connection conn = CSDL.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, ten_combo, gia FROM combo ORDER BY gia")) {
            comboListModel.clear();
            comboListModel.addElement("Không chọn combo");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("ten_combo");
                int price = rs.getInt("gia");
                comboListModel.addElement(name + " - " + String.format("%,d VND", price));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadComboInfo(int idx) {
        if (idx == 0) {
            selectedComboId = -1;
            comboPrice = 0;
        } else {
            try (Connection conn = CSDL.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, gia FROM combo LIMIT " + (idx) + ", 1")) {
                if (rs.next()) {
                    selectedComboId = rs.getInt("id");
                    comboPrice = rs.getDouble("gia");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void confirmPayment(long ticketTotal) {
        long total = ticketTotal + (long)comboPrice;
        
        try (Connection conn = CSDL.getConnection()) {
            conn.setAutoCommit(false);

            // Insert tickets
            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ve (nguoi_dung_id, xuat_chieu_id, ghe_id, combo_id, tong_tien, trang_thai, ngay_dat) " +
                "VALUES (?, ?, ?, ?, ?, 'chua_thanh_toan', NOW())")) {
                
                for (String seatName : selectedSeats) {
                    // Get seat ID
                    int gheId = 0;
                    try (PreparedStatement psSeat = conn.prepareStatement(
                        "SELECT id FROM ghe WHERE ma_ghe = ? AND phong_id = ?")) {
                        psSeat.setString(1, seatName);
                        psSeat.setInt(2, roomId);
                        ResultSet rs = psSeat.executeQuery();
                        if (rs.next()) {
                            gheId = rs.getInt("id");
                        }
                    }

                    ps.setInt(1, userId);
                    ps.setInt(2, showId);
                    ps.setInt(3, gheId);
                    if (selectedComboId != -1) {
                        ps.setInt(4, selectedComboId);
                    } else {
                        ps.setNull(4, java.sql.Types.INTEGER);
                    }
                    ps.setLong(5, total);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);

            String comboInfo = selectedComboId != -1 ? "\nCombo: " + comboPrice + " VND" : "\nKhông chọn combo";
            MessageDialog.showSuccess(this, 
                "Đặt vé thành công!\n\n" +
                "Ghế: " + String.join(", ", selectedSeats) + "\n" +
                "Giá vé: " + ticketPrice + " VND/ghế\n" +
                comboInfo + "\n" +
                "---\n" +
                "Tổng tiền: " + String.format("%,d VND", total));
            
            dispose();

        } catch (SQLException e) {
            try {
                if (this.getParent() instanceof javax.swing.JFrame) {
                    javax.swing.JFrame parent = (javax.swing.JFrame) this.getParent();
                    if (parent != null) {
                        Connection conn = CSDL.getConnection();
                        if (conn != null) {
                            conn.rollback();
                            conn.close();
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            e.printStackTrace();
            MessageDialog.showError(this, "Lỗi thanh toán: " + e.getMessage());
        }
    }
}
