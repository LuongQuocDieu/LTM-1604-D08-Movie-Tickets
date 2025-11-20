package web;

import mysql.CSDL;
import utils.AppTheme;
import utils.MessageDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class HistoryUI extends JFrame {
    private int userId;
    private String userName;
    private JTable tblHistory;
    private DefaultTableModel modelHistory;

    public HistoryUI(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;

        setTitle("Lịch sử đặt vé - " + userName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Bảng lịch sử
        modelHistory = new DefaultTableModel(
            new String[]{"Mã vé", "Phim", "Phòng", "Ghế", "Suất chiếu", "Giá vé", "Combo", "Trạng thái", "Ngày đặt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblHistory = new JTable(modelHistory);
        tblHistory.setRowHeight(25);
        tblHistory.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(tblHistory);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.addActionListener(e -> loadHistory());
        topPanel.add(btnRefresh);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
        loadHistory();
    }

    private void loadHistory() {
        modelHistory.setRowCount(0);

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT v.ma_ve, p.ten_phim, pc.ten_phong, g.ma_ghe, " +
                "DATE_FORMAT(xc.thoi_gian, '%Y-%m-%d %H:%i'), v.gia_ve, c.ten_combo, v.trang_thai, v.ngay_tao " +
                "FROM ve v " +
                "JOIN xuat_chieu xc ON v.xuat_chieu_id = xc.id " +
                "JOIN phim p ON xc.phim_id = p.id " +
                "JOIN phong_chieu pc ON xc.phong_id = pc.id " +
                "JOIN ghe g ON v.ghe_id = g.id " +
                "LEFT JOIN combo c ON v.combo_id = c.id " +
                "WHERE v.nguoi_dung_id = ? " +
                "ORDER BY v.ngay_tao DESC")) {
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String comboName = rs.getString("ten_combo");
                String combo = (comboName != null) ? comboName : "Không";

                modelHistory.addRow(new Object[]{
                    rs.getString("ma_ve"),
                    rs.getString("ten_phim"),
                    rs.getString("ten_phong"),
                    rs.getString("ma_ghe"),
                    rs.getString(5),
                    rs.getDouble("gia_ve") + " VND",
                    combo,
                    rs.getString("trang_thai").equals("da_thanh_toan") ? "Đã thanh toán" : "Chưa thanh toán",
                    rs.getString("ngay_tao")
                });
            }

            if (modelHistory.getRowCount() == 0) {
                MessageDialog.showInfo(this, "Bạn chưa đặt vé nào!");
            }

        } catch (SQLException ex) {
            MessageDialog.showInfo(this, "Lỗi khi tải lịch sử: " + ex.getMessage());
        }
    }
}
