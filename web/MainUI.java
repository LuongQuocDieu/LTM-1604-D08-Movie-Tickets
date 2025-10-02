package web;

import mysql.CSDL;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.sql.*;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class MainUI extends JFrame {

    // ---------------- CONFIG ----------------
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 2040;
    private static final int SOCKET_TIMEOUT_MS = 4000;

    // ---------------- App state ----------------
    private boolean serverAvailable = false;
    private String currentUser = null; // logged-in username (or email)

    // ---------------- Fallback local data ----------------
    private final List<Movie> localMovies = new ArrayList<>();
    private final Map<String, Show> localShows = new HashMap<>();
    private final Map<Integer, List<String>> localSeatsByRoom = new HashMap<>();
    private final List<Booking> localBookings = new ArrayList<>();
    private final Map<String, LocalUser> localUsers = new HashMap<>();

    // ---------------- UI components ----------------
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JPanel homePanel = new JPanel(new BorderLayout());
    private final JPanel moviesPanel = new JPanel(new BorderLayout());
    private final JPanel showsPanel = new JPanel(new BorderLayout()); // "Xuất chiếu" (vé đã đặt)
    private final JPanel contactPanel = new JPanel(new BorderLayout());
    private final JPanel accountPanel = new JPanel(new BorderLayout());

    // Components within Movies tab (left grid + right detail)
    private final JPanel movieGridPanel = new JPanel(new WrapFlowLayout(16, 16));
    private final JPanel movieDetailPanel = new JPanel(); // will be BoxLayout Y_AXIS
    private final JLabel lblDetailTitle = new JLabel();
    private final JLabel lblDetailGenre = new JLabel();
    private final JLabel lblDetailDuration = new JLabel();
    private final JTextArea taDetailDesc = new JTextArea();
    private final JLabel lblDetailPoster = new JLabel();
    private final JButton btnDetailBook = new JButton("Đặt vé"); // must appear under duration
    private final JLabel lblWelcome = new JLabel("Xin chào: (chưa đăng nhập)");

    // Booking fields (right side small form)
    private final JTextField tfDetailShowId = new JTextField(); // hidden usage to store selected show id if necessary

    // Shows tab components
    private final JTextArea taBookedTickets = new JTextArea();

    // Account tab components
    private final JLabel lblAccountInfo = new JLabel();

    // Format currency
    private final NumberFormat currencyF = NumberFormat.getInstance(Locale.US);

    // Constructor
    public MainUI() {
        super("CinemaX - Hệ thống đặt vé xem phim");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 820);
        setLocationRelativeTo(null);

        // prepare fallback data
        prepareLocalSampleData();

        // test server availability (quick)
        serverAvailable = testServer();

        buildUI();
        attachHandlers();

        // Start on home
        tabbedPane.setSelectedIndex(0);
        
    }
    
    public MainUI(String loggedInUser) {
        this(); // gọi constructor mặc định để build UI
        this.currentUser = loggedInUser;
        lblWelcome.setText("Xin chào: " + loggedInUser);
        refreshAccountInfo();
        loadBookedTicketsForUser();
    }


    // ---------------- Build UI ----------------
    private void buildUI() {
        // Top header with logo + welcome + tab bar (we'll show JTabbedPane as main nav)
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(Color.WHITE);
        topHeader.setBorder(BorderFactory.createMatteBorder(0,0,1,0, new Color(230,230,230)));

        JLabel logo = new JLabel("CinemaX");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setBorder(new EmptyBorder(10,20,10,20));
        topHeader.add(logo, BorderLayout.WEST);

        lblWelcome.setBorder(new EmptyBorder(6,6,6,20));
        lblWelcome.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblWelcome.setHorizontalAlignment(SwingConstants.RIGHT);
        topHeader.add(lblWelcome, BorderLayout.EAST);

        add(topHeader, BorderLayout.NORTH);

        // Prepare tab panels
        buildHomePanel();
        buildMoviesPanel();
        buildShowsPanel();
        buildContactPanel();
        buildAccountPanel();

        tabbedPane.addTab("Trang chủ", null, homePanel, "Trang chủ");
        tabbedPane.addTab("Phim", null, moviesPanel, "Danh sách phim");
        tabbedPane.addTab("Xuất chiếu", null, showsPanel, "Vé đã đặt / Xuất chiếu");
        tabbedPane.addTab("Liên hệ", null, contactPanel, "Liên hệ");
        tabbedPane.addTab("Tài khoản", null, accountPanel, "Thông tin tài khoản / Đăng xuất");

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void buildHomePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(20,20,20,20));
        JLabel h = new JLabel("<html><center><span style='font-size:24pt'>Chào mừng đến với <b>CinemaX</b></span><br/>Đặt vé nhanh — Xem phim đã đời</center></html>", SwingConstants.CENTER);
        p.add(h, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton btnGoMovies = new JButton("Xem Phim");
        btnGoMovies.addActionListener(e -> tabbedPane.setSelectedComponent(moviesPanel));
        bottom.add(btnGoMovies);
        p.add(bottom, BorderLayout.SOUTH);

        homePanel.add(p, BorderLayout.CENTER);
    }

    private void buildMoviesPanel() {
        // Left: grid of movies (scrollable)
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(new EmptyBorder(10,10,10,10));
        JLabel title = new JLabel("Danh sách phim");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        left.add(title, BorderLayout.NORTH);

        movieGridPanel.setBorder(new EmptyBorder(12,12,12,12));
        JScrollPane sp = new JScrollPane(movieGridPanel);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        left.add(sp, BorderLayout.CENTER);

        // Right: details
        movieDetailPanel.setLayout(new BoxLayout(movieDetailPanel, BoxLayout.Y_AXIS));
        movieDetailPanel.setBorder(new EmptyBorder(20,20,20,20));

        JPanel detailTop = new JPanel(new BorderLayout(10, 0)); // khoảng cách 10px

     // Poster bên trái
     lblDetailPoster.setPreferredSize(new Dimension(220, 280));
     lblDetailPoster.setHorizontalAlignment(SwingConstants.CENTER);
     lblDetailPoster.setVerticalAlignment(SwingConstants.CENTER);
     lblDetailPoster.setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));
     detailTop.add(lblDetailPoster, BorderLayout.WEST);

     // Thông tin phim bên phải (căn giữa theo poster)
     JPanel infoWrapper = new JPanel(new GridBagLayout()); // giúp canh giữa dọc
     JPanel infoPanel = new JPanel();
     infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

     lblDetailTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
     lblDetailGenre.setFont(new Font("Segoe UI", Font.PLAIN, 14));
     lblDetailGenre.setForeground(new Color(100,100,100));
     lblDetailDuration.setFont(new Font("Segoe UI", Font.PLAIN, 14));
     lblDetailDuration.setForeground(new Color(60,60,60));

     infoPanel.add(lblDetailTitle);
     infoPanel.add(Box.createRigidArea(new Dimension(0,6)));
     infoPanel.add(lblDetailGenre);
     infoPanel.add(Box.createRigidArea(new Dimension(0,6)));
     infoPanel.add(lblDetailDuration);
     infoPanel.add(Box.createRigidArea(new Dimension(0,12)));

     btnDetailBook.setBackground(new Color(30,144,255));
     btnDetailBook.setForeground(Color.WHITE);
     btnDetailBook.setPreferredSize(new Dimension(160,36));
     btnDetailBook.setVisible(false);
     infoPanel.add(btnDetailBook);

     // Dùng GridBagLayout để căn giữa infoPanel so với poster
     GridBagConstraints gbc = new GridBagConstraints();
     gbc.gridx = 0;
     gbc.gridy = 0;
     gbc.anchor = GridBagConstraints.CENTER;
     infoWrapper.add(infoPanel, gbc);

     detailTop.add(infoWrapper, BorderLayout.CENTER);

     // add cụm này vào movieDetailPanel
     movieDetailPanel.add(detailTop);
     movieDetailPanel.add(Box.createRigidArea(new Dimension(0,10)));


        // Hidden field for storing selected show id (optional)
        tfDetailShowId.setVisible(false);
        movieDetailPanel.add(tfDetailShowId);

        // Wrap left+right into split pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, movieDetailPanel);
        split.setResizeWeight(0.70);
        moviesPanel.add(split, BorderLayout.CENTER);
        
        taDetailDesc.setEditable(false);
        taDetailDesc.setLineWrap(true);
        taDetailDesc.setWrapStyleWord(true);
        taDetailDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taDetailDesc.setBorder(BorderFactory.createEtchedBorder());
        taDetailDesc.setBackground(new Color(250,250,250));
        taDetailDesc.setMaximumSize(new Dimension(350,200));
        taDetailDesc.setAlignmentX(Component.CENTER_ALIGNMENT);  // THÊM
        movieDetailPanel.add(taDetailDesc);
        
        // Footer small
        JPanel footer = new JPanel();
        footer.setBorder(new EmptyBorder(10,10,10,10));
        footer.add(new JLabel("© 2025 CinemaX"));
        moviesPanel.add(footer, BorderLayout.SOUTH);
    }

    private void buildShowsPanel() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(12,12,12,12));
        JLabel h = new JLabel("Vé đã đặt / Xuất chiếu của bạn");
        h.setFont(new Font("Segoe UI", Font.BOLD, 18));
        center.add(h, BorderLayout.NORTH);

        taBookedTickets.setEditable(false);
        taBookedTickets.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(taBookedTickets);
        center.add(sp, BorderLayout.CENTER);

        JButton btnReload = new JButton("Tải lại");
        btnReload.addActionListener(e -> loadBookedTicketsForUser());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnReload);
        center.add(bottom, BorderLayout.SOUTH);

        showsPanel.add(center, BorderLayout.CENTER);
    }

    private void buildContactPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20,20,20,20));

        JLabel h = new JLabel("<html><b>Liên hệ với CinemaX</b></html>");
        h.setFont(new Font("Segoe UI", Font.BOLD, 18));
        p.add(h);
        p.add(Box.createRigidArea(new Dimension(0,8)));

        JLabel email = new JLabel("Email: support@cinemax.example");
        JLabel phone = new JLabel("SĐT: +84 912 345 678");
        JLabel addr = new JLabel("Địa chỉ: 123 Đường Điện Ảnh, Quận 1, TP. HCM");
        p.add(email); p.add(Box.createRigidArea(new Dimension(0,4)));
        p.add(phone); p.add(Box.createRigidArea(new Dimension(0,4)));
        p.add(addr);

        p.add(Box.createRigidArea(new Dimension(0,12)));
        JLabel social = new JLabel("Fanpage: facebook.com/cinemax | Zalo: @cinemax");
        p.add(social);

        contactPanel.add(p, BorderLayout.NORTH);
    }

    private void buildAccountPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20,20,20,20));

        lblAccountInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(lblAccountInfo);
        p.add(Box.createRigidArea(new Dimension(0,10)));

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.addActionListener(e -> {
            currentUser = null;
            lblWelcome.setText("Xin chào: (chưa đăng nhập)");
            lblAccountInfo.setText("Bạn chưa đăng nhập.");
            tabbedPane.setSelectedIndex(0);
        });
        p.add(btnLogout);

        accountPanel.add(p, BorderLayout.NORTH);
    }

    // ---------------- Handlers ----------------
    private void attachHandlers() {
        // Tab change listener (lazy load)
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            String title = tabbedPane.getTitleAt(idx);
            switch (title) {
                case "Phim":
                    // load movies only when user opens tab
                    SwingUtilities.invokeLater(this::loadMoviesFromServerOrLocal);
                    break;
                case "Xuất chiếu":
                    // load booked tickets
                    SwingUtilities.invokeLater(this::loadBookedTicketsForUser);
                    break;
                case "Tài khoản":
                    SwingUtilities.invokeLater(this::refreshAccountInfo);
                    break;
                case "Liên hệ":
                    // nothing heavy to load
                    break;
                case "Trang chủ":
                    // nothing
                    break;
            }
        });

        // Book button: open shows dialog (select show then seats)
        btnDetailBook.addActionListener(e -> {
            // Attempt to find shows for selected movie via tfDetailShowId (we store movie id there)
            String movieId = tfDetailShowId.getText();
            if (movieId == null || movieId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một phim trước khi đặt vé.");
                return;
            }
            // open shows dialog for this movie (reusing openShowsDialogForMovie logic)
            Movie m = findLocalOrServerMovieById(movieId);
            if (m != null) {
                openShowsDialogForMovie(m);
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy phim để đặt vé.");
            }
        });

        // Double-click movie card selection: handled in createMovieCard -> click listener
    }

    // ---------------- Movie loading & UI update ----------------
    private void loadMoviesFromServerOrLocal() {
        // Clear grid and show loading text
        movieGridPanel.removeAll();
        movieGridPanel.add(new JLabel("Đang tải danh sách phim..."));
        movieGridPanel.revalidate();
        movieGridPanel.repaint();

        // Try server (mocked via socket protocol if available), else fallback local
        if (serverAvailable) {
            List<String> lines = sendRequestMultiLine("GET_MOVIES");
            if (lines != null) {
                List<String[]> rows = new ArrayList<>();
                for (String L : lines) {
                    if (L == null || L.trim().isEmpty() || L.startsWith("ERR;")) continue;
                    rows.add(L.split(";", -1));
                }
                SwingUtilities.invokeLater(() -> updateMovieGridFromRows(rows));
                return;
            } else {
                // server failed during request; fallback
            }
        }
        // fallback to local
        SwingUtilities.invokeLater(this::updateMovieGridFromLocal);
    }

    private void updateMovieGridFromRows(List<String[]> rows) {
        movieGridPanel.removeAll();
        for (String[] r : rows) {
            String id = r.length > 0 ? r[0] : UUID.randomUUID().toString();
            String title = r.length > 1 ? r[1] : "Untitled";
            String genre = r.length > 2 ? r[2] : "";
            int dur = 0;
            try { dur = r.length > 3 ? Integer.parseInt(r[3]) : 0; } catch (Exception ignored) {}
            String poster = r.length > 4 ? r[4] : "";
            Movie m = new Movie(id, title, genre, dur, poster);
            movieGridPanel.add(createMovieCard(m));
        }
        movieGridPanel.revalidate();
        movieGridPanel.repaint();
    }

    private void updateMovieGridFromLocal() {
        movieGridPanel.removeAll();
        for (Movie m : localMovies) {
            movieGridPanel.add(createMovieCard(m));
        }
        movieGridPanel.revalidate();
        movieGridPanel.repaint();
    }

    private JPanel createMovieCard(Movie m) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(200, 300));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220)),
                new EmptyBorder(8,8,8,8)
        ));

        // ================== Hình ảnh ==================
        JLabel img = new JLabel();
        img.setPreferredSize(new Dimension(180, 220));
        img.setHorizontalAlignment(SwingConstants.CENTER);
        img.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (m.poster != null && !m.poster.isEmpty()) {
            ImageIcon ic = loadImageIconSafely(m.poster, 180, 220);
            if (ic != null) {
                img.setIcon(ic);
                img.setText(null);
            } else {
                img.setText("No Image");
            }
        } else {
            img.setText("No Image");
        }
        img.setAlignmentX(Component.CENTER_ALIGNMENT); // căn giữa
        card.add(img);

        card.add(Box.createRigidArea(new Dimension(0, 8)));

        // ================== Thông tin phim ==================
        JLabel title = new JLabel("<html><b>" + m.title + "</b></html>");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);

        JLabel genre = new JLabel(m.genre);
        genre.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        genre.setForeground(new Color(90,90,90));
        genre.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(genre);

        JLabel dur = new JLabel(m.duration > 0 ? (m.duration + " phút") : "");
        dur.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dur.setForeground(new Color(90,90,90));
        dur.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(dur);

        card.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btn = new JButton("Xem chi tiết");
        btn.setBackground(new Color(30,144,255));
        btn.setForeground(Color.WHITE);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT); // căn giữa
        btn.addActionListener(e -> selectMovieShowDetail(m));
        card.add(btn);

        // ================== Click cả card ==================
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectMovieShowDetail(m);
            }
        });

        return card;
    }


    private void selectMovieShowDetail(Movie m) {
        // Populate detail panel fields
        lblDetailPoster.setIcon(null);
        if (m.poster != null && !m.poster.isEmpty()) {
            ImageIcon ic = loadImageIconSafely(m.poster, 220, 280);
            if (ic != null) lblDetailPoster.setIcon(ic);
            else lblDetailPoster.setText("No Image");
        } else {
            lblDetailPoster.setText("No Image");
        }
        lblDetailTitle.setText(m.title);
        lblDetailGenre.setText("Thể loại: " + m.genre);
        lblDetailDuration.setText((m.duration > 0 ? (m.duration + " phút") : "Không rõ thời lượng"));
        taDetailDesc.setText("Mô tả: " + (m.poster == null ? "Mô tả phim chưa có." : "Phiên bản demo - mô tả phim."));
        tfDetailShowId.setText(m.id); // store movie id

        // Show Đặt vé button under duration (visible now)
        btnDetailBook.setVisible(true);

        // Ensure movieDetailPanel revalidates
        movieDetailPanel.revalidate();
        movieDetailPanel.repaint();
    }

    private Movie findLocalOrServerMovieById(String id) {
        // try local
        for (Movie m : localMovies) if (m.id.equals(id)) return m;
        // if server available, try request (simple GET_MOVIE;id)
        if (serverAvailable) {
            String resp = sendRequestSingleLine("GET_MOVIE;" + id);
            if (resp != null && !resp.startsWith("ERR")) {
                String[] p = resp.split(";", -1);
                String mid = p.length>0 ? p[0] : id;
                String t = p.length>1 ? p[1] : "Untitled";
                String g = p.length>2 ? p[2] : "";
                int d = 0;
                try { d = p.length>3 ? Integer.parseInt(p[3]) : 0; } catch (Exception ignored) {}
                String poster = p.length>4 ? p[4] : "";
                return new Movie(mid, t, g, d, poster);
            }
        }
        return null;
    }

    // ---------------- Shows/Booking (reused logic) ----------------

    /**
     * Open Shows (xuất chiếu) dialog for a movie - user selects show then seats
     */
    private void openShowsDialogForMovie(Movie movie) {
        JDialog dlg = new JDialog(this, "Suất chiếu - " + movie.title, true);
        dlg.setSize(720,420);
        dlg.setLocationRelativeTo(this);

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        JScrollPane sp = new JScrollPane(list);

        List<String[]> rows = new ArrayList<>();

        // Try server
        if (serverAvailable) {
            List<String> lines = sendRequestMultiLine("GET_SHOWS;" + movie.id);
            if (lines != null) {
                for (String L : lines) {
                    if (L == null || L.trim().isEmpty() || L.startsWith("ERR")) continue;
                    String[] p = L.split(";", -1);
                    rows.add(p);
                    if (p.length >= 6) model.addElement(p[0] + " | " + p[2] + " | Phòng " + p[3] + " | Giá " + p[5]);
                    else model.addElement(String.join(" | ", p));
                }
            }
        }

        // fallback local
        if (rows.isEmpty()) {
            for (Show s : localShows.values()) {
                if (s.movieId.equals(movie.id)) {
                    String[] p = new String[]{s.id, movie.title, s.datetime, String.valueOf(s.roomId), String.valueOf(s.totalSeats), String.valueOf(s.price)};
                    rows.add(p);
                    model.addElement(s.id + " | " + s.datetime + " | Phòng " + s.roomId + " | Giá " + s.price);
                }
            }
        }

        dlg.setLayout(new BorderLayout());
        dlg.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnChoose = new JButton("Chọn suất");
        bottom.add(btnChoose);
        dlg.add(bottom, BorderLayout.SOUTH);

        btnChoose.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx < 0) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn 1 suất!");
                return;
            }
            String[] sel = rows.get(idx);
            // Pass to seat dialog
            String showId = sel.length > 0 ? sel[0] : "";
            int roomId = sel.length > 3 ? safeParseInt(sel[3], 1) : 1;
            int totalSeats = sel.length > 4 ? safeParseInt(sel[4], 100) : 100;
            double price = sel.length > 5 ? safeParseDouble(sel[5], 0) : 0;

            dlg.dispose();
            // Show seats selection (server or local)
            if (serverAvailable) openSeatDialogServer(showId, price);
            else openSeatDialogLocal(showId, roomId, totalSeats, price);
        });

        dlg.setVisible(true);
    }

    private void openSeatDialogServer(String showId, double price) {
        // Try to fetch booked seats via server
        List<String> booked = new ArrayList<>();
        List<String> lines = sendRequestMultiLine("GET_SEATS;" + showId);
        if (lines != null) {
            for (String L : lines) {
                if (L == null || L.trim().isEmpty() || L.startsWith("ERR")) continue;
                if (L.contains(",")) {
                    String[] parts = L.split(",");
                    for (String p : parts) if (!p.trim().isEmpty()) booked.add(p.trim());
                } else booked.add(L.trim());
            }
        } else {
            // fallback random booked from localShows
            Show s = localShows.get(showId);
            if (s != null) booked = pickRandom(localSeatsByRoom.getOrDefault(s.roomId, Collections.emptyList()), Math.min(10, s.totalSeats/10));
        }
        Show s = localShows.get(showId);
        int roomId = s != null ? s.roomId : 1;
        int total = s != null ? s.totalSeats : 100;
        openSeatSelectionDialogGeneric(showId, roomId, total, price, booked);
    }

    private void openSeatDialogLocal(String showId, int roomId, int totalSeats, double price) {
        List<String> booked = getBookedSeatsFromDB(showId);
        if (booked == null) {
            booked = localBookings.stream()
                    .filter(b -> b.showId.equals(showId))
                    .flatMap(bk -> bk.seats.stream())
                    .collect(Collectors.toList());
        }
        openSeatSelectionDialogGeneric(showId, roomId, totalSeats, price, booked);
    }

    private void openSeatSelectionDialogGeneric(final String showId, int roomId, int totalSeats, double price, List<String> bookedSeats) {
        JDialog dlg = new JDialog(this, "Chọn ghế - Suất " + showId, true);
        dlg.setSize(920, 620);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Suất: " + showId + " | Phòng: " + roomId + " | Giá 1 vé: " + currencyF.format(price) + " VND"));
        dlg.add(top, BorderLayout.NORTH);

        int cols = 10;
        int rows = (int)Math.ceil((double)totalSeats / cols);
        JPanel grid = new JPanel(new GridLayout(rows, cols, 6, 6));
        List<JToggleButton> seatButtons = new ArrayList<>();
        List<String> seatLabels = generateSeatLabels(totalSeats);

        for (String label : seatLabels) {
            JToggleButton b = new JToggleButton(label);
            b.setBackground(new Color(34,139,34));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            if (bookedSeats.contains(label)) {
                b.setEnabled(false);
                b.setBackground(Color.RED);
            }
            seatButtons.add(b);
            grid.add(b);
        }
        JScrollPane sp = new JScrollPane(grid);
        dlg.add(sp, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(12,12,12,12));
        JLabel lblSelected = new JLabel("Ghế đã chọn: -");
        lblSelected.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(lblSelected);
        right.add(Box.createRigidArea(new Dimension(0,8)));
        JLabel lblTotal = new JLabel("Tổng tiền: 0 VND");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        right.add(lblTotal);
        right.add(Box.createRigidArea(new Dimension(0,12)));

        JButton btnConfirm = new JButton("Đặt vé");
        btnConfirm.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(btnConfirm);
        right.add(Box.createRigidArea(new Dimension(0,8)));
        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(ev -> dlg.dispose());
        right.add(btnCancel);

        dlg.add(right, BorderLayout.EAST);

        Runnable updater = () -> {
            List<String> selected = seatButtons.stream().filter(AbstractButton::isSelected).map(AbstractButton::getText).collect(Collectors.toList());
            if (selected.isEmpty()) lblSelected.setText("Ghế đã chọn: -");
            else lblSelected.setText("Ghế đã chọn: " + String.join(", ", selected));
            double total = selected.size() * price;
            lblTotal.setText("Tổng tiền: " + currencyF.format(total) + " VND");
        };
        seatButtons.forEach(b -> b.addItemListener(e -> updater.run()));

        btnConfirm.addActionListener(e -> {
            List<String> selected = seatButtons.stream().filter(AbstractButton::isSelected).map(AbstractButton::getText).collect(Collectors.toList());
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn ít nhất 1 ghế.");
                return;
            }
            String seatsCsv = String.join(",", selected);
            if (serverAvailable) {
                String req = String.format("BOOK_TICKET;%s;%s;%s", showId, currentUser == null ? "guest" : currentUser, seatsCsv);
                String resp = sendRequestSingleLine(req);
                if (resp != null && resp.startsWith("OK")) {
                    // success
                    JOptionPane.showMessageDialog(dlg, "Đặt vé thành công (server).");
                    dlg.dispose();
                } else {
                    JOptionPane.showMessageDialog(dlg, resp == null ? "Server không phản hồi" : resp);
                }
            } else {
                double amount = selected.size() * price;
                boolean saved = saveBookingToDB(showId, currentUser == null ? "guest" : currentUser, selected, amount);
                Booking b = new Booking(UUID.randomUUID().toString(), currentUser == null ? "guest" : currentUser, showId, new ArrayList<>(selected), amount, Timestamp.valueOf(LocalDateTime.now()));
                localBookings.add(b);
                if (saved) {
                    JOptionPane.showMessageDialog(dlg, "Đặt vé thành công (lưu vào SQL).");
                    dlg.dispose();
                } else {
                    JOptionPane.showMessageDialog(dlg, "Đặt vé thành công (local) — nhưng không thể lưu vào SQL.");
                    dlg.dispose();
                }
            }
        });

        dlg.setVisible(true);
    }

    // ---------------- History / Booked tickets ----------------
    private void loadBookedTicketsForUser() {
        if (currentUser == null) {
            taBookedTickets.setText("Bạn chưa đăng nhập. Vui lòng đăng nhập để xem vé đã đặt.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        // Try DB: select from ve join xuat_chieu, phim, ghe
        try (Connection conn = CSDL.getConnection()) {
            String sql = "SELECT v.id AS ve_id, p.ten_phim, xc.ngay, xc.gio, ph.ten_phong, g.ma_ghe, v.gia, v.trang_thai, v.ngay_tao " +
                    "FROM ve v " +
                    "JOIN xuat_chieu xc ON v.xuat_chieu_id = xc.id " +
                    "JOIN phim p ON xc.phim_id = p.id " +
                    "JOIN ghe g ON v.ghe_id = g.id " +
                    "JOIN phong_chieu ph ON xc.phong_id = ph.id " +
                    "JOIN nguoi_dung nd ON v.nguoi_dung_id = nd.id " +
                    "WHERE nd.gmail = ? OR nd.so_dien_thoai = ? OR nd.ho_ten = ? " +
                    "ORDER BY v.ngay_tao DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUser);
                ps.setString(2, currentUser);
                ps.setString(3, currentUser);
                ResultSet rs = ps.executeQuery();
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    sb.append(String.format("VéID:%d | Phim:%s | Ngày:%s %s | Phòng:%s | Ghế:%s | Giá:%,.0f | Trạng thái:%s | %s%n",
                            rs.getInt("ve_id"),
                            rs.getString("ten_phim"),
                            rs.getDate("ngay"),
                            rs.getTime("gio"),
                            rs.getString("ten_phong"),
                            rs.getString("ma_ghe"),
                            rs.getDouble("gia"),
                            rs.getString("trang_thai"),
                            rs.getTimestamp("ngay_tao").toString()
                    ));
                }
                if (any) {
                    taBookedTickets.setText(sb.toString());
                    return;
                }
            }
        } catch (Exception ex) {
            // fallback to localBookings
            ex.printStackTrace();
        }

        // fallback local in-memory
        sb.append("==== Vé đã đặt (local) ====\n");
        for (Booking b : localBookings) {
            if (b.username.equalsIgnoreCase(currentUser) || b.username.equalsIgnoreCase(getLocalUserName(currentUser))) {
                sb.append(String.format("ID:%s | Suất:%s | Ghế:%s | Tiền:%,.0f | %s%n",
                        b.id, b.showId, String.join(",", b.seats), b.amount, b.timestamp.toString()));
            }
        }
        if (sb.toString().trim().isEmpty()) taBookedTickets.setText("Không có vé đã đặt.");
        else taBookedTickets.setText(sb.toString());
    }

    // ---------------- DB helpers ----------------
    private void initBookingLocalTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS booking_local (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "show_code VARCHAR(80) NOT NULL," +
                "username VARCHAR(150) NOT NULL," +
                "seat VARCHAR(20) NOT NULL," +
                "amount DECIMAL(12,2) DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_show_seat (show_code, seat)" +
                ") ENGINE=InnoDB";
        try (Connection conn = CSDL.getConnection(); Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private List<String> getBookedSeatsFromDB(String showId) {
        List<String> out = new ArrayList<>();
        String sql = "SELECT seat FROM booking_local WHERE show_code = ?";
        try (Connection conn = CSDL.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, showId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(rs.getString("seat"));
            return out;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private boolean saveBookingToDB(String showId, String username, List<String> seats, double amountPerBooking) {
        String sql = "INSERT INTO booking_local (show_code, username, seat, amount) VALUES (?, ?, ?, ?)";
        boolean anySaved = false;
        try (Connection conn = CSDL.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (String seat : seats) {
                    ps.setString(1, showId);
                    ps.setString(2, username);
                    ps.setString(3, seat);
                    ps.setDouble(4, amountPerBooking / Math.max(1, seats.size()));
                    try {
                        ps.executeUpdate();
                        anySaved = true;
                    } catch (SQLException ex) {
                        System.err.println("Không lưu được ghế " + seat + ": " + ex.getMessage());
                    }
                }
            }
            conn.commit();
            return anySaved;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // ---------------- Networking helpers (socket protocol to server if present) ----------------
    private boolean testServer() {
        try (Socket s = new Socket(SERVER_HOST, SERVER_PORT)) {
            s.setSoTimeout(1000);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String sendRequestSingleLine(String req) {
        try (Socket s = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            s.setSoTimeout(SOCKET_TIMEOUT_MS);
            out.println(req);
            return in.readLine();
        } catch (Exception ex) {
            ex.printStackTrace();
            serverAvailable = false;
            return null;
        }
    }

    private List<String> sendRequestMultiLine(String req) {
        try (Socket s = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            s.setSoTimeout(SOCKET_TIMEOUT_MS);
            out.println(req);
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                lines.add(line);
            }
            return lines;
        } catch (Exception ex) {
            ex.printStackTrace();
            serverAvailable = false;
            return null;
        }
    }

    // ---------------- Helpers and utilities ----------------
    private ImageIcon loadImageIconSafely(String pathOrUrl, int w, int h) {
        try {
            BufferedImage img = null;
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                URL url = new URL(pathOrUrl);
                try (InputStream is = url.openStream()) {
                    img = ImageIO.read(is);
                }
            } else {
                File f = new File(pathOrUrl);
                if (f.exists()) img = ImageIO.read(f);
            }
            if (img != null) {
                Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    private List<String> generateSeatLabels(int capacity) {
        List<String> list = new ArrayList<>();
        int cols = 10;
        int rows = (int)Math.ceil((double)capacity / cols);
        for (int r=0; r<rows; r++) {
            char row = (char)('A' + r);
            for (int c=1; c<=cols && list.size() < capacity; c++) {
                list.add(String.format("%c%d", row, c));
            }
        }
        return list;
    }

    private List<String> pickRandom(List<String> all, int n) {
        List<String> copy = new ArrayList<>(all);
        Collections.shuffle(copy);
        return copy.stream().limit(n).collect(Collectors.toList());
    }

    private int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception ex) { return def; }
    }
    private double safeParseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception ex) { return def; }
    }

    // ---------------- Local sample data (fallback) ----------------
    private void prepareLocalSampleData() {
        localMovies.clear();
        localShows.clear();
        localSeatsByRoom.clear();
        localBookings.clear();
        localUsers.clear();

        localMovies.add(new Movie("1", "The Great Adventure", "Action", 120, ""));
        localMovies.add(new Movie("2", "Love in Spring", "Romance", 95, ""));
        localMovies.add(new Movie("3", "Space Odyssey", "Sci-Fi", 140, ""));
        localMovies.add(new Movie("4", "Funny Days", "Comedy", 105, ""));
        localMovies.add(new Movie("5", "Horror Night", "Horror", 90, ""));

        localShows.put("S1001", new Show("S1001","1",1,"2025-10-01 10:00:00",100,80000));
        localShows.put("S1002", new Show("S1002","1",1,"2025-10-01 14:00:00",100,90000));
        localShows.put("S2001", new Show("S2001","2",2,"2025-10-02 16:00:00",100,70000));
        localShows.put("S3001", new Show("S3001","3",3,"2025-10-03 20:00:00",120,120000));
        localShows.put("S4001", new Show("S4001","4",4,"2025-10-04 18:00:00",100,60000));

        for (int room=1; room<=10; room++) {
            int cap = (room <= 5) ? 100 : (room <= 8) ? 150 : 200;
            localSeatsByRoom.put(room, generateSeatLabels(cap));
        }

        localUsers.put("admin@example.com", new LocalUser("Admin", "admin@example.com", "0123456789", "admin123"));
        localUsers.put("user1@example.com", new LocalUser("User One", "user1@example.com", "0987654321", "123456"));

        // Add one sample booking to show in Xuất chiếu when logged as "user1@example.com"
        Booking b = new Booking(UUID.randomUUID().toString(), "user1@example.com", "S1001", Arrays.asList("A1","A2"), 160000, Timestamp.valueOf(LocalDateTime.now()));
        localBookings.add(b);
    }

    private String getLocalUserName(String key) {
        LocalUser u = localUsers.get(key);
        return u == null ? key : u.name;
    }

    private void refreshAccountInfo() {
        if (currentUser == null) {
            lblAccountInfo.setText("Bạn chưa đăng nhập.");
        } else {
            lblAccountInfo.setText(String.format("<html>Xin chào <b>%s</b><br/>Tài khoản: %s</html>", getLocalUserName(currentUser), currentUser));
            lblWelcome.setText("Xin chào: " + getLocalUserName(currentUser));
        }
    }

    // ---------------- Small helper classes ----------------
    static class Movie {
        String id, title, genre;
        int duration;
        String poster;
        Movie(String id, String title, String genre, int duration, String poster) {
            this.id = id; this.title = title; this.genre = genre; this.duration = duration; this.poster = poster;
        }
    }
    static class Show {
        String id, movieId, datetime;
        int roomId, totalSeats;
        double price;
        Show(String id, String movieId, int roomId, String datetime, int totalSeats, double price) {
            this.id = id; this.movieId = movieId; this.roomId = roomId; this.datetime = datetime; this.totalSeats = totalSeats; this.price = price;
        }
    }
    static class Booking {
        String id, username, showId;
        List<String> seats;
        double amount;
        Timestamp timestamp;
        Booking(String id, String username, String showId, List<String> seats, double amount, Timestamp timestamp) {
            this.id = id; this.username = username; this.showId = showId; this.seats = seats; this.amount = amount; this.timestamp = timestamp;
        }
    }
    static class LocalUser {
        String name, email, phone, password;
        LocalUser(String name, String email, String phone, String password) {
            this.name = name; this.email = email; this.phone = phone; this.password = password;
        }
    }

    static class WrapFlowLayout extends FlowLayout {
        public WrapFlowLayout(int hgap, int vgap) { super(FlowLayout.LEFT, hgap, vgap); }
    }

    // ---------------- Main ----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainUI app = new MainUI();
            app.setVisible(true);
        });
    }
}
