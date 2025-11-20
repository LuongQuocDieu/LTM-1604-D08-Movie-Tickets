package web;

import mysql.CSDL;
import utils.AppTheme;
import utils.MessageDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.Set;

public class BookingUI extends JFrame {
    private int userId;
    private String userName;
    private int selectedMovieId;
    private String selectedMovieName;
    private int selectedRoomId;
    private String selectedRoomName;
    private int selectedShowId;
    private Set<String> selectedSeats = new HashSet<>();
    private java.util.Map<Integer, String> seatMap = new HashMap<>();
    private int selectedComboId = -1;
    private double comboPrice = 0;
    
    private JTabbedPane tabbedPane;
    private DefaultTableModel movieModel, roomModel, showModel, seatModel, comboModel;
    private JTable movieTable, roomTable, showTable, seatTable, comboTable;
    private JLabel lblSelectedMovie, lblSelectedRoom, lblSelectedShow, lblTotalPrice, lblComboSelected;

    // Constructor duy nhất - full step mode (không dùng nữa, nhưng giữ lại nếu admin cần)
    public BookingUI(int userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        
        setTitle("CinemaX - Đặt Vé (" + userName + ")");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        
        tabbedPane = new JTabbedPane();
        
        // Hiển thị tất cả các bước
        tabbedPane.addTab("Bước 1: Chọn Phim", createMoviePanel());
        tabbedPane.addTab("Bước 2: Chọn Phòng", createRoomPanel());
        tabbedPane.addTab("Bước 3: Chọn Suất Chiếu", createShowPanel());
        tabbedPane.addTab("Bước 4: Chọn Ghế", createSeatPanel());
        tabbedPane.addTab("Bước 5: Chọn Combo", createComboPanel());
        
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 4) {
                loadCombo();
            }
        });
        
        add(tabbedPane);
        loadMovies();
    }

    // ========== BẬC 1: CHỌN PHIM (CHỈ TÊN) ==========
    private JPanel createMoviePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Thông tin phim được chọn
        JPanel infoPanel = new JPanel();
        lblSelectedMovie = new JLabel("Chưa chọn phim");
        infoPanel.add(lblSelectedMovie);
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Bảng danh sách phim - CHỈ TÊN PHIM
        movieModel = new DefaultTableModel(new String[]{"Tên phim"}, 0);
        movieTable = new JTable(movieModel);
        movieTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        movieTable.getSelectionModel().addListSelectionListener(e -> {
            int row = movieTable.getSelectedRow();
            if (row >= 0) {
                // Lấy tên phim từ table, rồi query lấy ID từ DB
                selectedMovieName = (String) movieModel.getValueAt(row, 0);
                try (Connection conn = CSDL.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT id FROM phim WHERE ten_phim = ?")) {
                    ps.setString(1, selectedMovieName);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        selectedMovieId = rs.getInt("id");
                        lblSelectedMovie.setText("Đã chọn: " + selectedMovieName);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(movieTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Nút tiếp theo
        JPanel buttonPanel = new JPanel();
        JButton btnNext = new JButton("Tiếp tục →");
        btnNext.addActionListener(e -> {
            if (selectedMovieId == 0) {
                MessageDialog.showInfo(panel, "Vui lòng chọn phim!");
                return;
            }
            tabbedPane.setSelectedIndex(1);
        });
        buttonPanel.add(btnNext);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // ========== BẬC 2: CHỌN PHÒNG ==========
    private JPanel createRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Thông tin
        JPanel infoPanel = new JPanel();
        lblSelectedRoom = new JLabel("Chưa chọn phòng");
        infoPanel.add(lblSelectedRoom);
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Bảng danh sách phòng
        roomModel = new DefaultTableModel(new String[]{"Phòng", "Sức chứa"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        roomTable = new JTable(roomModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.getSelectionModel().addListSelectionListener(e -> {
            int row = roomTable.getSelectedRow();
            if (row >= 0) {
                selectedRoomName = (String) roomModel.getValueAt(row, 0);
                // Query để lấy ID phòng
                try (Connection conn = CSDL.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT id FROM phong_chieu WHERE ten_phong = ?")) {
                    ps.setString(1, selectedRoomName);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        selectedRoomId = rs.getInt("id");
                        lblSelectedRoom.setText("Đã chọn: " + selectedRoomName);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(roomTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Nút
        JPanel buttonPanel = new JPanel();
        JButton btnBack = new JButton("← Quay lại");
        btnBack.addActionListener(e -> {
            tabbedPane.setSelectedIndex(0);
            selectedRoomId = 0;
        });
        
        JButton btnNext = new JButton("Tiếp tục →");
        btnNext.addActionListener(e -> {
            if (selectedRoomId == 0) {
                MessageDialog.showInfo(panel, "Vui lòng chọn phòng!");
                return;
            }
            tabbedPane.setSelectedIndex(2);
            loadShows();
        });
        
        buttonPanel.add(btnBack);
        buttonPanel.add(btnNext);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Load phòng
        loadRooms();
        
        return panel;
    }

    // ========== BẬC 3: CHỌN SUẤT CHIẾU ==========
    private JPanel createShowPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Thông tin
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblSelectedShow = new JLabel("Phim: " + selectedMovieName + " | Phòng: " + selectedRoomName);
        infoPanel.add(lblSelectedShow);
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Bảng suất chiếu
        showModel = new DefaultTableModel(
            new String[]{"ID", "Ngày chiếu", "Giờ chiếu", "Giá vé", "Ghế trống"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        showTable = new JTable(showModel);
        showTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        showTable.getSelectionModel().addListSelectionListener(e -> {
            int row = showTable.getSelectedRow();
            if (row >= 0) {
                selectedShowId = (Integer) showModel.getValueAt(row, 0);
                lblSelectedShow.setText("Phim: " + selectedMovieName + " | Phòng: " + selectedRoomName + 
                    " | Suất: " + showModel.getValueAt(row, 2) + " - Giá: " + showModel.getValueAt(row, 3));
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(showTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Nút
        JPanel buttonPanel = new JPanel();
        JButton btnBack = new JButton("← Quay lại");
        btnBack.addActionListener(e -> {
            tabbedPane.setSelectedIndex(1);
            selectedShowId = 0;
        });
        
        JButton btnNext = new JButton("Tiếp tục →");
        btnNext.addActionListener(e -> {
            if (selectedShowId == 0) {
                MessageDialog.showInfo(panel, "Vui lòng chọn suất chiếu!");
                return;
            }
            selectedSeats.clear();
            
            // Lấy tên suất chiếu và giá vé
            int row = showTable.getSelectedRow();
            String showName = showModel.getValueAt(row, 2).toString();
            int price = Integer.parseInt(showModel.getValueAt(row, 3).toString());
            
            // Mở SeatMapUI modal
            SeatMapUI seatMapUI = new SeatMapUI(selectedShowId, selectedRoomId, showName, price);
            seatMapUI.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            seatMapUI.setModal(true);
            seatMapUI.setLocationRelativeTo(null);
            seatMapUI.setVisible(true);
            
            // Nhận ghế đã chọn từ SeatMapUI
            Set<String> selectedSeatSet = seatMapUI.getSelectedSeats();
            if (selectedSeatSet.isEmpty()) {
                MessageDialog.showInfo(panel, "Vui lòng chọn ít nhất một ghế!");
                return;
            }
            selectedSeats.addAll(selectedSeatSet);
            tabbedPane.setSelectedIndex(3);
        });
        
        buttonPanel.add(btnBack);
        buttonPanel.add(btnNext);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // ========== BẬC 4: CHỌN GHẾ ==========
    private JPanel createSeatPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Thông tin
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblTotalPrice = new JLabel("Tổng tiền vé: 0 VND");
        infoPanel.add(lblTotalPrice);
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Bảng ghế
        seatModel = new DefaultTableModel(
            new String[]{"Ghế", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        seatTable = new JTable(seatModel);
        seatTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(seatTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Nút
        JPanel buttonPanel = new JPanel();
        JButton btnBack = new JButton("← Quay lại");
        btnBack.addActionListener(e -> {
            tabbedPane.setSelectedIndex(2);
            selectedSeats.clear();
        });
        
        JButton btnSelect = new JButton("Chọn ghế");
        btnSelect.addActionListener(e -> selectSeats(panel));
        
        JButton btnNext = new JButton("Tiếp tục →");
        btnNext.addActionListener(e -> {
            if (selectedSeats.isEmpty()) {
                MessageDialog.showInfo(panel, "Vui lòng chọn ghế!");
                return;
            }
            tabbedPane.setSelectedIndex(4);
        });
        
        buttonPanel.add(btnBack);
        buttonPanel.add(btnSelect);
        buttonPanel.add(btnNext);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // ========== BẬC 5: CHỌN COMBO ==========
    private JPanel createComboPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Thông tin
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblComboSelected = new JLabel("Combo: Không chọn");
        infoPanel.add(lblComboSelected);
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Bảng combo
        comboModel = new DefaultTableModel(
            new String[]{"ID", "Tên Combo", "Giá"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        comboTable = new JTable(comboModel);
        comboTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        comboTable.getSelectionModel().addListSelectionListener(e -> {
            int row = comboTable.getSelectedRow();
            if (row >= 0) {
                selectedComboId = (Integer) comboModel.getValueAt(row, 0);
                String comboName = (String) comboModel.getValueAt(row, 1);
                String priceStr = (String) comboModel.getValueAt(row, 2);
                comboPrice = Double.parseDouble(priceStr.replace(" VND", "").replace(",", ""));
                lblComboSelected.setText("Combo: " + comboName + " (" + priceStr + ")");
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(comboTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Nút
        JPanel buttonPanel = new JPanel();
        JButton btnBack = new JButton("← Quay lại");
        btnBack.addActionListener(e -> {
            tabbedPane.setSelectedIndex(3);
            selectedComboId = -1;
            comboPrice = 0;
        });
        
        JButton btnNoCombo = new JButton("Không chọn combo");
        btnNoCombo.addActionListener(e -> {
            selectedComboId = -1;
            comboPrice = 0;
            lblComboSelected.setText("Combo: Không chọn");
        });
        
        JButton btnConfirm = new JButton("Xác nhận & Thanh toán");
        btnConfirm.addActionListener(e -> confirmBooking(panel));
        
        buttonPanel.add(btnBack);
        buttonPanel.add(btnNoCombo);
        buttonPanel.add(btnConfirm);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    // ========== LOAD DATA METHODS ==========
    private void loadMovies() {
        movieModel.setRowCount(0);
        try (Connection conn = CSDL.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT ten_phim FROM phim ORDER BY ten_phim")) {
            while (rs.next()) {
                movieModel.addRow(new Object[]{
                    rs.getString("ten_phim")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRooms() {
        roomModel.setRowCount(0);
        try (Connection conn = CSDL.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ten_phong, suc_chua FROM phong_chieu ORDER BY ten_phong")) {
            while (rs.next()) {
                roomModel.addRow(new Object[]{
                    rs.getString("ten_phong"),
                    rs.getInt("suc_chua") + " ghế"
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadShows() {
        showModel.setRowCount(0);
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT x.id, x.ngay, x.gio, x.gia_ve, x.ve_con " +
                "FROM xuat_chieu x " +
                "JOIN phim p ON x.phim_id = p.id " +
                "WHERE x.phim_id = ? AND x.phong_id = ? AND x.ngay >= ? " +
                "ORDER BY x.ngay, x.gio")) {
            ps.setInt(1, selectedMovieId);
            ps.setInt(2, selectedRoomId);
            ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                showModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("ngay"),
                    rs.getString("gio"),
                    rs.getInt("gia_ve") + " VND",
                    rs.getInt("ve_con")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSeats() {
        seatModel.setRowCount(0);
        seatMap.clear();
        
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT g.id, g.ma_ghe FROM ghe g " +
                "WHERE g.phong_id = ? " +
                "ORDER BY g.ma_ghe")) {
            ps.setInt(1, selectedRoomId);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int gheId = rs.getInt("id");
                String maGhe = rs.getString("ma_ghe");
                seatMap.put(gheId, maGhe);
                
                // Kiểm tra xem ghế đã đặt chưa
                if (isSeatBooked(gheId)) {
                    seatModel.addRow(new Object[]{maGhe, "Đã đặt"});
                } else {
                    seatModel.addRow(new Object[]{maGhe, "Còn trống"});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isSeatBooked(int gheId) throws SQLException {
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM ve WHERE xuat_chieu_id = ? AND ghe_id = ? AND trang_thai <> 'da_huy'")) {
            ps.setInt(1, selectedShowId);
            ps.setInt(2, gheId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ========== ACTION METHODS ==========
    private void selectSeats(JPanel panel) {
        int[] selectedRows = seatTable.getSelectedRows();
        if (selectedRows.length == 0) {
            MessageDialog.showInfo(panel, "Vui lòng chọn ít nhất 1 ghế!");
            return;
        }
        
        selectedSeats.clear();
        for (int row : selectedRows) {
            String seatStatus = (String) seatModel.getValueAt(row, 1);
            if ("Đã đặt".equals(seatStatus)) {
                MessageDialog.showInfo(panel, "Ghế " + seatModel.getValueAt(row, 0) + " đã được đặt!");
                return;
            }
        }
        
        // Lấy tên ghế từ map
        for (int row : selectedRows) {
            String maGhe = (String) seatModel.getValueAt(row, 0);
            selectedSeats.add(maGhe);
        }
        
        updateTotalPrice();
        MessageDialog.showInfo(panel, "Đã chọn " + selectedSeats.size() + " ghế!");
    }

    private void updateTotalPrice() {
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT gia_ve FROM xuat_chieu WHERE id = ?")) {
            ps.setInt(1, selectedShowId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int price = rs.getInt("gia_ve");
                long totalPrice = (long) price * selectedSeats.size();
                lblTotalPrice.setText("Tổng tiền vé: " + totalPrice + " VND");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void confirmBooking(JPanel panel) {
        if (selectedSeats.isEmpty()) {
            MessageDialog.showInfo(panel, "Vui lòng chọn ghế!");
            return;
        }
        
        try (Connection conn = CSDL.getConnection()) {
            conn.setAutoCommit(false);
            
            // Lấy giá vé
            double price;
            try (PreparedStatement ps = conn.prepareStatement("SELECT gia_ve FROM xuat_chieu WHERE id = ?")) {
                ps.setInt(1, selectedShowId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    price = rs.getDouble("gia_ve");
                } else {
                    throw new SQLException("Không tìm thấy suất chiếu");
                }
            }
            
            // Đặt vé
            long totalTicketPrice = (long) price * selectedSeats.size();
            long totalPrice = totalTicketPrice;
            if (selectedComboId != -1) {
                totalPrice += (long) comboPrice;
            }
            
            for (String seatName : selectedSeats) {
                // Lấy ID ghế từ tên ghế
                int gheId = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM ghe WHERE ten_ghe = ? AND phong_id = ?")) {
                    ps.setString(1, seatName);
                    ps.setInt(2, selectedRoomId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        gheId = rs.getInt("id");
                    } else {
                        throw new SQLException("Không tìm thấy ghế " + seatName);
                    }
                }
                
                try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ve (nguoi_dung_id, xuat_chieu_id, ghe_id, combo_id, tong_tien, trang_thai, ngay_dat) " +
                    "VALUES (?, ?, ?, ?, ?, 'chua_thanh_toan', NOW())")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, selectedShowId);
                    ps.setInt(3, gheId);
                    if (selectedComboId != -1) {
                        ps.setInt(4, selectedComboId);
                    } else {
                        ps.setNull(4, java.sql.Types.INTEGER);
                    }
                    ps.setDouble(5, totalPrice);
                    ps.executeUpdate();
                }
            }
            
            conn.commit();
            conn.setAutoCommit(true);
            
            String comboInfo = selectedComboId != -1 ? "\n+ Combo: " + comboPrice + " VND" : "";
            MessageDialog.showInfo(panel, "Đặt vé thành công!\n\nVé: " + 
                totalTicketPrice + " VND" + comboInfo + "\nTổng: " + totalPrice + " VND");
            
            this.dispose();
            
        } catch (SQLException e) {
            MessageDialog.showInfo(panel, "Lỗi: " + e.getMessage());
            try {
                Connection conn = CSDL.getConnection();
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void loadCombo() {
        comboModel.setRowCount(0);
        try (Connection conn = CSDL.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, ten_combo, gia FROM combo ORDER BY gia")) {
            while (rs.next()) {
                comboModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("ten_combo"),
                    String.format("%,d VND", rs.getLong("gia"))
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BookingUI frame = new BookingUI(1, "User Test");
            frame.setVisible(true);
        });
    }
}
