package web;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainUI extends JFrame {
    private final String currentUser; // username (string). Nếu cần userId, sửa lại để lưu id.

    // Fields đặt vé (trong tab Đặt vé)
    private final JTextField txtMovieId = new JTextField();
    private final JTextField txtMovieName = new JTextField();
    private final JTextField txtGenre = new JTextField();
    private final JTextField txtRoom = new JTextField();
    private final JTextField txtDatetime = new JTextField();
    private final JTextField txtPrice = new JTextField();
    private final JTextField txtSeat = new JTextField(); // sẽ được cập nhật khi chọn ghế

    private final JButton btnBook = new JButton("Đặt Vé");
    private final JButton btnHistory = new JButton("Xem lịch sử đặt vé");
    private final JButton btnPay = new JButton("Thanh toán (giả định)");
    private final JButton btnQR = new JButton("Quét QR (Giả định)");
    private final JButton btnBank = new JButton("Chuyển khoản (Giả định)");

    private final JTextArea txtHistory = new JTextArea();

    // Panel hiển thị danh sách phim (grid)
    private final JPanel moviePanel = new JPanel();

    public MainUI(String user) {
        this.currentUser = user;
        setTitle("🎬 Hệ thống đặt vé xem phim - User: " + currentUser);
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // ============== TAB Trang chủ ===========
        JPanel homePanel = new JPanel(new BorderLayout());
        // banner nếu có (nếu không có file, sẽ không hiển thị là ok)
        try {
            JLabel banner = new JLabel(new ImageIcon("banner.jpg"));
            homePanel.add(banner, BorderLayout.NORTH);
        } catch (Exception ignored) {}

        moviePanel.setLayout(new WrapLayout(FlowLayout.LEFT, 20, 20));
        moviePanel.setBackground(Color.WHITE);
        JScrollPane scrollMovies = new JScrollPane(moviePanel);
        scrollMovies.setBorder(BorderFactory.createEmptyBorder());
        homePanel.add(scrollMovies, BorderLayout.CENTER);

        tabbedPane.addTab("Trang chủ", homePanel);

        // ============== TAB Đặt vé ============
        JPanel bookPanel = new JPanel();
        bookPanel.setLayout(new BoxLayout(bookPanel, BoxLayout.Y_AXIS));
        bookPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        bookPanel.setBackground(Color.WHITE);

        // các field chỉ để hiển thị (không cho người dùng sửa, vì dữ liệu lấy từ server)
        setFieldReadonly(txtMovieId);
        setFieldReadonly(txtMovieName);
        setFieldReadonly(txtGenre);
        setFieldReadonly(txtRoom);
        setFieldReadonly(txtDatetime);
        setFieldReadonly(txtPrice);
        setFieldReadonly(txtSeat);

        bookPanel.add(createFieldPanel("ID Phim:", txtMovieId));
        bookPanel.add(createFieldPanel("Tên Phim:", txtMovieName));
        bookPanel.add(createFieldPanel("Thể Loại:", txtGenre));
        bookPanel.add(createFieldPanel("Phòng Chiếu:", txtRoom));
        bookPanel.add(createFieldPanel("Ngày Giờ Chiếu:", txtDatetime));
        bookPanel.add(createFieldPanel("Giá Vé:", txtPrice));
        bookPanel.add(createFieldPanel("Ghế đã chọn:", txtSeat));

        // nút chọn ghế & đặt vé; chọn ghế sẽ mở dialog seat chooser
        JButton btnChooseSeats = new JButton("Chọn Ghế & Đặt Vé");
        styleButton(btnChooseSeats, new Color(13, 110, 253));
        btnChooseSeats.setAlignmentX(Component.CENTER_ALIGNMENT);
        bookPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        bookPanel.add(btnChooseSeats);

        tabbedPane.addTab("Đặt vé", bookPanel);

        // ============== TAB Lịch sử ============
        JPanel historyPanel = new JPanel(new BorderLayout(10,10));
        txtHistory.setEditable(false);
        txtHistory.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scrollHistory = new JScrollPane(txtHistory);
        historyPanel.add(scrollHistory, BorderLayout.CENTER);

        JButton btnLoadHistory = new JButton("Xem lịch sử đặt vé");
        styleButton(btnLoadHistory, new Color(40, 167, 69));
        historyPanel.add(btnLoadHistory, BorderLayout.SOUTH);

        tabbedPane.addTab("Lịch sử vé", historyPanel);

        // ============== TAB Thanh toán ============
        JPanel paymentPanel = new JPanel();
        paymentPanel.setLayout(new BoxLayout(paymentPanel, BoxLayout.Y_AXIS));
        paymentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        paymentPanel.setBackground(Color.WHITE);

        JLabel lblPay = new JLabel("Chọn phương thức thanh toán:");
        lblPay.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblPay.setAlignmentX(Component.CENTER_ALIGNMENT);
        paymentPanel.add(lblPay);
        paymentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        styleButton(btnQR, new Color(255, 193, 7));
        btnQR.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        paymentPanel.add(btnQR);
        paymentPanel.add(Box.createRigidArea(new Dimension(0,10)));

        styleButton(btnBank, new Color(23, 162, 184));
        btnBank.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        paymentPanel.add(btnBank);
        paymentPanel.add(Box.createRigidArea(new Dimension(0,10)));

        styleButton(btnPay, new Color(40, 167, 69));
        btnPay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        paymentPanel.add(btnPay);

        tabbedPane.addTab("Thanh toán", paymentPanel);

        add(tabbedPane);

        // ============== Sự kiện ===================
        btnChooseSeats.addActionListener(e -> {
            String suatId = txtMovieId.getText().trim();
            String giaStr = txtPrice.getText().trim();
            String tongVeStr = "100"; // giả sử, hoặc bạn phải lấy từ server/field nào đó

            if (suatId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn phim/suất trước (ở tab Trang chủ).");
                return;
            }

            try {
                int tongVe = Integer.parseInt(tongVeStr);
                double giaVe = Double.parseDouble(giaStr);
                openSeatSelectionDialog(suatId, tongVe, giaVe);  // ✅ gọi hàm đã có
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở chọn ghế vì thiếu dữ liệu.");
            }
        });

        btnLoadHistory.addActionListener(e -> loadHistory());
        btnPay.addActionListener(e -> JOptionPane.showMessageDialog(this, "Thanh toán xác nhận (giả định)."));

        // Load movies lên trang chủ
        SwingUtilities.invokeLater(this::loadMoviesFromServer);
    }

    // Helper: set textfield read-only style
    private void setFieldReadonly(JTextField tf){
        tf.setEditable(false);
        tf.setBackground(new Color(0xF5F7FA));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    private JPanel createFieldPanel(String label, JTextField textField){
        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        panel.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(lbl, BorderLayout.WEST);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(textField, BorderLayout.CENTER);
        panel.add(Box.createRigidArea(new Dimension(0,5)), BorderLayout.SOUTH);
        return panel;
    }

    private void styleButton(JButton btn, Color bg){
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
    }

    // ================== Lấy danh sách phim từ server ==================
    private void loadMoviesFromServer() {
        List<String[]> movies = new ArrayList<>();
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_MOVIES");
            // đọc dòng đến END (server của bạn thường gửi END)
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END")) break;
                // bỏ qua tiền tố OK;... (nếu có)
                if (line.startsWith("OK;") || line.startsWith("ERR;")) {
                    // nếu "OK;count" thì tiếp tục đọc các dòng tiếp theo
                    if (line.contains(";")) {
                        String after = line.substring(line.indexOf(';') + 1);
                        // nếu phần sau là số -> OK;count, skip and continue
                        try { Integer.parseInt(after); continue; } catch (Exception ignored) {}
                    }
                    // nếu dòng chứa dữ liệu phim thì cũng parse tiếp
                }
                String[] parts = line.split(";");
                // server có thể trả: id;ten;the_loai;thoi_luong;anh   (5 columns)
                // ta gắng làm sao để đưa vào card (hiển thị cơ bản)
                movies.add(parts);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách phim từ server!");
        }

        updateMovieGrid(movies);
    }

    private void updateMovieGrid(List<String[]> movies){
        moviePanel.removeAll();
        moviePanel.setLayout(new WrapLayout(FlowLayout.LEFT, 20,20));
        for (String[] m : movies) {
            moviePanel.add(createMovieCard(m));
        }
        moviePanel.revalidate();
        moviePanel.repaint();
    }

    /**
     * Tạo card hiển thị phim. Dữ liệu dòng 'movie' có thể khác nhau tùy server.
     * - Nếu parts length >= 2: parts[0]=id, parts[1]=ten
     * - Nếu server cung cấp thêm fields thì cố gắng hiển thị.
     */
    private JPanel createMovieCard(String[] movie){
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(220,320));
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        String id = movie.length>0 ? movie[0] : "";
        String ten = movie.length>1 ? movie[1] : "No title";
        String theLoai = movie.length>2 ? movie[2] : "";
        String thoiLuong = movie.length>3 ? movie[3] : "";
        String anh = movie.length>4 ? movie[4] : "";

        // poster (nếu có)
        JLabel poster;
        try {
            poster = new JLabel(new ImageIcon("posters/" + id + ".jpg"));
        } catch (Exception ex) {
            poster = new JLabel();
        }
        poster.setPreferredSize(new Dimension(180,180));
        poster.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblName = new JLabel("<html><b>"+ten+"</b></html>");
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblGenre = new JLabel(theLoai);
        lblGenre.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTime = new JLabel((thoiLuong==null || thoiLuong.isEmpty()) ? "" : (thoiLuong + " phút"));
        lblTime.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnShows = new JButton("Xem suất & chọn ghế");
        styleButton(btnShows, new Color(13,110,253));
        btnShows.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnShows.addActionListener(e -> openShowsDialog(id, ten));

        // add components
        card.add(Box.createRigidArea(new Dimension(0,6)));
        card.add(poster);
        card.add(Box.createRigidArea(new Dimension(0,8)));
        card.add(lblName);
        card.add(Box.createRigidArea(new Dimension(0,4)));
        card.add(lblGenre);
        card.add(Box.createRigidArea(new Dimension(0,4)));
        card.add(lblTime);
        card.add(Box.createVerticalGlue());
        card.add(btnShows);
        card.add(Box.createRigidArea(new Dimension(0,6)));

        return card;
    }

    // ================== Dialog chọn suất chiếu (GET_SHOWS;movieId) ==================
    private void openShowsDialog(String movieId, String movieTitle) {
        // tạo dialog
        JDialog dlg = new JDialog(this, "Suất chiếu: " + movieTitle, true);
        dlg.setSize(700, 400);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10,10));

        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<String[]> shows = new ArrayList<>(); // lưu từng show row (string array)
        JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(list);
        dlg.add(sp, BorderLayout.CENTER);

        // load shows từ server
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_SHOWS;" + movieId);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END")) break;
                if (line.startsWith("ERR;")) continue;
                if (line.startsWith("OK;")) {
                    // nếu dòng 'OK;count' thì skip
                    continue;
                }
                String[] parts = line.split(";");
                // server expected: showId;ten_phim;ngay_gio;phong_id;tong_ve;gia_ve
                shows.add(parts);
                // prepare display text
                String display;
                if (parts.length >= 6) {
                    display = String.format("Suất %s | %s | Phòng %s | Vé: %s | Giá: %s",
                            parts[0], parts[2], parts[3], parts[4], parts[5]);
                } else {
                    display = String.join(" | ", parts);
                }
                listModel.addElement(display);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể tải suất chiếu từ server.");
            return;
        }

        // nút chọn suất
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnChoose = new JButton("Chọn suất");
        pnlBottom.add(btnChoose);
        dlg.add(pnlBottom, BorderLayout.SOUTH);

        btnChoose.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx < 0) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn một suất.");
                return;
            }
            String[] sel = shows.get(idx);
            // nếu server trả theo dạng: id;ten;ngay_gio;phong;tong_ve;gia
            // ta điền các field trong tab Đặt vé:
            if (sel.length >= 6) {
                // showId
                String suatId = sel[0];
                String ngayGio = sel[2];
                String phong = sel[3];
                String tongVe = sel[4];
                String gia = sel[5];

                // LƯU: chúng ta dùng txtMovieId để lưu suatId (vì booking cần suatId)
                txtMovieId.setText(suatId);
                txtMovieName.setText(movieTitle);
                // giữ thể loại trống (bởi chúngta chỉ có movieId -> need GET_MOVIES if want genre)
                txtGenre.setText("");
                txtRoom.setText(phong);
                txtDatetime.setText(ngayGio);
                txtPrice.setText(gia);
                txtSeat.setText(""); // reset

                // mở dialog chọn ghế dựa trên suatId và tongVe
                dlg.dispose();
                openSeatSelectionDialog(suatId, Integer.parseInt(tongVe), Double.parseDouble(gia));
            } else {
                // fallback: nếu format khác, hiển thị lên các field chung
                txtMovieId.setText(sel.length>0?sel[0]:"");
                txtMovieName.setText(sel.length>1?sel[1]:movieTitle);
                txtDatetime.setText(sel.length>2?sel[2]:"");
                txtRoom.setText(sel.length>3?sel[3]:"");
                txtPrice.setText(sel.length>4?sel[4]:"");
                dlg.dispose();
            }
        });

        dlg.setVisible(true);
    }

    // ================== Dialog chọn ghế ==================
    // Nếu server hỗ trợ GET_SEATS;suatId -> nên trả về trạng thái từng ghế (0/1) hoặc list ghế đã đặt.
    // Nếu không có, ta dùng tổng vé (tongVe) + CHECK_ROOM_STATUS để disable một số ghế đầu tiên (xấp xỉ).
    private void openSeatSelectionDialog(String suatId, int tongVe, double giaVe) {
        // lấy thông tin ghế đã đặt từ server nếu có (GET_SEATS;suatId)
        List<String> bookedSeats = new ArrayList<>(); // chứa seat labels (ví dụ A1,B5...) nếu server trả
        boolean haveSeatLevelInfo = false;

        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_SEATS;" + suatId);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END")) break;
                if (line.startsWith("ERR;")) { bookedSeats.clear(); break; }
                // server có thể trả:
                // - danh sách seat labels đã đặt (A1, A2, B1...) -> trong mỗi line
                // - hoặc dòng "0" / "1" cho mỗi seat -> không xử lý ở đây
                if (line.contains(",")) {
                    // nếu server trả "A1,A2,..." trong 1 dòng
                    String[] arr = line.split(",");
                    for (String s : arr) if (!s.trim().isEmpty()) bookedSeats.add(s.trim());
                    haveSeatLevelInfo = true;
                } else if (line.matches("[A-Za-z]+\\d+")) {
                    bookedSeats.add(line.trim());
                    haveSeatLevelInfo = true;
                } else if (line.equals("0") || line.equals("1")) {
                    // fallback: server trả theo chỉ số 0/1 (mỗi dòng). => we'll ignore and fallback to count-based.
                    haveSeatLevelInfo = false;
                    bookedSeats.clear();
                    break;
                } else {
                    // other unrecognized -> ignore
                }
            }
        } catch (IOException ignored) {
            // server không hỗ trợ GET_SEATS -> fallback
        }

        // nếu không có seat-level info, lấy số đã bán bằng CHECK_ROOM_STATUS
        int soldCount = 0;
        if (!haveSeatLevelInfo) {
            try (Socket socket = new Socket("localhost", 2039);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("CHECK_ROOM_STATUS;" + suatId); // server trả OK;tong;booked;remain
                String line = in.readLine();
                if (line != null && line.startsWith("OK")) {
                    String[] p = line.split(";");
                    if (p.length >= 3) {
                        try { soldCount = Integer.parseInt(p[2]); } catch (Exception ex) { soldCount = 0; }
                        // but server earlier returned OK;booked;remain in some versions, handle both:
                        // if p[1] is total and p[2] is booked, use p[2]
                        if (p.length >= 4) {
                            // maybe OK;total;booked;remain
                            try { soldCount = Integer.parseInt(p[2]); } catch (Exception ignore) {}
                        }
                    }
                }
            } catch (IOException ignored) {}
        }

        // build dialog with seat grid
        JDialog dlg = new JDialog(this, "Chọn ghế - Suất " + suatId, true);
        dlg.setSize(900, 600);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(8,8));

        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTop.add(new JLabel("Suất ID: " + suatId + " | Tổng vé: " + tongVe + " | Giá 1 vé: " + giaVe + " VND"));
        dlg.add(pnlTop, BorderLayout.NORTH);

        // seat grid
        JPanel gridWrap = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(gridWrap);
        dlg.add(scroll, BorderLayout.CENTER);

        // determine columns/rows (ví dụ cột = 10)
        int cols = 10;
        int rows = (int)Math.ceil((double)tongVe / cols);

        JPanel seatGrid = new JPanel(new GridLayout(rows, cols, 4, 4));
        seatGrid.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // create buttons and keep references
        List<JToggleButton> seatButtons = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            char rowLetter = (char)('A' + r);
            for (int c = 1; c <= cols; c++) {
                int idx = r * cols + (c - 1);
                if (idx >= tongVe) {
                    // filler invisible button for last row
                    JPanel filler = new JPanel();
                    seatGrid.add(filler);
                    continue;
                }
                String seatLabel = String.format("%c%d", rowLetter, c);
                JToggleButton b = new JToggleButton(seatLabel);
                b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                // if have seat-level info and seatLabel in bookedSeats => disable
                if (haveSeatLevelInfo && bookedSeats.contains(seatLabel)) {
                    b.setEnabled(false);
                    b.setBackground(Color.RED);
                    b.setForeground(Color.WHITE);
                } else if (!haveSeatLevelInfo && soldCount > 0) {
                    // disable some first seats as 'booked' approximation
                    if (idx < soldCount) {
                        b.setEnabled(false);
                        b.setBackground(Color.RED);
                        b.setForeground(Color.WHITE);
                    } else {
                        b.setBackground(Color.GREEN);
                        b.setForeground(Color.WHITE);
                    }
                } else {
                    b.setBackground(Color.GREEN);
                    b.setForeground(Color.WHITE);
                }
                seatButtons.add(b);
                seatGrid.add(b);
            }
        }

        gridWrap.add(seatGrid, BorderLayout.CENTER);

        // right panel: summary & actions
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
        pnlRight.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JLabel lblSelected = new JLabel("Chưa chọn ghế");
        lblSelected.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlRight.add(lblSelected);
        pnlRight.add(Box.createRigidArea(new Dimension(0,10)));

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 10, 1);
        JSpinner spQty = new JSpinner(spinnerModel);
        spQty.setMaximumSize(new Dimension(100, 30));
        pnlRight.add(new JLabel("Số lượng vé muốn mua:"));
        pnlRight.add(spQty);
        pnlRight.add(Box.createRigidArea(new Dimension(0,10)));

        JLabel lblTotal = new JLabel("Tổng tiền: 0 VND");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlRight.add(lblTotal);
        pnlRight.add(Box.createRigidArea(new Dimension(0,20)));

        JButton btnConfirm = new JButton("Đặt " + giaVe + " x");
        styleButton(btnConfirm, new Color(13,110,253));
        btnConfirm.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlRight.add(btnConfirm);
        pnlRight.add(Box.createRigidArea(new Dimension(0,8)));

        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(a -> dlg.dispose());
        pnlRight.add(btnCancel);

        dlg.add(pnlRight, BorderLayout.EAST);

        // cập nhật selected/tổng tiền khi click ghế hoặc thay đổi số lượng
        Runnable updateSummary = () -> {
            List<String> sel = seatButtons.stream()
                    .filter(AbstractButton::isSelected)
                    .map(AbstractButton::getText)
                    .collect(Collectors.toList());
            if (sel.isEmpty()) {
                lblSelected.setText("Chưa chọn ghế");
            } else {
                lblSelected.setText("Ghế đã chọn: " + String.join(", ", sel));
            }
            int qty = sel.size();
            double total = qty * giaVe;
            lblTotal.setText(String.format("Tổng tiền: %,.0f VND", total));
            btnConfirm.setText("Đặt (" + qty + " ghế) - " + String.format("%,.0f VND", total));
        };

        for (JToggleButton b : seatButtons) {
            b.addItemListener(ev -> updateSummary.run());
        }
        spQty.addChangeListener(ev -> {
            // spinner chỉ informative here (limit seat selection), we enforce selection count on confirm
            updateSummary.run();
        });

     // === Thay trong openSeatSelectionDialog(...) ===
        btnConfirm.addActionListener(ev -> {
            List<String> selected = seatButtons.stream()
                    .filter(AbstractButton::isSelected)
                    .map(AbstractButton::getText)
                    .collect(Collectors.toList());

            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn ít nhất một ghế.");
                return;
            }

            String seatList = String.join(",", selected);

            // Gửi yêu cầu BOOK_TICKET tới server
            try (Socket socket = new Socket("localhost", 2039);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("BOOK_TICKET;" + suatId + ";" + currentUser + ";" + seatList);
                String resp = in.readLine();

                if (resp != null && resp.startsWith("OK")) {
                    txtSeat.setText(seatList);

                    // Hiển thị dialog QR + bank
                    JPanel payPanel = new JPanel();
                    payPanel.setLayout(new BoxLayout(payPanel, BoxLayout.Y_AXIS));

                    JLabel lblQR = new JLabel(new ImageIcon("qr.png")); // file qr giả định
                    lblQR.setAlignmentX(Component.CENTER_ALIGNMENT);

                    JLabel lblBank = new JLabel("Số tài khoản: 123456789 - Ngân hàng ABC");
                    lblBank.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    lblBank.setAlignmentX(Component.CENTER_ALIGNMENT);

                    JButton btnDone = new JButton("Xác nhận đã thanh toán");
                    btnDone.setAlignmentX(Component.CENTER_ALIGNMENT);

                    payPanel.add(lblQR);
                    payPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                    payPanel.add(lblBank);
                    payPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                    payPanel.add(btnDone);

                    JDialog payDlg = new JDialog(this, "Thanh toán", true);
                    payDlg.setSize(400, 450);
                    payDlg.setLocationRelativeTo(this);
                    payDlg.add(payPanel);
                    
                    btnDone.addActionListener(a -> {
                        JOptionPane.showMessageDialog(payDlg, "Thanh toán thành công!");
                        payDlg.dispose();
                        dlg.dispose();
                        loadHistory(); // cập nhật lịch sử vé
                    });

                    payDlg.setVisible(true);

                } else {
                    JOptionPane.showMessageDialog(dlg, "Đặt vé thất bại: " + resp);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dlg, "Lỗi kết nối server khi đặt vé.");
            }
        });


        // initial update
        updateSummary.run();

        dlg.setVisible(true);
    }
    private void showPaymentDialog(int totalAmount, List<String> selectedSeats, String suatId) {
        JDialog paymentDialog = new JDialog(this, "Thanh toán", true);
        paymentDialog.setSize(400, 500);
        paymentDialog.setLayout(new BorderLayout());

        // Hiển thị QR (bạn lưu ảnh ở thư mục images/qr.png)
        JLabel qrLabel = new JLabel(new ImageIcon("images/qr.png"), SwingConstants.CENTER);
        paymentDialog.add(qrLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.add(new JLabel("Ngân hàng: Vietcombank"));
        infoPanel.add(new JLabel("Số tài khoản: 123456789"));
        infoPanel.add(new JLabel("Số tiền: " + totalAmount + " VND"));
        paymentDialog.add(infoPanel, BorderLayout.NORTH);

        JButton confirmBtn = new JButton("Xác nhận đã chuyển khoản");
        confirmBtn.addActionListener(e -> {
            String seatsCsv = String.join(",", selectedSeats);
            boolean ok = attemptBookingWithSeats(suatId, selectedSeats.size(), seatsCsv);
            if (ok) {
                txtSeat.setText(seatsCsv);
                loadMoviesFromServer(); // cập nhật lại màu ghế
                paymentDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(paymentDialog, "Đặt vé không thành công.");
            }
        });

        paymentDialog.add(confirmBtn, BorderLayout.SOUTH);
        paymentDialog.setLocationRelativeTo(this);
        paymentDialog.setVisible(true);
    }


    /**
     * Thử gửi lệnh đặt vé đến server.
     * Gửi dạng: BOOK;username;suatId;soLuong;ghe1,ghe2...
     * Trả về true nếu server trả OK...
     */
    private boolean attemptBookingWithSeats(String suatId, int soLuong, String seatsCsv) {
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // format theo biến thể client cũ (phổ biến trong project của bạn)
            String cmd = "BOOK;" + currentUser + ";" + suatId + ";" + soLuong + ";" + seatsCsv;
            out.println(cmd);

            String line;
            StringBuilder resp = new StringBuilder();
            while ((line = in.readLine()) != null) {
                if (line.equals("END")) break;
                resp.append(line).append("\n");
            }
            String response = resp.toString().trim();
            if (response.startsWith("OK") || response.toUpperCase().contains("THANH CONG") || response.toUpperCase().contains("BOOKED")) {
                JOptionPane.showMessageDialog(this, "Đặt vé thành công!");
                return true;
            } else {
                // hiển thị lỗi trả về từ server
                JOptionPane.showMessageDialog(this, "Server trả lỗi: " + response);
                return false;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi kết nối server khi đặt vé: " + ex.getMessage());
            return false;
        }
    }

    // ================== Lấy lịch sử đặt vé của user ==================
    private void loadHistory() {
        txtHistory.setText("");
        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Gửi HISTORY;username (nhiều server cũ dùng format này)
            out.println("GET_BOOKINGS;" + currentUser); // hoặc "HISTORY;username" tùy server. Mình dùng GET_BOOKINGS;username
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("END")) break;
                if (line.startsWith("ERR;")) {
                    txtHistory.append("Lỗi: " + line + "\n");
                    continue;
                }
                txtHistory.append(line + "\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể tải lịch sử: " + ex.getMessage());
        }
    }

    // ================== WrapLayout (giữ nguyên) ==================
    public class WrapLayout extends FlowLayout{
        public WrapLayout(){ super(); }
        public WrapLayout(int align){ super(align); }
        public WrapLayout(int align,int hgap,int vgap){ super(align,hgap,vgap); }

        @Override
        public Dimension preferredLayoutSize(Container target){ return layoutSize(target,true);}
        @Override
        public Dimension minimumLayoutSize(Container target){ return layoutSize(target,false);}

        private Dimension layoutSize(Container target, boolean preferred){
            synchronized(target.getTreeLock()){
                int targetWidth = target.getWidth();
                if(targetWidth==0) targetWidth=Integer.MAX_VALUE;

                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth-(insets.left+insets.right+hgap*2);

                Dimension dim = new Dimension(0,0);
                int rowWidth=0,rowHeight=0;
                int nmembers = target.getComponentCount();
                for(int i=0;i<nmembers;i++){
                    Component m = target.getComponent(i);
                    if(m.isVisible()){
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if(rowWidth+d.width>maxWidth){
                            dim.width = Math.max(dim.width,rowWidth);
                            dim.height += rowHeight+vgap;
                            rowWidth=0;
                            rowHeight=0;
                        }
                        if(rowWidth!=0) rowWidth+=hgap;
                        rowWidth+=d.width;
                        rowHeight=Math.max(rowHeight,d.height);
                    }
                }
                dim.width=Math.max(dim.width,rowWidth);
                dim.height+=rowHeight;
                dim.width+=insets.left+insets.right+hgap*2;
                dim.height+=insets.top+insets.bottom+vgap*2;
                return dim;
            }
        }
    }

    // ================== main ==================
    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new MainUI("hihi").setVisible(true));
    }
}
