package web;

import mysql.CSDL;
import utils.AppTheme;
import utils.MessageDialog;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.*;

// ============= Callback Interface =============
interface OnSeatsSelectedListener {
    void onSeatsSelected(int showId, java.util.List<String> selectedSeats, int totalPrice);
}

// ============= Custom Rounded Button =============
class RoundedSeatButton extends JButton {
    private static final int CORNER_RADIUS = 8;

    public RoundedSeatButton(String text) {
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

public class SeatMapUI extends JDialog {
    private int showId;
    private int roomId;
    private String showName;
    private int price;
    private Set<String> selectedSeats = new HashSet<>();
    private Map<String, JButton> seatButtons = new HashMap<>();
    private JLabel lblSelectedSeats;
    private JLabel lblTotal;
    private JButton btnConfirm;
    private OnSeatsSelectedListener listener;

    public SeatMapUI(int showId, int roomId, String showName, int price) {
        this(showId, roomId, showName, price, null);
    }

    public SeatMapUI(int showId, int roomId, String showName, int price, OnSeatsSelectedListener listener) {
        this.showId = showId;
        this.roomId = roomId;
        this.showName = showName;
        this.price = price;
        this.listener = listener;

        setTitle("Chọn ghế - " + showName);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(1200, 820);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(248, 249, 250));

        // ===== TOP: HEADER =====
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(new Color(25, 103, 210));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel lblHeader = new JLabel("Suất: " + showName + " | Phòng: " + roomId + " | Giá 1 vé: " + String.format("%,d VND", price));
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblHeader.setForeground(Color.WHITE);
        headerPanel.add(lblHeader);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ===== CENTER: SEAT MAP + SIDEBAR =====
        JPanel centerPanel = new JPanel(new BorderLayout(15, 0));

        // SEAT MAP (LEFT)
        JPanel seatMapPanel = new JPanel();
        seatMapPanel.setLayout(new BoxLayout(seatMapPanel, BoxLayout.Y_AXIS));
        seatMapPanel.setBackground(Color.WHITE);
        seatMapPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        loadSeats(seatMapPanel);

        JScrollPane seatScroll = new JScrollPane(seatMapPanel);
        seatScroll.getVerticalScrollBar().setUnitIncrement(20);
        centerPanel.add(seatScroll, BorderLayout.CENTER);

        // SIDEBAR (RIGHT) - Hiển thị ghế đã chọn
        JPanel sidebarPanel = new JPanel(new BorderLayout(10, 10));
        sidebarPanel.setPreferredSize(new Dimension(250, 600));
        sidebarPanel.setBackground(new Color(250, 250, 250));
        sidebarPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel lblSideTitle = new JLabel("Ghế đã chọn:");
        lblSideTitle.setFont(new Font("Arial", Font.BOLD, 12));
        sidebarPanel.add(lblSideTitle, BorderLayout.NORTH);

        lblSelectedSeats = new JLabel("<html>-</html>");
        lblSelectedSeats.setFont(new Font("Arial", Font.PLAIN, 11));
        JScrollPane sideScroll = new JScrollPane(lblSelectedSeats);
        sidebarPanel.add(sideScroll, BorderLayout.CENTER);

        lblTotal = new JLabel("<html><b>Tổng tiền: 0 VND</b></html>");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 13));
        lblTotal.setBackground(new Color(230, 245, 230));
        lblTotal.setOpaque(true);
        lblTotal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sidebarPanel.add(lblTotal, BorderLayout.SOUTH);

        centerPanel.add(sidebarPanel, BorderLayout.EAST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // ===== BOTTOM: BUTTONS =====
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        bottomPanel.setBackground(Color.WHITE);

        RoundedSeatButton btnCancel = new RoundedSeatButton("Hủy");
        btnCancel.setBackground(new Color(158, 158, 158));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancel.setPreferredSize(new Dimension(100, 40));
        btnCancel.addActionListener(e -> {
            selectedSeats.clear();
            dispose();
        });

        btnConfirm = new RoundedSeatButton("Thêm vào giỏ hàng");
        btnConfirm.setBackground(new Color(52, 168, 83));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConfirm.setPreferredSize(new Dimension(140, 40));
        btnConfirm.setEnabled(false);
        btnConfirm.addActionListener(e -> {
            if (listener != null && !selectedSeats.isEmpty()) {
                int totalPrice = selectedSeats.size() * price;
                listener.onSeatsSelected(showId, new ArrayList<>(selectedSeats), totalPrice);
            }
            dispose();
        });

        bottomPanel.add(btnCancel);
        bottomPanel.add(btnConfirm);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void loadSeats(JPanel seatMapPanel) {
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id, ma_ghe FROM ghe WHERE phong_id = ? ORDER BY ma_ghe")) {
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();

            Map<String, Integer> seatIds = new HashMap<>();
            java.util.List<String> seats = new ArrayList<>();

            while (rs.next()) {
                int id = rs.getInt("id");
                String maGhe = rs.getString("ma_ghe");
                seatIds.put(maGhe, id);
                seats.add(maGhe);
            }

            // Group by row (A1-A10, B1-B10, etc.)
            Map<Character, java.util.List<String>> rowSeats = new TreeMap<>();
            for (String seat : seats) {
                char row = seat.charAt(0);
                rowSeats.computeIfAbsent(row, k -> new ArrayList<>()).add(seat);
            }

            // Screen label
            JPanel screenLabel = new JPanel();
            screenLabel.setBackground(Color.WHITE);
            screenLabel.setPreferredSize(new Dimension(600, 30));
            JLabel lblScreen = new JLabel("MÀN HÌNH");
            lblScreen.setFont(new Font("Arial", Font.BOLD, 12));
            lblScreen.setForeground(Color.GRAY);
            screenLabel.add(lblScreen);
            seatMapPanel.add(screenLabel);

            // Create seat grid
            for (Map.Entry<Character, java.util.List<String>> entry : rowSeats.entrySet()) {
                JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
                rowPanel.setBackground(Color.WHITE);

                for (String seatName : entry.getValue()) {
                    JButton seatBtn = new JButton(seatName);
                    seatBtn.setPreferredSize(new Dimension(45, 45));
                    seatBtn.setFont(new Font("Arial", Font.BOLD, 10));
                    seatBtn.setBackground(new Color(76, 175, 80)); // Xanh lá
                    seatBtn.setForeground(Color.WHITE);
                    seatBtn.setFocusPainted(false);
                    seatBtn.setBorder(BorderFactory.createLineBorder(new Color(56, 142, 60)));

                    // Check if already booked
                    if (isSeatBooked(seatIds.get(seatName))) {
                        seatBtn.setBackground(Color.RED);
                        seatBtn.setEnabled(false);
                    } else {
                        seatBtn.addActionListener(e -> toggleSeat(seatName, seatBtn));
                    }

                    seatButtons.put(seatName, seatBtn);
                    rowPanel.add(seatBtn);
                }
                seatMapPanel.add(rowPanel);
            }

            // Add spacing
            seatMapPanel.add(Box.createVerticalStrut(10));

        } catch (SQLException e) {
            e.printStackTrace();
            MessageDialog.showInfo(this, "Lỗi tải ghế: " + e.getMessage());
        }
    }

    private boolean isSeatBooked(int gheId) throws SQLException {
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM ve WHERE xuat_chieu_id = ? AND ghe_id = ? AND trang_thai != 'da_huy'")) {
            ps.setInt(1, showId);
            ps.setInt(2, gheId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void toggleSeat(String seatName, JButton btn) {
        if (selectedSeats.contains(seatName)) {
            selectedSeats.remove(seatName);
            btn.setBackground(new Color(76, 175, 80)); // Xanh lá
        } else {
            selectedSeats.add(seatName);
            btn.setBackground(new Color(255, 193, 7)); // Vàng
        }

        updateDisplay();
    }

    private void updateDisplay() {
        // Update selected seats label
        if (selectedSeats.isEmpty()) {
            lblSelectedSeats.setText("<html>-</html>");
        } else {
            java.util.List<String> sorted = new ArrayList<>(selectedSeats);
            Collections.sort(sorted);
            StringBuilder html = new StringBuilder("<html>");
            for (int i = 0; i < sorted.size(); i++) {
                html.append(sorted.get(i));
                if ((i + 1) % 5 == 0) html.append("<br>");
                else html.append(", ");
            }
            html.append("</html>");
            lblSelectedSeats.setText(html.toString());
        }

        // Update total
        long total = (long) selectedSeats.size() * price;
        lblTotal.setText(String.format("<html><b>Tổng tiền:<br>%,d VND</b></html>", total));
        btnConfirm.setEnabled(!selectedSeats.isEmpty());
    }

    public Set<String> getSelectedSeats() {
        return selectedSeats;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SeatMapUI frame = new SeatMapUI(4, 1, "Suất 4 | Phòng 1 | Giá 1 vé: 100 VND", 100);
            frame.setVisible(true);
        });
    }
}
