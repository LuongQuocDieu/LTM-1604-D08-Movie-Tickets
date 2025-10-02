package quanly;

import mysql.CSDL;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class QuanlyUI extends JFrame {
    // UI components
    private JTabbedPane tabbedPane;

    // Movies
    private JTable tblMovies;
    private DefaultTableModel modelMovies;
    private JTextField tfMovieId, tfMovieTitle, tfMovieGenre, tfMovieImage;
    private JSpinner spnMovieDuration;

    // Shows (xuất chiếu)
    private JTable tblShows;
    private DefaultTableModel modelShows;
    private JComboBox<MovieItem> cbMovieForShow;
    private JComboBox<RoomItem> cbRoomForShow;
    private JSpinner spnShowDateTime;
    private JSpinner spnShowTickets;
    private JTextField tfShowPrice, tfShowImage;

    // Rooms
    private JTable tblRooms;
    private DefaultTableModel modelRooms;
    private JTable tblRoomShows; // shows for selected room
    private DefaultTableModel modelRoomShows;

    // Users / Bookings / Stats
    private JTable tblUsers, tblBookings;
    private DefaultTableModel modelUsers, modelBookings;
    private JLabel lblStats;

    // Formatters
    private final SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public QuanlyUI() {
        setTitle("Admin Panel - Quản lý hệ thống đặt vé");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        loadInitialData();
    }

    private void initUI() {
        // Tăng font toàn bộ UI
        Font bigFont = new Font("Segoe UI", Font.PLAIN, 16);
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof Font) {
                UIManager.put(key, bigFont);
            }
        }

        tabbedPane = new JTabbedPane();
        add(tabbedPane);

        buildMoviesTab();
        buildShowsTab();
        buildRoomsTab();
        buildUsersTab();
        buildBookingsTab();
        buildStatsTab();
    }



    // ---------------------- Movies Tab ----------------------
    private void buildMoviesTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Form (left)
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("ID (tự tạo/để trống):"), g);
        g.gridx = 1;
        tfMovieId = new JTextField();
        form.add(tfMovieId, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Tên Phim:"), g);
        g.gridx = 1;
        tfMovieTitle = new JTextField();
        form.add(tfMovieTitle, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Thể Loại:"), g);
        g.gridx = 1;
        tfMovieGenre = new JTextField();
        form.add(tfMovieGenre, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Độ dài (phút):"), g);
        g.gridx = 1;
        spnMovieDuration = new JSpinner(new SpinnerNumberModel(90, 1, 999, 1));
        form.add(spnMovieDuration, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Ảnh (path/url):"), g);
        g.gridx = 1;
        tfMovieImage = new JTextField();
        enableDragDrop(tfMovieImage);
        form.add(tfMovieImage, g);

        g.gridx = 1; g.gridy++;
        JPanel fileBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnChooseImage = new JButton("Chọn Ảnh...");
        btnChooseImage.addActionListener(e -> chooseImageFor(tfMovieImage));
        fileBtns.add(btnChooseImage);
        form.add(fileBtns, g);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnAdd = new JButton("Thêm Phim");
        JButton btnUpdate = new JButton("Sửa Phim");
        JButton btnDelete = new JButton("Xóa Phim");
        btns.add(btnAdd); btns.add(btnUpdate); btns.add(btnDelete);
        g.gridx = 1; g.gridy++;
        form.add(btns, g);

        // Table (right)
        modelMovies = new DefaultTableModel(new Object[]{"id","ten_phim","the_loai","thoi_luong","anh"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblMovies = new JTable(modelMovies);
        tblMovies.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane spMovies = new JScrollPane(tblMovies);

        panel.add(form, BorderLayout.WEST);
        panel.add(spMovies, BorderLayout.CENTER);

        // button actions
        btnAdd.addActionListener(e -> addMovie());
        btnUpdate.addActionListener(e -> updateMovie());
        btnDelete.addActionListener(e -> deleteMovie());

        // when select movie in table -> fill form
        tblMovies.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillMovieFormFromSelection();
        });

        tabbedPane.addTab("Quản lý Phim", panel);
    }

    private void fillMovieFormFromSelection() {
        int r = tblMovies.getSelectedRow();
        if (r < 0) return;
        tfMovieId.setText(String.valueOf(modelMovies.getValueAt(r, 0)));
        tfMovieTitle.setText(String.valueOf(modelMovies.getValueAt(r, 1)));
        tfMovieGenre.setText(String.valueOf(modelMovies.getValueAt(r, 2)));
        Object dur = modelMovies.getValueAt(r, 3);
        try { spnMovieDuration.setValue(Integer.parseInt(String.valueOf(dur))); } catch (Exception ex) {}
        tfMovieImage.setText(String.valueOf(modelMovies.getValueAt(r, 4)));
    }

    // ---------------------- Shows Tab ----------------------
    private void buildShowsTab() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(8,8,8,8));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6); g.fill = GridBagConstraints.HORIZONTAL; g.gridx = 0; g.gridy = 0;

        form.add(new JLabel("Phim:"), g);
        g.gridx = 1;
        cbMovieForShow = new JComboBox<>();
        form.add(cbMovieForShow, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Phòng:"), g);
        g.gridx = 1;
        cbRoomForShow = new JComboBox<>();
        form.add(cbRoomForShow, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Ngày giờ:"), g);
        g.gridx = 1;
        spnShowDateTime = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spnShowDateTime, "yyyy-MM-dd HH:mm:ss");
        spnShowDateTime.setEditor(dateEditor);
        form.add(spnShowDateTime, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Số vé (tổng):"), g);
        g.gridx = 1;
        spnShowTickets = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
        form.add(spnShowTickets, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Giá vé (VND):"), g);
        g.gridx = 1;
        tfShowPrice = new JTextField();
        form.add(tfShowPrice, g);

        g.gridx = 0; g.gridy++;
        form.add(new JLabel("Ảnh (path):"), g);
        g.gridx = 1;
        tfShowImage = new JTextField();
        enableDragDrop(tfShowImage);
        form.add(tfShowImage, g);

        JButton btnChooseShowImg = new JButton("Chọn ảnh...");
        btnChooseShowImg.addActionListener(e -> chooseImageFor(tfShowImage));
        g.gridx = 1; g.gridy++;
        form.add(btnChooseShowImg, g);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddShow = new JButton("Thêm Suất");
        JButton btnUpdateShow = new JButton("Sửa Suất");
        JButton btnDeleteShow = new JButton("Xóa Suất");
        btns.add(btnAddShow); btns.add(btnUpdateShow); btns.add(btnDeleteShow);
        g.gridx = 1; g.gridy++;
        form.add(btns, g);

        // table: tách riêng ngày và giờ
        modelShows = new DefaultTableModel(new String[]{"id","movie_name","ngay","gio","room_name","tong_ve","gia_ve","anh"},0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblShows = new JTable(modelShows);
        tblShows.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane spShows = new JScrollPane(tblShows);

        panel.add(form, BorderLayout.WEST);
        panel.add(spShows, BorderLayout.CENTER);

        // actions
        btnAddShow.addActionListener(e -> addShow());
        btnUpdateShow.addActionListener(e -> updateShow());
        btnDeleteShow.addActionListener(e -> deleteShow());

        // fill form from selection
        tblShows.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillShowFormFromSelection();
        });

        tabbedPane.addTab("Quản lý Lịch Chiếu", panel);
    }

    private void fillShowFormFromSelection() {
        int r = tblShows.getSelectedRow();
        if (r < 0) return;
        // column indices: 0=id,1=movie,2=ngay,3=gio,4=room,5=tong_ve,6=gia_ve,7=anh
        String movieName = String.valueOf(modelShows.getValueAt(r,1));
        String ngay = String.valueOf(modelShows.getValueAt(r,2));
        String gio  = String.valueOf(modelShows.getValueAt(r,3));
        String roomName = String.valueOf(modelShows.getValueAt(r,4));
        String tongve = String.valueOf(modelShows.getValueAt(r,5));
        String giave = String.valueOf(modelShows.getValueAt(r,6));
        String anh = String.valueOf(modelShows.getValueAt(r,7));

        // select movie combobox item with same name
        for (int i=0;i<cbMovieForShow.getItemCount();i++){
            MovieItem it = cbMovieForShow.getItemAt(i);
            if (it.name.equals(movieName)) { cbMovieForShow.setSelectedIndex(i); break; }
        }
        for (int i=0;i<cbRoomForShow.getItemCount();i++){
            RoomItem it = cbRoomForShow.getItemAt(i);
            if (it.name.equals(roomName)) { cbRoomForShow.setSelectedIndex(i); break; }
        }
        try {
            java.util.Date d = dtFormat.parse(ngay + " " + gio);
            spnShowDateTime.setValue(d);
        } catch (Exception ex) { /* ignore */ }
        try { spnShowTickets.setValue(Integer.parseInt(tongve)); } catch (Exception ignored) {}
        tfShowPrice.setText(giave);
        tfShowImage.setText(anh);
    }

    // ---------------------- Rooms Tab ----------------------
    private void buildRoomsTab() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        panel.setBorder(new EmptyBorder(8,8,8,8));

        // Left: rooms table
        modelRooms = new DefaultTableModel(new String[]{"id","ten_phong","suc_chua","trang_thai"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblRooms = new JTable(modelRooms);
        tblRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane spRooms = new JScrollPane(tblRooms);

        // Right: shows for selected room (with ngay/gio split)
        modelRoomShows = new DefaultTableModel(new String[]{"id","phim","ngay","gio","tong_ve","ve_con","gia_ve"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblRoomShows = new JTable(modelRoomShows);
        JScrollPane spRoomShows = new JScrollPane(tblRoomShows);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spRooms, spRoomShows);
        split.setResizeWeight(0.4);
        panel.add(split, BorderLayout.CENTER);

        // On room select -> load shows for that room
        tblRooms.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblRooms.getSelectedRow();
                if (row < 0) return;
                String roomId = String.valueOf(modelRooms.getValueAt(row,0));
                loadShowsForRoom(roomId);
            }
        });

        tabbedPane.addTab("Phòng Chiếu", panel);
    }

    // ---------------------- Users Tab ----------------------
    private void buildUsersTab() {
        JPanel p = new JPanel(new BorderLayout());
        modelUsers = new DefaultTableModel(new String[]{"id","ho_ten","email","sdt"}, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        tblUsers = new JTable(modelUsers);
        JScrollPane sp = new JScrollPane(tblUsers);
        JButton btnLoad = new JButton("Tải người dùng");
        btnLoad.addActionListener(e -> loadUsers());
        p.add(btnLoad, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        tabbedPane.addTab("Người Dùng", p);
    }

    // ---------------------- Bookings Tab ----------------------
    private void buildBookingsTab() {
        JPanel p = new JPanel(new BorderLayout());
        // modelBookings with columns matching booking_local
        modelBookings = new DefaultTableModel(new String[]{"id","show_code","username","seat","amount","created_at"},0){
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        tblBookings = new JTable(modelBookings);
        tblBookings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(tblBookings);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnLoad = new JButton("Tải đặt vé (booking_local)");
        JButton btnDelete = new JButton("Xóa đặt vé đã chọn");
        JButton btnRefresh = new JButton("Làm mới");
        top.add(btnLoad);
        top.add(btnDelete);
        top.add(btnRefresh);

        btnLoad.addActionListener(e -> loadBookings());
        btnRefresh.addActionListener(e -> loadBookings());
        btnDelete.addActionListener(e -> deleteSelectedBooking());

        p.add(top, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        tabbedPane.addTab("Đặt Vé", p);
    }

    // ---------------------- Stats Tab ----------------------
    private void buildStatsTab() {
        JPanel p = new JPanel(new BorderLayout());
        lblStats = new JLabel("<html><i>Nhấn 'Cập nhật' để lấy thống kê</i></html>");
        p.add(lblStats, BorderLayout.CENTER);
        JButton btn = new JButton("Cập nhật Thống kê");
        btn.addActionListener(e -> loadStats());
        p.add(btn, BorderLayout.NORTH);
        tabbedPane.addTab("Thống kê", p);
    }

    // =================== Data access / Actions ===================

    private void loadInitialData() {
        loadMovies();
        loadRooms();
        loadShows();
        loadCombos(); // fill comboboxes (movies & rooms)
    }

    private void loadCombos() {
        cbMovieForShow.removeAllItems();
        cbRoomForShow.removeAllItems();
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT id,ten_phim FROM phim ORDER BY id");
            while (rs.next()) cbMovieForShow.addItem(new MovieItem(rs.getInt("id"), rs.getString("ten_phim")));
            rs.close();

            rs = st.executeQuery("SELECT id,ten_phong FROM phong_chieu ORDER BY id");
            while (rs.next()) cbRoomForShow.addItem(new RoomItem(rs.getInt("id"), rs.getString("ten_phong")));
            rs.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi load combobox: " + ex.getMessage());
        }
    }

    // ---- Movies CRUD ----
    private void loadMovies() {
        modelMovies.setRowCount(0);
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id,ten_phim,the_loai,thoi_luong,anh FROM phim ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelMovies.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("ten_phim"),
                        rs.getString("the_loai"),
                        rs.getInt("thoi_luong"),
                        rs.getString("anh")
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi tải phim: "+ex.getMessage()); }
        loadCombos();
    }

    private void addMovie() {
        String ten = tfMovieTitle.getText().trim();
        String theLoai = tfMovieGenre.getText().trim();
        int thoiLuong = (Integer) spnMovieDuration.getValue();
        String anh = tfMovieImage.getText().trim();

        if (ten.isEmpty()) { JOptionPane.showMessageDialog(this, "Tên phim không được để trống"); return; }

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO phim (ten_phim,the_loai,thoi_luong,anh,ngay_tao) VALUES (?,?,?,?,NOW())")) {
            ps.setString(1, ten);
            ps.setString(2, theLoai);
            ps.setInt(3, thoiLuong);
            ps.setString(4, anh);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Thêm phim thành công");
            loadMovies();
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi thêm phim: "+ex.getMessage()); }
    }

    private void updateMovie() {
        int r = tblMovies.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this,"Chọn phim cần sửa"); return; }
        int id = Integer.parseInt(String.valueOf(modelMovies.getValueAt(r,0)));
        String ten = tfMovieTitle.getText().trim();
        String theLoai = tfMovieGenre.getText().trim();
        int thoiLuong = (Integer) spnMovieDuration.getValue();
        String anh = tfMovieImage.getText().trim();

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE phim SET ten_phim=?,the_loai=?,thoi_luong=?,anh=? WHERE id=?")) {
            ps.setString(1, ten);
            ps.setString(2, theLoai);
            ps.setInt(3, thoiLuong);
            ps.setString(4, anh);
            ps.setInt(5, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Sửa phim thành công");
            loadMovies();
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi sửa phim: "+ex.getMessage()); }
    }

    private void deleteMovie() {
        int r = tblMovies.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this,"Chọn phim cần xóa"); return; }
        int id = Integer.parseInt(String.valueOf(modelMovies.getValueAt(r,0)));
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa phim id="+id+" ?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM phim WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Xóa phim thành công");
            loadMovies();
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi xóa phim: "+ex.getMessage()); }
    }

 // ---- Shows CRUD ----
    private void loadShows() {
        modelShows.setRowCount(0);
        String sql = "SELECT s.id, p.ten_phim, s.ngay, s.gio, r.ten_phong, " +
                     "s.tong_ve, s.gia_ve, s.anh " +
                     "FROM xuat_chieu s " +
                     "JOIN phim p ON s.phim_id = p.id " +
                     "JOIN phong_chieu r ON s.phong_id = r.id ORDER BY s.id";

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String ngay = rs.getString("ngay");
                String gio  = rs.getString("gio");

                modelShows.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("ten_phim"),
                    ngay,
                    gio,
                    rs.getString("ten_phong"),
                    rs.getInt("tong_ve"),
                    rs.getDouble("gia_ve"),
                    rs.getString("anh")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi tải suất chiếu: " + ex.getMessage());
        }
    }

    private void addShow() {
        MovieItem mi = (MovieItem) cbMovieForShow.getSelectedItem();
        RoomItem ri = (RoomItem) cbRoomForShow.getSelectedItem();
        java.util.Date dt = (java.util.Date) spnShowDateTime.getValue();
        int tongVe = (Integer) spnShowTickets.getValue();
        String giaStr = tfShowPrice.getText().trim();
        String anh = tfShowImage.getText().trim();

        if (mi == null || ri == null) {
            JOptionPane.showMessageDialog(this, "Chọn phim & phòng.");
            return;
        }
        if (giaStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập giá vé.");
            return;
        }

        double gia;
        try { gia = Double.parseDouble(giaStr); }
        catch (Exception ex){ JOptionPane.showMessageDialog(this,"Giá vé phải là số"); return; }

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO xuat_chieu (phim_id,phong_id,ngay,gio,tong_ve,ve_con,gia_ve,anh,ngay_tao) " +
                "VALUES (?,?,?,?,?,?,?,?,NOW())")) {

            java.sql.Date sqlDate = new java.sql.Date(dt.getTime());
            java.sql.Time sqlTime = new java.sql.Time(dt.getTime());

            ps.setInt(1, mi.id);
            ps.setInt(2, ri.id);
            ps.setDate(3, sqlDate);
            ps.setTime(4, sqlTime);
            ps.setInt(5, tongVe);
            ps.setInt(6, tongVe);
            ps.setDouble(7, gia);
            ps.setString(8, anh);

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Thêm suất chiếu thành công");
            loadShows();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi thêm suất: "+ex.getMessage());
        }
    }

    private void updateShow() {
        int r = tblShows.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this,"Chọn suất cần sửa"); return; }
        int showId = Integer.parseInt(String.valueOf(modelShows.getValueAt(r,0)));

        MovieItem mi = (MovieItem) cbMovieForShow.getSelectedItem();
        RoomItem ri = (RoomItem) cbRoomForShow.getSelectedItem();
        java.util.Date dt = (java.util.Date) spnShowDateTime.getValue();
        int tongVe = (Integer) spnShowTickets.getValue();
        String giaStr = tfShowPrice.getText().trim();
        String anh = tfShowImage.getText().trim();
        double gia;
        try { gia = Double.parseDouble(giaStr); }
        catch (Exception ex){ JOptionPane.showMessageDialog(this,"Giá vé phải là số"); return; }

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE xuat_chieu SET phim_id=?,phong_id=?,ngay=?,gio=?,tong_ve=?,ve_con=?,gia_ve=?,anh=? WHERE id=?")) {

            java.sql.Date sqlDate = new java.sql.Date(dt.getTime());
            java.sql.Time sqlTime = new java.sql.Time(dt.getTime());

            ps.setInt(1, mi.id);
            ps.setInt(2, ri.id);
            ps.setDate(3, sqlDate);
            ps.setTime(4, sqlTime);
            ps.setInt(5, tongVe);
            ps.setInt(6, tongVe);
            ps.setDouble(7, gia);
            ps.setString(8, anh);
            ps.setInt(9, showId);

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Sửa suất thành công");
            loadShows();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi sửa suất: "+ex.getMessage());
        }
    }


    private void deleteShow() {
        int r = tblShows.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this,"Chọn suất cần xóa"); return; }
        int showId = Integer.parseInt(String.valueOf(modelShows.getValueAt(r,0)));
        int conf = JOptionPane.showConfirmDialog(this, "Xóa suất id="+showId+" ?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM xuat_chieu WHERE id=?")) {
            ps.setInt(1, showId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Xóa suất thành công");
            loadShows();
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi xóa suất: "+ex.getMessage()); }
    }

    // ---- Rooms ----
    private void loadRooms() {
        modelRooms.setRowCount(0);
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id,ten_phong,suc_chua,trang_thai FROM phong_chieu ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelRooms.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("ten_phong"),
                        rs.getInt("suc_chua"),
                        rs.getString("trang_thai")
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi tải phòng: " + ex.getMessage()); }
    }

    private void loadShowsForRoom(String roomId) {
        modelRoomShows.setRowCount(0);
        String sql = "SELECT s.id, p.ten_phim, s.ngay_gio, s.tong_ve, s.ve_con, s.gia_ve FROM xuat_chieu s JOIN phim p ON s.phim_id = p.id WHERE s.phong_id = ? ORDER BY s.ngay_gio";
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(roomId));
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    Timestamp ts = rs.getTimestamp("ngay_gio");
                    String ngay = ts == null ? "" : dateFormat.format(ts);
                    String gio  = ts == null ? "" : timeFormat.format(ts);

                    modelRoomShows.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("ten_phim"),
                            ngay,
                            gio,
                            rs.getInt("tong_ve"),
                            rs.getInt("ve_con"),
                            rs.getDouble("gia_ve")
                    });
                }
                if (!any) {
                    JOptionPane.showMessageDialog(this, "Phòng này chưa có lịch chiếu.");
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi tải lịch phòng: " + ex.getMessage()); }
    }

    // ---- Users ----
    private void loadUsers() {
        modelUsers.setRowCount(0);
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id,ho_ten,email,sdt FROM nguoi_dung ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelUsers.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("ho_ten"),
                        rs.getString("email"),
                        rs.getString("sdt")
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this,"Lỗi tải user: "+ex.getMessage()); }
    }

    // ---- Bookings ----
    private void loadBookings() {
        modelBookings.setRowCount(0);
        String sql = "SELECT id, show_code, username, seat, amount, created_at FROM booking_local ORDER BY id";

       try (Connection conn = CSDL.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
           while (rs.next()) {
               modelBookings.addRow(new Object[]{
                   rs.getInt("id"),
                   rs.getString("show_code"),
                   rs.getString("username"),
                   rs.getString("seat"),
                   rs.getDouble("amount"),
                   rs.getTimestamp("created_at")
               });
           }
       } catch (Exception ex) {
           ex.printStackTrace();
           JOptionPane.showMessageDialog(this, "Lỗi tải đặt vé (booking_local): " + ex.getMessage());
       }

    }

    // Delete selected booking row (booking_local.id)
    private void deleteSelectedBooking() {
        int r = tblBookings.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Chọn 1 dòng đặt vé để xóa"); return; }
        int id = Integer.parseInt(String.valueOf(modelBookings.getValueAt(r, 0)));
        int conf = JOptionPane.showConfirmDialog(this, "Xóa đặt vé id="+id+" (giải phóng ghế)?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM booking_local WHERE id = ?")) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Xóa đặt vé thành công. Ghế đã được giải phóng.");
                loadBookings();
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy đặt vé (có thể đã bị xóa trước đó).");
                loadBookings();
            }
        } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi xóa đặt vé: "+ex.getMessage()); }
    }

    // ---- Stats ----
    private void loadStats() {
        StringBuilder sb = new StringBuilder("<html><div style='padding:10px;font-family:Segoe UI;'>");
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT SUM(tong_ve - ve_con) AS da_ban, SUM(ve_con) AS con_lai FROM xuat_chieu");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                sb.append("<b>Tổng vé đã bán:</b> ").append(rs.getInt("da_ban")).append("<br>");
                sb.append("<b>Tổng vé còn lại:</b> ").append(rs.getInt("con_lai")).append("<br>");
            }
            // doanh thu theo phim (top 10)
            sb.append("<hr><b>Doanh thu theo phim (tính từ dat_ve):</b><br>");
            try (PreparedStatement p2 = conn.prepareStatement(
                    "SELECT p.ten_phim, COALESCE(SUM(d.tong_tien),0) AS doanh_thu FROM dat_ve d JOIN xuat_chieu s ON d.suat_id=s.id JOIN phim p ON s.phim_id=p.id GROUP BY p.ten_phim ORDER BY doanh_thu DESC LIMIT 10");
                 ResultSet r2 = p2.executeQuery()) {
                while (r2.next()) {
                    sb.append(r2.getString("ten_phim")).append(": ").append(r2.getDouble("doanh_thu")).append(" VND<br>");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sb.append("Lỗi thống kê: ").append(ex.getMessage());
        }
        sb.append("</div></html>");
        lblStats.setText(sb.toString());
    }

    // ----------------- Helpers -----------------
    private void chooseImageFor(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            target.setText(f.getAbsolutePath());
        }
    }

    private void enableDragDrop(JTextField textField) {
        new DropTarget(textField, new DropTargetListener() {
            @Override public void dragEnter(DropTargetDragEvent dtde) {}
            @Override public void dragOver(DropTargetDragEvent dtde) {}
            @Override public void dropActionChanged(DropTargetDragEvent dtde) {}
            @Override public void dragExit(DropTargetEvent dte) {}
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> droppedFiles = (java.util.List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!droppedFiles.isEmpty()) {
                        textField.setText(droppedFiles.get(0).getAbsolutePath());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    // -------------------- Small inner helper types --------------------
    private static class MovieItem {
        int id; String name;
        MovieItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString(){ return name; }
    }
    private static class RoomItem {
        int id; String name;
        RoomItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString(){ return name; }
    }

    // -------------------- main --------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // optional: set look and feel native
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            QuanlyUI ui = new QuanlyUI();
            ui.setVisible(true);
        });
    }
}
