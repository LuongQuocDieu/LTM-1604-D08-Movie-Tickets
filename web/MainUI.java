package web;

import mysql.CSDL;
import utils.AppTheme;
import utils.MessageDialog;
import utils.MessageDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
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
    private final JButton btnDetailBook = new JButton("Thêm vào giỏ hàng"); // must appear under duration
    private final JLabel lblWelcome = new JLabel("Xin chào: (chưa đăng nhập)");
    private Movie selectedMovieDetail = null; // Track selected movie for booking

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
        
        // Apply modern theme
        applyModernTheme();

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

    // ================== Modern Theme ==================
    private void applyModernTheme() {
        // Set Nimbus Look and Feel for better color support
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Use centralized AppTheme for all UI styling
        // Table styling
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.alternateRowColor", AppTheme.BG_LIGHT);
        UIManager.put("Table.gridColor", AppTheme.BORDER_LIGHT);
        UIManager.put("TableHeader.background", AppTheme.PRIMARY_BLUE);
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("TableHeader.font", AppTheme.FONT_TABLE_HEADER);
        
        // Panel styling
        UIManager.put("Panel.background", AppTheme.BG_LIGHT);
        UIManager.put("Panel.foreground", AppTheme.TEXT_DARK);
        
        // ComboBox styling
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", AppTheme.TEXT_DARK);
        
        // TextField styling
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", AppTheme.TEXT_DARK);
        
        // TabbedPane styling
        UIManager.put("TabbedPane.background", AppTheme.BG_LIGHT);
        UIManager.put("TabbedPane.foreground", AppTheme.TEXT_DARK);
    }

    // ---------------- Build UI ----------------
    private void buildUI() {
        // Top header with gradient background
        GradientPanel topHeader = new GradientPanel(
            AppTheme.PRIMARY_BLUE,        // Primary blue
            AppTheme.PRIMARY_BLUE_LIGHT   // Light blue
        );
        topHeader.setLayout(new BorderLayout());
        topHeader.setBorder(BorderFactory.createMatteBorder(0,0,3,0, AppTheme.PRIMARY_BLUE_DARK));

        JLabel logo = new JLabel("CinemaX");
        logo.setFont(AppTheme.FONT_HEADER_LARGE);
        logo.setForeground(Color.WHITE);
        logo.setBorder(new EmptyBorder(12,20,12,20));
        topHeader.add(logo, BorderLayout.WEST);

        lblWelcome.setBorder(new EmptyBorder(8,8,8,20));
        lblWelcome.setFont(AppTheme.FONT_LABEL_REGULAR);
        lblWelcome.setForeground(new Color(220, 230, 250));
        lblWelcome.setHorizontalAlignment(SwingConstants.RIGHT);
        
        // Thêm nút Giỏ Hàng + Lịch sử
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rightPanel.setOpaque(false);
        
        RoundedButton btnHistory = new RoundedButton("Lịch sử");
        btnHistory.setFocusPainted(false);
        btnHistory.setBackground(AppTheme.PRIMARY_BLUE_LIGHT);
        btnHistory.setForeground(Color.WHITE);
        btnHistory.setFont(AppTheme.FONT_BUTTON);
        btnHistory.setPreferredSize(new Dimension(100, 36));
        btnHistory.addActionListener(e -> {
            if (currentUser == null || currentUser.isEmpty()) {
                MessageDialog.showInfo(this, "Vui lòng đăng nhập trước!");
                return;
            }
            try (Connection conn = CSDL.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id FROM nguoi_dung WHERE gmail = ? OR ho_ten = ?")) {
                ps.setString(1, currentUser);
                ps.setString(2, currentUser);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    showDetailedBookingHistory(userId, currentUser);
                }
            } catch (SQLException ex) {
                MessageDialog.showInfo(this, "Lỗi: " + ex.getMessage());
            }
        });
        
        RoundedButton btnCart = new RoundedButton("Giỏ Hàng");
        btnCart.setFocusPainted(false);
        btnCart.setBackground(AppTheme.SUCCESS_GREEN);
        btnCart.setForeground(Color.WHITE);
        btnCart.setFont(AppTheme.FONT_BUTTON);
        btnCart.setPreferredSize(new Dimension(110, 36));
        btnCart.addActionListener(e -> {
            if (currentUser == null || currentUser.isEmpty()) {
                MessageDialog.showInfo(this, "Vui lòng đăng nhập trước!");
                return;
            }
            try (Connection conn = CSDL.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id FROM nguoi_dung WHERE gmail = ? OR ho_ten = ?")) {
                ps.setString(1, currentUser);
                ps.setString(2, currentUser);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    CartUI cartUI = new CartUI(userId, currentUser);
                    cartUI.setVisible(true);
                }
            } catch (SQLException ex) {
                MessageDialog.showInfo(this, "Lỗi: " + ex.getMessage());
            }
        });
        rightPanel.add(btnHistory);
        rightPanel.add(btnCart);
        rightPanel.add(lblWelcome);
        topHeader.add(rightPanel, BorderLayout.EAST);

        add(topHeader, BorderLayout.NORTH);

        // Prepare tab panels
        buildHomePanel();
        buildMoviesPanel();
        buildShowsPanel();
        buildContactPanel();
        buildAccountPanel();

        tabbedPane.addTab("Trang chủ", null, homePanel, "Trang chủ");
        tabbedPane.addTab("Phim", null, moviesPanel, "Danh sách phim");
        tabbedPane.addTab("Liên hệ", null, contactPanel, "Liên hệ");
        tabbedPane.addTab("Tài khoản", null, accountPanel, "Thông tin tài khoản / Đăng xuất");

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void buildHomePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(248, 249, 250));
        p.setBorder(new EmptyBorder(40,20,40,20));
        
        JLabel h = new JLabel("<html><center><span style='font-size:28pt; color:#1967d2'><b>CinemaX</b></span><br/><span style='font-size:16pt; color:#666'>Đặt vé nhanh — Xem phim đã đời</span></center></html>", SwingConstants.CENTER);
        h.setFont(new Font("Segoe UI", Font.BOLD, 24));
        p.add(h, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setBackground(new Color(248, 249, 250));
        JButton btnGoMovies = new RoundedButton("Xem Phim Ngay");
        btnGoMovies.setBackground(new Color(25, 103, 210));
        btnGoMovies.setForeground(Color.WHITE);
        btnGoMovies.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnGoMovies.setPreferredSize(new Dimension(200, 44));
        btnGoMovies.addActionListener(e -> tabbedPane.setSelectedComponent(moviesPanel));
        bottom.add(btnGoMovies);
        p.add(bottom, BorderLayout.SOUTH);

        homePanel.add(p, BorderLayout.CENTER);
    }

    private void buildMoviesPanel() {
        // Left: grid of movies (scrollable)
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(new Color(248, 249, 250));
        left.setBorder(new EmptyBorder(15,15,15,15));
        
        // Filter panel with modern styling
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(new Color(248, 249, 250));
        JLabel lblFilter = new JLabel("🔍 Lọc:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFilter.setForeground(new Color(33, 33, 33));
        
        JComboBox<String> filterCombo = new JComboBox<>(new String[]{"Tất cả", "Đang chiếu", "Sắp chiếu"});
        filterCombo.setPreferredSize(new Dimension(150, 32));
        filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        filterCombo.addActionListener(e -> {
            String filter = (String) filterCombo.getSelectedItem();
            filterMovies(filter);
        });
        filterPanel.add(lblFilter);
        filterPanel.add(filterCombo);
        
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setBackground(new Color(248, 249, 250));
        JLabel title = new JLabel("Danh sách phim");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(25, 103, 210));
        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(filterPanel, BorderLayout.EAST);
        left.add(topPanel, BorderLayout.NORTH);

        movieGridPanel.setBackground(new Color(248, 249, 250));
        movieGridPanel.setBorder(new EmptyBorder(12,12,12,12));
        JScrollPane sp = new JScrollPane(movieGridPanel);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setBackground(new Color(248, 249, 250));
        left.add(sp, BorderLayout.CENTER);

        // Right: details panel with gradient
        movieDetailPanel.setLayout(new BoxLayout(movieDetailPanel, BoxLayout.Y_AXIS));
        movieDetailPanel.setBackground(new Color(255, 255, 255));
        movieDetailPanel.setBorder(new EmptyBorder(25,25,25,25));

        JPanel detailTop = new JPanel(new BorderLayout(15, 0));
        detailTop.setBackground(Color.WHITE);

        // Poster với shadow
        JPanel posterPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw shadow
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 4, 4);
                super.paintComponent(g);
            }
        };
        posterPanel.setLayout(new BorderLayout());
        posterPanel.setBackground(Color.WHITE);
        posterPanel.setPreferredSize(new Dimension(240, 320));
        
        lblDetailPoster.setPreferredSize(new Dimension(240, 320));
        lblDetailPoster.setHorizontalAlignment(SwingConstants.CENTER);
        lblDetailPoster.setVerticalAlignment(SwingConstants.CENTER);
        lblDetailPoster.setBackground(new Color(240, 240, 240));
        lblDetailPoster.setOpaque(true);
        posterPanel.add(lblDetailPoster, BorderLayout.CENTER);
        detailTop.add(posterPanel, BorderLayout.WEST);

        // Thông tin phim
        JPanel infoWrapper = new JPanel(new GridBagLayout());
        infoWrapper.setBackground(Color.WHITE);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        lblDetailTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblDetailTitle.setForeground(new Color(25, 103, 210));
        
        lblDetailGenre.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDetailGenre.setForeground(new Color(100, 100, 100));
        
        lblDetailDuration.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDetailDuration.setForeground(new Color(100, 100, 100));

        infoPanel.add(lblDetailTitle);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        infoPanel.add(lblDetailGenre);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        infoPanel.add(lblDetailDuration);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        btnDetailBook.setBackground(new Color(25, 103, 210));
        btnDetailBook.setForeground(Color.WHITE);
        btnDetailBook.setPreferredSize(new Dimension(180, 40));
        btnDetailBook.setMaximumSize(new Dimension(180, 40));
        btnDetailBook.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDetailBook.setVisible(false);
        infoPanel.add(btnDetailBook);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        infoWrapper.add(infoPanel, gbc);

        detailTop.add(infoWrapper, BorderLayout.CENTER);

        movieDetailPanel.add(detailTop);
        movieDetailPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Hidden field
        tfDetailShowId.setVisible(false);
        movieDetailPanel.add(tfDetailShowId);

        // Wrap left+right into split pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, movieDetailPanel);
        split.setResizeWeight(0.65);
        split.setDividerLocation(700);
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

        // Form chỉnh sửa
        JPanel editForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel lblName = new JLabel("Tên:");
        JTextField tfName = new JTextField(20);
        JLabel lblEmail = new JLabel("Email:");
        JTextField tfEmail = new JTextField(20);
        JLabel lblPhone = new JLabel("Điện thoại:");
        JTextField tfPhone = new JTextField(20);
        JLabel lblPass = new JLabel("Mật khẩu mới (để trống nếu không đổi):");
        JPasswordField pfPass = new JPasswordField(20);
        
        gbc.gridx = 0; gbc.gridy = 0;
        editForm.add(lblName, gbc);
        gbc.gridx = 1;
        editForm.add(tfName, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        editForm.add(lblEmail, gbc);
        gbc.gridx = 1;
        editForm.add(tfEmail, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        editForm.add(lblPhone, gbc);
        gbc.gridx = 1;
        editForm.add(tfPhone, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        editForm.add(lblPass, gbc);
        gbc.gridx = 1;
        editForm.add(pfPass, gbc);
        
        JButton btnEdit = new JButton("Chỉnh sửa");
        btnEdit.addActionListener(e -> {
            if (currentUser == null) {
                MessageDialog.showInfo(editForm, "Vui lòng đăng nhập!");
                return;
            }
            try (Connection conn = CSDL.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT id FROM nguoi_dung WHERE gmail = ? OR ho_ten = ?")) {
                ps.setString(1, currentUser);
                ps.setString(2, currentUser);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    editForm.setEnabled(false);
                    tfName.setEditable(true);
                    tfEmail.setEditable(true);
                    tfPhone.setEditable(true);
                    pfPass.setEditable(true);
                    MessageDialog.showInfo(editForm, "Bạn có thể chỉnh sửa thông tin");
                }
            } catch (SQLException ex) {
                MessageDialog.showInfo(editForm, "Lỗi: " + ex.getMessage());
            }
        });
        
        JButton btnSave = new JButton("Lưu Thay Đổi");
        btnSave.addActionListener(e -> {
            if (currentUser == null) {
                MessageDialog.showInfo(editForm, "Vui lòng đăng nhập!");
                return;
            }
            
            String newName = tfName.getText().trim();
            String newEmail = tfEmail.getText().trim();
            String newPhone = tfPhone.getText().trim();
            String newPass = new String(pfPass.getPassword()).trim();
            
            if (newName.isEmpty() || newEmail.isEmpty()) {
                MessageDialog.showInfo(editForm, "Tên và Email không được để trống!");
                return;
            }
            
            try (Connection conn = CSDL.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "UPDATE nguoi_dung SET ho_ten = ?, gmail = ?, so_dien_thoai = ?" + 
                    (newPass.isEmpty() ? "" : ", mat_khau = ?") + 
                    " WHERE gmail = ? OR ho_ten = ?")) {
                
                ps.setString(1, newName);
                ps.setString(2, newEmail);
                ps.setString(3, newPhone);
                
                if (!newPass.isEmpty()) {
                    ps.setString(4, newPass);
                    ps.setString(5, currentUser);
                    ps.setString(6, currentUser);
                } else {
                    ps.setString(4, currentUser);
                    ps.setString(5, currentUser);
                }
                
                ps.executeUpdate();
                currentUser = newName;
                lblWelcome.setText("Xin chào: " + currentUser);
                refreshAccountInfo();
                MessageDialog.showInfo(editForm, "Cập nhật thông tin thành công!");
                
            } catch (SQLException ex) {
                MessageDialog.showInfo(editForm, "Lỗi: " + ex.getMessage());
            }
        });
        
        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.addActionListener(e -> {
            currentUser = null;
            lblWelcome.setText("Xin chào: (chưa đăng nhập)");
            lblAccountInfo.setText("Bạn chưa đăng nhập.");
            tfName.setText("");
            tfEmail.setText("");
            tfPhone.setText("");
            pfPass.setText("");
            tabbedPane.setSelectedIndex(0);
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnSave);
        buttonPanel.add(btnLogout);
        
        p.add(editForm);
        p.add(Box.createRigidArea(new Dimension(0, 10)));
        p.add(buttonPanel);
        p.add(Box.createVerticalGlue());

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

        // Book button: open Shows dialog to let user select which show
        btnDetailBook.addActionListener(e -> {
            if (currentUser == null || currentUser.isEmpty()) {
                MessageDialog.showInfo(this, "Vui lòng đăng nhập trước khi đặt vé!");
                return;
            }
            
            if (selectedMovieDetail == null) {
                MessageDialog.showInfo(this, "Vui lòng chọn phim trước!");
                return;
            }
            
            // Open shows dialog to let user select which show
            openShowsDialogForMovie(selectedMovieDetail);
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

        // PRIORITY 1: Always try to load from database first
        try (Connection conn = CSDL.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String sql = "SELECT id, ten_phim, the_loai, thoi_luong, poster FROM phim WHERE id IN (SELECT DISTINCT phim_id FROM xuat_chieu)";
            ResultSet rs = stmt.executeQuery(sql);
            List<String[]> rows = new ArrayList<>();
            
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("ten_phim"),
                    rs.getString("the_loai"),
                    String.valueOf(rs.getInt("thoi_luong")),
                    rs.getString("poster")
                });
            }
            
            if (!rows.isEmpty()) {
                SwingUtilities.invokeLater(() -> updateMovieGridFromRows(rows));
                return;
            }
        } catch (Exception ex) {
            // Fall back to server/local if DB fails
        }

        // PRIORITY 2: Try server if database loading didn't work
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
            }
        }
        
        // PRIORITY 3: Fallback to local cached data
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
        BufferedImage poster = null;
        if (m.poster != null && !m.poster.isEmpty()) {
            try {
                if (m.poster.startsWith("http")) {
                    poster = ImageIO.read(new URL(m.poster));
                } else {
                    File file = new File(m.poster);
                    if (file.exists()) {
                        poster = ImageIO.read(file);
                    }
                }
                if (poster != null) {
                    Image scaledImg = poster.getScaledInstance(180, 220, Image.SCALE_SMOOTH);
                    poster = new BufferedImage(180, 220, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2 = poster.createGraphics();
                    g2.drawImage(scaledImg, 0, 0, null);
                    g2.dispose();
                }
            } catch (Exception e) {
                poster = null;
            }
        }

        MovieCard card = new MovieCard(m.title, m.genre, poster, () -> selectMovieShowDetail(m));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectMovieShowDetail(m);
            }
        });
        return card;
    }


    private void selectMovieShowDetail(Movie m) {
        // Set selected movie for booking
        selectedMovieDetail = m;
        
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
        List<String[]> rows = new ArrayList<>();

        // PRIORITY 1: Try to load from database
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT xc.id, p.ten_phim, CONCAT(xc.ngay,' ',xc.gio) as ngay_gio, xc.phong_id, xc.tong_ve, xc.gia_ve " +
                "FROM xuat_chieu xc " +
                "JOIN phim p ON xc.phim_id = p.id " +
                "WHERE xc.phim_id = ? " +
                "ORDER BY xc.ngay DESC, xc.gio DESC")) {
            ps.setInt(1, Integer.parseInt(movie.id));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String[] p = new String[]{
                    rs.getString("id"),           // 0: show id
                    rs.getString("ten_phim"),     // 1: movie title
                    rs.getString("ngay_gio"),     // 2: datetime (combined)
                    rs.getString("phong_id"),     // 3: room id
                    rs.getString("tong_ve"),      // 4: total seats
                    rs.getString("gia_ve")        // 5: price
                };
                System.out.println("DEBUG: Show ID=" + p[0] + ", Price=" + p[5] + ", TotalSeats=" + p[4]);
                rows.add(p);
            }
        } catch (Exception ex) {
            // Fallback to server if DB fails
        }

        // PRIORITY 2: Try server if DB didn't work
        if (rows.isEmpty() && serverAvailable) {
            List<String> lines = sendRequestMultiLine("GET_SHOWS;" + movie.id);
            if (lines != null) {
                for (String L : lines) {
                    if (L == null || L.trim().isEmpty() || L.startsWith("ERR")) continue;
                    String[] p = L.split(";", -1);
                    rows.add(p);
                }
            }
        }

        // PRIORITY 3: Fallback to local data (old cached data)
        if (rows.isEmpty()) {
            for (Show s : localShows.values()) {
                if (s.movieId.equals(movie.id)) {
                    String[] p = new String[]{s.id, movie.title, s.datetime, String.valueOf(s.roomId), String.valueOf(s.totalSeats), String.valueOf(s.price)};
                    rows.add(p);
                }
            }
        }

        // SMART ROOM SELECTION: Skip dialog if only 1 show
        if (rows.size() == 1) {
            String[] sel = rows.get(0);
            String showId = sel[0];
            int roomId = safeParseInt(sel[3], 1);
            int totalSeats = safeParseInt(sel[4], 100);
            int price = (int)Double.parseDouble(sel[5]);
            new SeatMapUI(Integer.parseInt(showId), roomId, sel[2], price, (sid, seats, total) -> {
                addToCartFromSeats(movie, sid, seats, total);
            }).setVisible(true);
            return;
        }

        // Multiple shows: show dialog
        JDialog dlg = new JDialog(this, "Suất chiếu - " + movie.title, true);
        dlg.setSize(800, 450);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(AppTheme.BG_LIGHT);

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setBackground(AppTheme.BG_LIGHT);
        list.setForeground(AppTheme.TEXT_DARK);
        list.setFont(AppTheme.FONT_LABEL_REGULAR);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add formatted display with proper price formatting
        for (String[] p : rows) {
            int price = (int)Double.parseDouble(p[5]);
            String formattedPrice = String.format("%,d", price);
            model.addElement(p[2] + "  |  Phòng " + p[3] + "  |  " + formattedPrice + " VND");
        }
        
        JScrollPane sp = new JScrollPane(list);
        sp.getViewport().setBackground(AppTheme.BG_LIGHT);
        sp.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.PRIMARY_BLUE, 2));

        dlg.setLayout(new BorderLayout());
        dlg.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(AppTheme.BG_LIGHT);
        
        JButton btnChoose = new JButton("Chọn suất");
        btnChoose.setBackground(AppTheme.PRIMARY_BLUE);
        btnChoose.setForeground(Color.WHITE);
        btnChoose.setFont(AppTheme.FONT_LABEL_BOLD);
        btnChoose.setFocusPainted(false);
        btnChoose.setBorderPainted(false);
        btnChoose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton btnCancel = new JButton("Hủy");
        btnCancel.setBackground(AppTheme.ERROR_RED);
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(AppTheme.FONT_LABEL_BOLD);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        bottom.add(btnChoose);
        bottom.add(btnCancel);
        dlg.add(bottom, BorderLayout.SOUTH);

        btnChoose.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx < 0) {
                MessageDialog.showInfo(dlg, "Vui lòng chọn 1 suất!");
                return;
            }
            String[] sel = rows.get(idx);
            String showId = sel[0];
            int roomId = safeParseInt(sel[3], 1);
            int totalSeats = safeParseInt(sel[4], 100);
            int price = (int)Double.parseDouble(sel[5]);

            dlg.dispose();
            new SeatMapUI(Integer.parseInt(showId), roomId, sel[2], price, (sid, seats, total) -> {
                addToCartFromSeats(movie, sid, seats, total);
            }).setVisible(true);
        });

        btnCancel.addActionListener(e -> dlg.dispose());

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
                MessageDialog.showInfo(dlg, "Vui lòng chọn ít nhất 1 ghế.");
                return;
            }
            String seatsCsv = String.join(",", selected);
            if (serverAvailable) {
                String req = String.format("BOOK_TICKET;%s;%s;%s", showId, currentUser == null ? "guest" : currentUser, seatsCsv);
                String resp = sendRequestSingleLine(req);
                if (resp != null && resp.startsWith("OK")) {
                    // success
                    MessageDialog.showInfo(dlg, "Đặt vé thành công (server).");
                    dlg.dispose();
                } else {
                    MessageDialog.showInfo(dlg, resp == null ? "Server không phản hồi" : resp);
                }
            } else {
                double amount = selected.size() * price;
                boolean saved = saveBookingToDB(showId, currentUser == null ? "guest" : currentUser, selected, amount);
                Booking b = new Booking(UUID.randomUUID().toString(), currentUser == null ? "guest" : currentUser, showId, new ArrayList<>(selected), amount, Timestamp.valueOf(LocalDateTime.now()));
                localBookings.add(b);
                if (saved) {
                    MessageDialog.showInfo(dlg, "Đặt vé thành công (lưu vào SQL).");
                    dlg.dispose();
                } else {
                    MessageDialog.showInfo(dlg, "Đặt vé thành công (local) — nhưng không thể lưu vào SQL.");
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
        // Try DB: select from ve join xuat_chieu, phim, ghe - use LIKE for flexible matching
        try (Connection conn = CSDL.getConnection()) {
            String sql = "SELECT v.id AS ve_id, p.ten_phim, xc.ngay, xc.gio, ph.ten_phong, g.ma_ghe, v.tong_tien, v.trang_thai, v.ngay_dat " +
                    "FROM ve v " +
                    "JOIN xuat_chieu xc ON v.xuat_chieu_id = xc.id " +
                    "JOIN phim p ON xc.phim_id = p.id " +
                    "JOIN ghe g ON v.ghe_id = g.id " +
                    "JOIN phong_chieu ph ON xc.phong_id = ph.id " +
                    "JOIN nguoi_dung nd ON v.nguoi_dung_id = nd.id " +
                    "WHERE nd.gmail LIKE ? OR nd.so_dien_thoai LIKE ? OR nd.ho_ten LIKE ? " +
                    "ORDER BY v.ngay_dat DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String pattern = "%" + currentUser + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
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
                            rs.getDouble("tong_tien"),
                            rs.getString("trang_thai"),
                            rs.getTimestamp("ngay_dat").toString()
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

    // Show detailed booking history in a new window with table format
    private void showDetailedBookingHistory(int userId, String userName) {
        JFrame historyFrame = new JFrame("Lịch sử đặt vé - " + userName);
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyFrame.setSize(1000, 600);
        historyFrame.setLocationRelativeTo(this);

        // Create table for history
        javax.swing.table.DefaultTableModel historyModel = new javax.swing.table.DefaultTableModel(
            new String[]{"Mã vé", "Phim", "Phòng", "Ghế", "Suất chiếu", "Giá vé", "Combo", "Trạng thái", "Ngày đặt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable historyTable = new JTable(historyModel);
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scrollPane = new JScrollPane(historyTable);

        // Load data from database
        try (Connection conn = CSDL.getConnection()) {
            String sql = "SELECT v.id, p.ten_phim, pc.ten_phong, g.ma_ghe, x.ngay, x.gio, " +
                    "x.gia_ve, COALESCE(c.ten_combo, '-'), v.trang_thai, v.ngay_dat " +
                    "FROM ve v " +
                    "JOIN xuat_chieu x ON v.xuat_chieu_id = x.id " +
                    "JOIN phim p ON x.phim_id = p.id " +
                    "JOIN phong_chieu pc ON x.phong_id = pc.id " +
                    "JOIN ghe g ON v.ghe_id = g.id " +
                    "LEFT JOIN combo c ON v.combo_id = c.id " +
                    "WHERE v.nguoi_dung_id = ? " +
                    "ORDER BY v.ngay_dat DESC";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                
                while (rs.next()) {
                    historyModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("ten_phim"),
                        rs.getString("ten_phong"),
                        rs.getString("ma_ghe"),
                        rs.getString("ngay") + " " + rs.getString("gio"),
                        rs.getInt("gia_ve") + " VND",
                        rs.getString(8),  // Combo name
                        rs.getString("trang_thai"),
                        rs.getTimestamp("ngay_dat").toString()
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            MessageDialog.showInfo(historyFrame, "Lỗi tải lịch sử: " + ex.getMessage());
        }

        historyFrame.add(scrollPane);
        historyFrame.setVisible(true);
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

    private void addToCartFromSeats(Movie movie, int showId, java.util.List<String> seats, int totalPrice) {
        if (seats == null || seats.isEmpty()) {
            MessageDialog.showInfo(this, "Vui lòng chọn ít nhất 1 ghế!");
            return;
        }
        
        if (currentUser == null || currentUser.isEmpty()) {
            MessageDialog.showInfo(this, "Vui lòng đăng nhập!");
            return;
        }
        
        try (Connection conn = CSDL.getConnection()) {
            // Get user ID from username (ho_ten)
            PreparedStatement ps1 = conn.prepareStatement("SELECT id FROM nguoi_dung WHERE ho_ten = ?");
            ps1.setString(1, currentUser);
            ResultSet rs1 = ps1.executeQuery();
            
            if (!rs1.next()) {
                MessageDialog.showInfo(this, "Người dùng không tồn tại!");
                return;
            }
            
            int userId = rs1.getInt("id");
            
            // Insert each seat as a ve record
            String sqlInsert = "INSERT INTO ve (nguoi_dung_id, xuat_chieu_id, ghe_id, trang_thai, tong_tien) VALUES (?, ?, ?, 'chua_thanh_toan', ?)";
            PreparedStatement ps2 = conn.prepareStatement(sqlInsert);
            
            int seatPrice = totalPrice / seats.size();
            int insertCount = 0;
            
            for (String seatCode : seats) {
                // Get ghe_id from seat code
                String sqlGetSeat = "SELECT id FROM ghe WHERE phong_id = (SELECT phong_id FROM xuat_chieu WHERE id = ?) AND ma_ghe = ?";
                PreparedStatement ps3 = conn.prepareStatement(sqlGetSeat);
                ps3.setInt(1, showId);
                ps3.setString(2, seatCode);
                ResultSet rs3 = ps3.executeQuery();
                
                if (rs3.next()) {
                    int gheId = rs3.getInt("id");
                    ps2.setInt(1, userId);
                    ps2.setInt(2, showId);
                    ps2.setInt(3, gheId);
                    ps2.setInt(4, seatPrice);
                    ps2.addBatch();
                    insertCount++;
                }
            }
            
            ps2.executeBatch();
            
            // Show beautiful success dialog
            showSuccessDialog(insertCount, totalPrice);
            
        } catch (Exception ex) {
            MessageDialog.showInfo(this, "Lỗi: " + ex.getMessage());
        }
    }
    
    private void showSuccessDialog(int seatCount, int totalPrice) {
        JDialog dlg = new JDialog(this, "Thêm vé thành công", true);
        dlg.setSize(450, 280);
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
                int radius = 50;
                
                // Draw circle background
                g2.setColor(AppTheme.SUCCESS_GREEN);
                g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                
                // Draw checkmark
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int[] xPoints = {x - 20, x - 5, x + 25};
                int[] yPoints = {y + 5, y + 20, y - 15};
                g2.drawPolyline(xPoints, yPoints, 3);
            }
        };
        iconPanel.setBackground(AppTheme.BG_LIGHT);
        iconPanel.setPreferredSize(new Dimension(450, 120));
        mainPanel.add(iconPanel, BorderLayout.NORTH);
        
        // Message panel
        JPanel msgPanel = new JPanel();
        msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
        msgPanel.setBackground(AppTheme.BG_LIGHT);
        
        JLabel lblTitle = new JLabel("Đã thêm " + seatCount + " vé vào giỏ hàng!");
        lblTitle.setFont(AppTheme.FONT_LABEL_BOLD);
        lblTitle.setForeground(AppTheme.TEXT_DARK);
        lblTitle.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        JLabel lblPrice = new JLabel("Tổng tiền: " + String.format("%,d", totalPrice) + " VND");
        lblPrice.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblPrice.setForeground(AppTheme.PRIMARY_BLUE);
        lblPrice.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        
        msgPanel.add(lblTitle);
        msgPanel.add(Box.createVerticalStrut(12));
        msgPanel.add(lblPrice);
        
        mainPanel.add(msgPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(AppTheme.BG_LIGHT);
        
        JButton btnOK = new JButton("OK");
        btnOK.setBackground(AppTheme.SUCCESS_GREEN);
        btnOK.setForeground(Color.WHITE);
        btnOK.setFont(AppTheme.FONT_LABEL_BOLD);
        btnOK.setFocusPainted(false);
        btnOK.setBorderPainted(false);
        btnOK.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOK.setPreferredSize(new Dimension(150, 45));
        btnOK.addActionListener(e -> dlg.dispose());
        
        btnPanel.add(btnOK);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);
        
        dlg.add(mainPanel);
        dlg.setVisible(true);
    }

    private int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception ex) { return def; }
    }
    private double safeParseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception ex) { return def; }
    }

    private void filterMovies(String filter) {
        movieGridPanel.removeAll();
        
        try (Connection conn = CSDL.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String sql = "SELECT id, ten_phim, the_loai, thoi_luong, poster FROM phim";
            
            if ("Đang chiếu".equals(filter)) {
                // Đang chiếu = có lịch chiếu
                sql += " WHERE id IN (SELECT DISTINCT phim_id FROM xuat_chieu)";
            } else if ("Sắp chiếu".equals(filter)) {
                // Sắp chiếu = không có lịch chiếu
                sql += " WHERE id NOT IN (SELECT DISTINCT phim_id FROM xuat_chieu)";
            }
            
            ResultSet rs = stmt.executeQuery(sql);
            List<String[]> rows = new ArrayList<>();
            
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("ten_phim"),
                    rs.getString("the_loai"),
                    String.valueOf(rs.getInt("thoi_luong")),
                    rs.getString("poster")
                });
            }
            
            if (rows.isEmpty()) {
                movieGridPanel.add(new JLabel("Không tìm thấy phim"));
            } else {
                updateMovieGridFromRows(rows);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            updateMovieGridFromLocal();
        }
    }

    // Add items to shopping cart (creates temporary booking entries with status 'chua_thanh_toan')
    private void addItemsToCart(int userId, int showId, int roomId, Set<String> seats, int ticketPrice, String movieName) {
        try (Connection conn = CSDL.getConnection()) {
            conn.setAutoCommit(false);
            
            for (String seatName : seats) {
                // Get seat ID
                try (PreparedStatement psSeat = conn.prepareStatement(
                    "SELECT id FROM ghe WHERE ma_ghe = ? AND phong_id = ?")) {
                    psSeat.setString(1, seatName);
                    psSeat.setInt(2, roomId);
                    ResultSet rsSeat = psSeat.executeQuery();
                    
                    if (rsSeat.next()) {
                        int seatId = rsSeat.getInt("id");
                        
                        // Insert into cart (use ve table with status 'chua_thanh_toan')
                        try (PreparedStatement psInsert = conn.prepareStatement(
                            "INSERT INTO ve (nguoi_dung_id, xuat_chieu_id, ghe_id, combo_id, tong_tien, trang_thai, ngay_dat) " +
                            "VALUES (?, ?, ?, NULL, ?, 'chua_thanh_toan', NOW())")) {
                            psInsert.setInt(1, userId);
                            psInsert.setInt(2, showId);
                            psInsert.setInt(3, seatId);
                            psInsert.setLong(4, ticketPrice);
                            psInsert.executeUpdate();
                        }
                    }
                }
            }
            
            conn.commit();
            conn.setAutoCommit(true);
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            MessageDialog.showInfo(this, "Lỗi thêm vào giỏ: " + ex.getMessage());
        }
    }

    // ---------------- Local sample data (fallback) ----------------
    private void prepareLocalSampleData() {
        localMovies.clear();
        localShows.clear();
        localSeatsByRoom.clear();
        localBookings.clear();
        localUsers.clear();

        // Không thêm dữ liệu mẫu nữa - chỉ dùng database thực
        // Tất cả phim và suất chiếu sẽ được tải từ database

        for (int room=1; room<=10; room++) {
            int cap = (room <= 5) ? 100 : (room <= 8) ? 150 : 200;
            localSeatsByRoom.put(room, generateSeatLabels(cap));
        }

        localUsers.put("admin@example.com", new LocalUser("Admin", "admin@example.com", "0123456789", "admin123"));
        localUsers.put("user1@example.com", new LocalUser("User One", "user1@example.com", "0987654321", "123456"));
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

    // ============= Gradient Panel =============
    static class GradientPanel extends JPanel {
        private final Color colorStart;
        private final Color colorEnd;
        
        public GradientPanel(Color start, Color end) {
            this.colorStart = start;
            this.colorEnd = end;
            setOpaque(true);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            GradientPaint gradient = new GradientPaint(0, 0, colorStart, 
                                                       getWidth(), getHeight(), colorEnd);
            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    // ============= Movie Card Panel =============
    static class MovieCard extends JPanel {
        public MovieCard(String title, String genre, BufferedImage poster, Runnable onBookClick) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(224, 224, 224), 1));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(180, 320));
            setPreferredSize(new Dimension(180, 320));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            
            // Poster - căn giữa
            JLabel lblPoster = new JLabel();
            lblPoster.setAlignmentX(Component.CENTER_ALIGNMENT);
            if (poster != null) {
                Image scaledImg = poster.getScaledInstance(160, 190, Image.SCALE_SMOOTH);
                lblPoster.setIcon(new ImageIcon(scaledImg));
            } else {
                lblPoster.setText("Không có ảnh");
                lblPoster.setHorizontalAlignment(SwingConstants.CENTER);
                lblPoster.setPreferredSize(new Dimension(160, 190));
                lblPoster.setMaximumSize(new Dimension(160, 190));
                lblPoster.setBackground(new Color(240, 240, 240));
                lblPoster.setOpaque(true);
            }
            add(lblPoster);
            add(Box.createVerticalStrut(6));
            
            // Title - căn giữa
            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblTitle.setForeground(new Color(33, 33, 33));
            lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
            lblTitle.setMaximumSize(new Dimension(160, 32));
            lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(lblTitle);
            add(Box.createVerticalStrut(4));
            
            // Genre - căn giữa
            JLabel lblGenre = new JLabel(genre);
            lblGenre.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblGenre.setForeground(new Color(117, 117, 117));
            lblGenre.setHorizontalAlignment(SwingConstants.CENTER);
            lblGenre.setMaximumSize(new Dimension(160, 24));
            lblGenre.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(lblGenre);
            add(Box.createVerticalStrut(8));
            
            // Book button - căn giữa
            RoundedButton btnBook = new RoundedButton("Đặt vé");
            btnBook.setBackground(new Color(25, 103, 210));
            btnBook.setForeground(Color.WHITE);
            btnBook.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnBook.setMaximumSize(new Dimension(140, 32));
            btnBook.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnBook.setMaximumSize(new Dimension(160, 32));
            btnBook.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnBook.setBorder(new EmptyBorder(2, 12, 2, 12));
            btnBook.addActionListener(e -> onBookClick.run());
            add(Box.createVerticalStrut(4));
            add(btnBook);
            add(Box.createVerticalStrut(8));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw shadow
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
            
            // Draw white background
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 8, 8);
            
            super.paintComponent(g);
        }
    }

    // ============= Main ----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainUI app = new MainUI();
            app.setVisible(true);
        });
    }
}
