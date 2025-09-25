package quanly;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * QuanlyUI (fixed)
 * - Gửi request khớp server (cổng 2039)
 * - GET_SHOWS_ALL để lấy tất cả suất chiếu
 * - ADD_MOVIE gửi: ADD_MOVIE;ten;theLoai;thoiLuong;anh;moTa
 * - ADD_SHOW gửi:  ADD_SHOW;phimId;phongId;ngayGio;tongVe;giaVe
 * - Tab phòng chiếu: chọn 1 suất chiếu -> gọi CHECK_ROOM_STATUS;suat_id -> hiển thị grid (mô phỏng)
 *
 * LƯU Ý:
 * - Click ghế chỉ thay đổi UI (giả lập). Để persist cần bổ sung API UPDATE_SEAT trên server.
 * - Một số lệnh admin (GET_USERS, GET_BOOKINGS, GET_STATS) phụ thuộc server có implement hay không.
 */
public class QuanlyUI extends JFrame {
    private JTable tblMovies, tblSchedules, tblUsers, tblBookings;
    private DefaultTableModel modelMovies, modelSchedules, modelUsers, modelBookings;
    private JComboBox<String> cbShows; // chọn suất chiếu (thay vì chọn phòng)
    private JPanel seatPanel;
    private JLabel lblStats;

    // Fields thêm phim/lịch chiếu
    private JTextField txtMovieId, txtMovieName, txtGenre, txtDuration, txtMovieImage;
    private JTextField txtScheduleId, txtScheduleMovie, txtScheduleRoom, txtScheduleDatetime,
            txtScheduleSeats, txtSchedulePrice, txtScheduleImage;

    // giữ mapping showId -> display text (để cbShows)
    private java.util.List<Integer> showIds = new ArrayList<>();

    public QuanlyUI() {
        setTitle("🎬 Admin Panel - Quản lý hệ thống đặt vé");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // ================== Tab Quản lý Phim ==================
        JPanel moviePanel = new JPanel(new BorderLayout(10,10));
        modelMovies = new DefaultTableModel(new String[]{"ID","Tên Phim","Thể Loại","Độ dài","Ảnh"},0);
        tblMovies = new JTable(modelMovies);
        moviePanel.add(new JScrollPane(tblMovies), BorderLayout.CENTER);

        JPanel addMoviePanel = new JPanel(new GridLayout(6,2,10,10));
        // txtMovieId không cần thiết (DB auto-increment) nhưng giữ để hiển thị/edit nếu muốn
        txtMovieId = new JTextField();
        txtMovieName = new JTextField();
        txtGenre = new JTextField();
        txtDuration = new JTextField();
        txtMovieImage = new JTextField();

        addMoviePanel.add(new JLabel("ID Phim (không bắt buộc):")); addMoviePanel.add(txtMovieId);
        addMoviePanel.add(new JLabel("Tên Phim:")); addMoviePanel.add(txtMovieName);
        addMoviePanel.add(new JLabel("Thể Loại:")); addMoviePanel.add(txtGenre);
        addMoviePanel.add(new JLabel("Độ dài (phút):")); addMoviePanel.add(txtDuration);
        addMoviePanel.add(new JLabel("Ảnh (path/url):")); addMoviePanel.add(txtMovieImage);

        JButton btnAddMovie = new JButton("Thêm Phim");
        addMoviePanel.add(btnAddMovie);
        moviePanel.add(addMoviePanel, BorderLayout.NORTH);

        btnAddMovie.addActionListener(e -> addMovie());
        tabbedPane.addTab("Quản lý Phim", moviePanel);

        // ================== Tab Lịch Chiếu ==================
        JPanel schedulePanel = new JPanel(new BorderLayout(10,10));
        modelSchedules = new DefaultTableModel(new String[]{"ID","Phim","NgàyGiờ","Phòng","Số Vé","Giá Vé"},0);
        tblSchedules = new JTable(modelSchedules);
        schedulePanel.add(new JScrollPane(tblSchedules), BorderLayout.CENTER);

        JPanel addSchedulePanel = new JPanel(new GridLayout(8,2,10,10));
        // Trường hợp thêm suất chiếu, cần phim_id (số), phòng_id (số), ngay_gio (YYYY-MM-DD HH:MM:SS), tong_ve, gia_ve
        txtScheduleMovie = new JTextField(); // nhập phim_id
        txtScheduleRoom = new JTextField();  // nhập phong_id (1..10)
        txtScheduleDatetime = new JTextField(); // ví dụ: 2025-10-01 19:00:00
        txtScheduleSeats = new JTextField(); // tong_ve
        txtSchedulePrice = new JTextField(); // gia_ve
        txtScheduleImage = new JTextField();

        addSchedulePanel.add(new JLabel("Phim (phim_id):")); addSchedulePanel.add(txtScheduleMovie);
        addSchedulePanel.add(new JLabel("Phòng (phong_id 1..10):")); addSchedulePanel.add(txtScheduleRoom);
        addSchedulePanel.add(new JLabel("Ngày giờ (YYYY-MM-DD HH:MM:SS):")); addSchedulePanel.add(txtScheduleDatetime);
        addSchedulePanel.add(new JLabel("Số Vé (tổng):")); addSchedulePanel.add(txtScheduleSeats);
        addSchedulePanel.add(new JLabel("Giá Vé:")); addSchedulePanel.add(txtSchedulePrice);
        addSchedulePanel.add(new JLabel("Ảnh (không dùng):")); addSchedulePanel.add(txtScheduleImage);

        JButton btnAddSchedule = new JButton("Thêm Lịch Chiếu");
        addSchedulePanel.add(btnAddSchedule);
        schedulePanel.add(addSchedulePanel, BorderLayout.NORTH);

        btnAddSchedule.addActionListener(e -> addSchedule());
        tabbedPane.addTab("Lịch Chiếu", schedulePanel);

        // ================== Tab Phòng Chiếu (chọn suất chiếu) ==================
        JPanel roomPanel = new JPanel(new BorderLayout(10,10));
        cbShows = new JComboBox<>();
        roomPanel.add(cbShows, BorderLayout.NORTH);

        seatPanel = new JPanel();
        JScrollPane scrollSeats = new JScrollPane(seatPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        roomPanel.add(scrollSeats, BorderLayout.CENTER);

        cbShows.addActionListener(e -> loadSeatsForSelectedShow());
        tabbedPane.addTab("Phòng Chiếu (Suất)", roomPanel);

        // ================== Tab Người Dùng ==================
        JPanel userPanel = new JPanel(new BorderLayout());
        modelUsers = new DefaultTableModel(new String[]{"Tên","Email","SĐT"},0);
        tblUsers = new JTable(modelUsers);
        userPanel.add(new JScrollPane(tblUsers), BorderLayout.CENTER);
        JButton btnLoadUsers = new JButton("Tải danh sách user");
        btnLoadUsers.addActionListener(e -> loadUsers());
        userPanel.add(btnLoadUsers, BorderLayout.NORTH);
        tabbedPane.addTab("Người Dùng", userPanel);

        // ================== Tab Đặt Vé ==================
        JPanel bookingPanel = new JPanel(new BorderLayout());
        modelBookings = new DefaultTableModel(new String[]{"ID","User","Phim","NgàyGiờ","Số Vé","Tổng tiền","Trạng thái"},0);
        tblBookings = new JTable(modelBookings);
        bookingPanel.add(new JScrollPane(tblBookings), BorderLayout.CENTER);
        JButton btnLoadBookings = new JButton("Tải tất cả đặt vé (admin)");
        btnLoadBookings.addActionListener(e -> loadAllBookings());
        bookingPanel.add(btnLoadBookings, BorderLayout.NORTH);
        tabbedPane.addTab("Đặt Vé", bookingPanel);

        // ================== Tab Thống Kê ==================
        JPanel statsPanel = new JPanel(new BorderLayout());
        lblStats = new JLabel();
        lblStats.setFont(new Font("Segoe UI",Font.BOLD,16));
        statsPanel.add(new JScrollPane(lblStats), BorderLayout.CENTER);
        JButton btnLoadStats = new JButton("Cập nhật Thống kê");
        btnLoadStats.addActionListener(e -> loadStats());
        statsPanel.add(btnLoadStats, BorderLayout.NORTH);
        tabbedPane.addTab("Thống kê", statsPanel);

        add(tabbedPane);

        // Load dữ liệu ban đầu
        SwingUtilities.invokeLater(() -> {
            loadMovies();
            loadSchedules();    // sẽ điền bảng và cbShows
        });
    }

    // ================== Phương thức ==================
    private void addMovie(){
        String name = txtMovieName.getText().trim();
        String genre = txtGenre.getText().trim();
        String duration = txtDuration.getText().trim();
        String image = txtMovieImage.getText().trim();
        String moTa = ""; // không có trường mô tả trong UI, để rỗng

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên phim không được để trống");
            return;
        }

        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Gửi theo format server yêu cầu
            out.println("ADD_MOVIE;" + name + ";" + genre + ";" + duration + ";" + image + ";" + moTa);

            // Đọc phản hồi (server sẽ gửi ...\nEND)
            StringBuilder resp = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                resp.append(line).append("\n");
            }
            JOptionPane.showMessageDialog(this, resp.toString());
            loadMovies();
            loadSchedules(); // cập nhật nếu cần

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm phim: " + ex.getMessage());
        }
    }

    private void addSchedule(){
        String phimId = txtScheduleMovie.getText().trim();
        String phongId = txtScheduleRoom.getText().trim();
        String ngayGio = txtScheduleDatetime.getText().trim();
        String seats = txtScheduleSeats.getText().trim();
        String price = txtSchedulePrice.getText().trim();

        if (phimId.isEmpty() || phongId.isEmpty() || ngayGio.isEmpty() || seats.isEmpty() || price.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin suất chiếu");
            return;
        }

        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Server expects: ADD_SHOW;phimId;phongId;ngayGio;tongVe;giaVe
            out.println("ADD_SHOW;" + phimId + ";" + phongId + ";" + ngayGio + ";" + seats + ";" + price);

            StringBuilder resp = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                resp.append(line).append("\n");
            }
            JOptionPane.showMessageDialog(this, resp.toString());
            loadSchedules();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm suất chiếu: " + ex.getMessage());
        }
    }

    private void loadMovies(){
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_MOVIES");
            modelMovies.setRowCount(0);
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                if (line.startsWith("ERR;")) {
                    // show lỗi nếu muốn
                    System.out.println("Server: " + line);
                    continue;
                }
                String[] parts = line.split(";", -1);
                // đảm bảo đủ cột
                if (parts.length < 5) continue;
                modelMovies.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3], parts[4]});
            }

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadSchedules(){
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_SHOWS_ALL"); // lấy tất cả suất chiếu
            modelSchedules.setRowCount(0);
            cbShows.removeAllItems();
            showIds.clear();

            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                if (line.startsWith("ERR;")) {
                    System.out.println("Server: " + line);
                    continue;
                }
                String[] parts = line.split(";", -1);
                // server trả: id;ten_phim;ngay_gio;phong_id;tong_ve;gia_ve
                if (parts.length < 6) continue;
                String id = parts[0];
                String ten = parts[1];
                String ngaygio = parts[2];
                String phong = parts[3];
                String tongve = parts[4];
                String giave = parts[5];

                modelSchedules.addRow(new Object[]{id, ten, ngaygio, phong, tongve, giave});

                // thêm vào cbShows hiển thị "id - ten - phòng - ngaygio"
                String cbText = id + " | " + ten + " | Phòng " + phong + " | " + ngaygio;
                cbShows.addItem(cbText);
                showIds.add(Integer.valueOf(id));
            }

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    /**
     * Khi chọn 1 suất trong cbShows -> gọi CHECK_ROOM_STATUS;suat_id
     * Server trả "OK;booked;remain" (theo server hiện tại).
     * Dùng = booked + remain = total, vẽ grid mô phỏng: đánh dấu booked số ghế đầu.
     */
    private void loadSeatsForSelectedShow() {
        seatPanel.removeAll();

        int idx = cbShows.getSelectedIndex();
        if (idx < 0 || idx >= showIds.size()) {
            seatPanel.revalidate();
            seatPanel.repaint();
            return;
        }
        int suatId = showIds.get(idx);

        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("CHECK_ROOM_STATUS;" + suatId);
            ArrayList<String> lines = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                lines.add(line);
            }
            if (lines.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Server không trả dữ liệu trạng thái phòng.");
                return;
            }
            // parse first line (server should return something like "OK;booked;remain" or "ERR;...")
            String first = lines.get(0);
            if (first.startsWith("ERR;")) {
                JOptionPane.showMessageDialog(this, first);
                return;
            }
            String[] parts = first.split(";", -1);
            if (parts.length < 3 || !"OK".equals(parts[0])) {
                JOptionPane.showMessageDialog(this, "Dữ liệu trạng thái không đúng: " + first);
                return;
            }
            int booked = Integer.parseInt(parts[1]);
            int remain = Integer.parseInt(parts[2]);
            int total = booked + remain;

            // layout: cố định 20 cột (như trước)
            int cols = 20;
            int rows = (int) Math.ceil((double) total / cols);
            seatPanel.setLayout(new GridLayout(rows, cols, 3, 3));
            seatPanel.setPreferredSize(new Dimension(Math.min(1200, cols*40), rows*40));

            // tạo button ghế: ghế index < booked => đã bán
            for (int i = 0; i < rows * cols; i++) {
                if (i >= total) {
                    JButton empty = new JButton("");
                    empty.setEnabled(false);
                    seatPanel.add(empty);
                    continue;
                }
                boolean isBooked = i < booked;
                JButton b = new JButton(isBooked ? "X" : "O");
                b.setBackground(isBooked ? Color.RED : Color.GREEN);
                b.setForeground(Color.WHITE);
                b.setFont(new Font("Segoe UI", Font.BOLD, 12));
                // click -> toggle trạng thái trên UI (giả lập admin đặt/hủy) - không gọi server
                b.addActionListener(evt -> {
                    boolean nowBooked = b.getText().equals("O");
                    b.setText(nowBooked ? "X" : "O");
                    b.setBackground(nowBooked ? Color.RED : Color.GREEN);
                });
                seatPanel.add(b);
            }

            seatPanel.revalidate();
            seatPanel.repaint();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi lấy trạng thái ghế: " + ex.getMessage());
        }
    }

    private void loadUsers(){
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_USERS");
            modelUsers.setRowCount(0);
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                if (line.startsWith("ERR;")) {
                    JOptionPane.showMessageDialog(this, line);
                    break;
                }
                String[] parts = line.split(";", -1);
                // expected "ten;email;sdt"
                if (parts.length < 3) continue;
                modelUsers.addRow(new Object[]{parts[0], parts[1], parts[2]});
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadAllBookings(){
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_BOOKINGS_ALL"); // note: server may expect a different command; if not implemented server will return ERR
            modelBookings.setRowCount(0);
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                if (line.startsWith("ERR;")) {
                    JOptionPane.showMessageDialog(this, line);
                    break;
                }
                String[] parts = line.split(";", -1);
                // expected: id;user;phim;ngaygio;so_luong;tong_tien;trang_thai
                if (parts.length < 7) continue;
                modelBookings.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]});
            }

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadStats(){
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_STATS");
            StringBuilder sb = new StringBuilder("<html><div style='padding:10px'>");
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                sb.append(line).append("<br>");
            }
            sb.append("</div></html>");
            lblStats.setText(sb.toString());
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new QuanlyUI().setVisible(true));
    }
}
