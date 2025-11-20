package quanly;

import mysql.CSDL;
import utils.AppTheme;
import utils.MessageDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

// ============= Custom Rounded Button =============
class RoundedAdminButton extends JButton {
    private static final int CORNER_RADIUS = 8;
    private Color hoverColor;
    private Color pressColor;
    private boolean isHovered = false;

    public RoundedAdminButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFont(new Font("Segoe UI", Font.BOLD, 11));
        
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                isHovered = true;
                repaint();
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }
    
    public void setHoverColor(Color hover, Color press) {
        this.hoverColor = hover;
        this.pressColor = press;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw shadow effect
        g2.setColor(new Color(0, 0, 0, 20));
        g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, CORNER_RADIUS, CORNER_RADIUS);

        // Draw rounded background with hover effect
        Color bgColor = getBackground();
        if (isHovered && hoverColor != null) {
            bgColor = hoverColor;
        }
        if (getModel().isPressed() && pressColor != null) {
            bgColor = pressColor;
        }
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);

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

// ============= Custom Table Header Renderer =============
class TableHeaderRenderer extends DefaultTableCellRenderer {
    public TableHeaderRenderer() {
        setBackground(AppTheme.PRIMARY_BLUE);
        setForeground(Color.WHITE);
        setFont(AppTheme.FONT_TABLE_HEADER);
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(true);
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.PRIMARY_BLUE_LIGHT));
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setBackground(AppTheme.PRIMARY_BLUE);
        setForeground(Color.WHITE);
        setText(value != null ? value.toString() : "");
        setOpaque(true);
        return this;
    }
}

// ============= Custom Table Cell Renderer =============
class TableCellRenderer extends DefaultTableCellRenderer {
    public TableCellRenderer() {
        setBackground(Color.WHITE);
        setForeground(AppTheme.TEXT_DARK);
        setHorizontalAlignment(SwingConstants.CENTER);
        setFont(AppTheme.FONT_TABLE_CELL);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (isSelected) {
            setBackground(AppTheme.SELECTION_BG);
            setForeground(AppTheme.PRIMARY_BLUE);
            setFont(new Font(AppTheme.FONT_FAMILY, Font.BOLD, 11));
        } else {
            if (row % 2 == 0) {
                setBackground(AppTheme.BG_LIGHT);
            } else {
                setBackground(Color.WHITE);
            }
            setForeground(AppTheme.TEXT_DARK);
            setFont(AppTheme.FONT_TABLE_CELL);
        }
        setText(value != null ? value.toString() : "");
        setOpaque(true);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_LIGHTER));
        return this;
    }
}

// ==================== Custom Tab UI for Modern Look ====================
class ModernTabUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
    @Override
    protected void paintTabBackground(java.awt.Graphics g, int tabPlacement, int tabIndex,
                                     int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (isSelected) {
            // Selected tab: blue with gradient
            g2.setColor(AppTheme.PRIMARY_BLUE);
            g2.fillRect(x, y, w, h - 2);
            g2.setColor(AppTheme.PRIMARY_BLUE_LIGHT);
            g2.fillRect(x, y, w, 3);
        } else {
            // Unselected tab: light gray
            g2.setColor(new Color(230, 230, 230));
            g2.fillRect(x, y, w, h);
        }
    }

    @Override
    protected void paintTabBorder(java.awt.Graphics g, int tabPlacement, int tabIndex,
                                 int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(AppTheme.BORDER_LIGHT);
        g2.drawRect(x, y, w - 1, h - 1);
    }

    @Override
    protected void paintFocusIndicator(java.awt.Graphics g, int tabPlacement, java.awt.Rectangle[] rects,
                                      int tabIndex, java.awt.Rectangle iconRect, java.awt.Rectangle textRect,
                                      boolean isSelected) {
        // Remove focus indicator for cleaner look
    }
}

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

    public QuanlyUI(int adminId, String adminName) {
        this();
        setTitle("Admin Panel - " + adminName);
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

        setLayout(new BorderLayout());
        
        // Header gradient with modern design
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Gradient background
                GradientPaint gradient = new GradientPaint(0, 0, AppTheme.PRIMARY_BLUE,
                                                          getWidth(), getHeight(), AppTheme.PRIMARY_BLUE_LIGHT);
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Bottom shadow line
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
        };
        headerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, AppTheme.PADDING_LARGE, 15));
        headerPanel.setPreferredSize(new Dimension(getWidth(), AppTheme.HEADER_HEIGHT));
        headerPanel.setOpaque(false);
        
        JLabel logoHeader = new JLabel("⚙️ Quản Lý Hệ Thống Rạp Phim");
        logoHeader.setFont(AppTheme.FONT_HEADER_LARGE);
        logoHeader.setForeground(Color.WHITE);
        logoHeader.setIconTextGap(12);
        headerPanel.add(logoHeader);
        
        add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(AppTheme.BG_LIGHT);
        tabbedPane.setForeground(AppTheme.TEXT_DARK);
        tabbedPane.setFont(AppTheme.FONT_LABEL_BOLD);
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setUI(new ModernTabUI());
        
        // Style tab pane
        UIManager.put("TabbedPane.selected", AppTheme.PRIMARY_BLUE);
        UIManager.put("TabbedPane.foreground", AppTheme.TEXT_DARK);
        UIManager.put("TabbedPane.background", AppTheme.BG_LIGHT);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 1, 1, 1));
        UIManager.put("TabbedPane.tabAreaBackground", AppTheme.BG_LIGHT);
        UIManager.put("TabbedPane.unselectedBackground", new Color(230, 230, 230));
        UIManager.put("TabbedPane.selectHighlight", AppTheme.PRIMARY_BLUE_LIGHT);
        
        add(tabbedPane, BorderLayout.CENTER);

        buildMoviesTab();
        buildShowsTab();
        buildRoomsTab();
        buildSeatsTab();
        // buildUsersTab(); // Commented out - not fully implemented
        // buildBookingsTab(); // Commented out - use ve tab instead
        buildStatsTab();
    }



    // ---------------------- Movies Tab ----------------------
    private void buildMoviesTab() {
        JPanel panel = new JPanel(new BorderLayout(AppTheme.PADDING_LARGE, AppTheme.PADDING_LARGE));
        panel.setBackground(AppTheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(AppTheme.PADDING_LARGE, AppTheme.PADDING_LARGE, AppTheme.PADDING_LARGE, AppTheme.PADDING_LARGE));

        // Form (left) - with modern styling
        JPanel form = createStyledFormPanel();
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(AppTheme.PADDING_SMALL, AppTheme.PADDING_SMALL, AppTheme.PADDING_SMALL, AppTheme.PADDING_SMALL);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0;
        JLabel lbl0 = new JLabel("ID (tự tạo/để trống):");
        lbl0.setFont(AppTheme.FONT_LABEL_BOLD);
        lbl0.setForeground(AppTheme.TEXT_DARK);
        form.add(lbl0, g);
        g.gridx = 1;
        tfMovieId = new JTextField();
        tfMovieId.setFont(AppTheme.FONT_INPUT);
        form.add(tfMovieId, g);

        g.gridx = 0; g.gridy++;
        JLabel lbl1 = new JLabel("Tên Phim:");
        lbl1.setFont(AppTheme.FONT_LABEL_BOLD);
        lbl1.setForeground(AppTheme.TEXT_DARK);
        form.add(lbl1, g);
        g.gridx = 1;
        tfMovieTitle = new JTextField();
        tfMovieTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        form.add(tfMovieTitle, g);

        g.gridx = 0; g.gridy++;
        JLabel lbl2 = new JLabel("Thể Loại:");
        lbl2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl2.setForeground(new Color(33, 33, 33));
        form.add(lbl2, g);
        g.gridx = 1;
        tfMovieGenre = new JTextField();
        tfMovieGenre.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        form.add(tfMovieGenre, g);

        g.gridx = 0; g.gridy++;
        JLabel lbl3 = new JLabel("Độ dài (phút):");
        lbl3.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl3.setForeground(new Color(33, 33, 33));
        form.add(lbl3, g);
        g.gridx = 1;
        spnMovieDuration = new JSpinner(new SpinnerNumberModel(90, 1, 999, 1));
        form.add(spnMovieDuration, g);

        g.gridx = 0; g.gridy++;
        JLabel lbl4 = new JLabel("Ảnh (path/url):");
        lbl4.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl4.setForeground(new Color(33, 33, 33));
        form.add(lbl4, g);
        g.gridx = 1;
        tfMovieImage = new JTextField();
        tfMovieImage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        enableDragDrop(tfMovieImage);
        form.add(tfMovieImage, g);

        g.gridx = 1; g.gridy++;
        JPanel fileBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fileBtns.setBackground(new Color(252, 253, 255));
        RoundedAdminButton btnChooseImage = new RoundedAdminButton("Chọn Ảnh...");
        styleButton(btnChooseImage, new Color(66, 133, 244), new Color(100, 160, 255), new Color(25, 103, 210));
        btnChooseImage.setPreferredSize(new Dimension(130, 32));
        btnChooseImage.addActionListener(e -> chooseImageFor(tfMovieImage));
        fileBtns.add(btnChooseImage);
        form.add(fileBtns, g);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btns.setBackground(new Color(252, 253, 255));
        
        RoundedAdminButton btnAdd = new RoundedAdminButton("+ Thêm Phim");
        styleButton(btnAdd, AppTheme.SUCCESS_GREEN, AppTheme.SUCCESS_GREEN_LIGHT, AppTheme.SUCCESS_GREEN_DARK);
        btnAdd.setPreferredSize(new Dimension(AppTheme.BUTTON_WIDTH, AppTheme.BUTTON_HEIGHT));
        
        RoundedAdminButton btnUpdate = new RoundedAdminButton("✎ Sửa Phim");
        styleButton(btnUpdate, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE);
        btnUpdate.setPreferredSize(new Dimension(AppTheme.BUTTON_WIDTH, AppTheme.BUTTON_HEIGHT));
        
        RoundedAdminButton btnDelete = new RoundedAdminButton("Xóa Phim");
        styleButton(btnDelete, AppTheme.ERROR_RED, AppTheme.ERROR_RED_LIGHT, AppTheme.ERROR_RED_DARK);
        btnDelete.setPreferredSize(new Dimension(AppTheme.BUTTON_WIDTH, AppTheme.BUTTON_HEIGHT));
        
        btns.add(btnAdd); btns.add(btnUpdate); btns.add(btnDelete);
        g.gridx = 1; g.gridy++;
        form.add(btns, g);

        // Table (right)
        modelMovies = new DefaultTableModel(new Object[]{"id","ten_phim","the_loai","thoi_luong","anh"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblMovies = new JTable(modelMovies);
        tblMovies.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMovies.getTableHeader().setBackground(AppTheme.PRIMARY_BLUE);
        tblMovies.getTableHeader().setForeground(Color.WHITE);
        tblMovies.getTableHeader().setFont(AppTheme.FONT_TABLE_HEADER);
        tblMovies.setRowHeight(28);
        tblMovies.setGridColor(AppTheme.BORDER_LIGHT);
        tblMovies.setBackground(Color.WHITE);
        tblMovies.setForeground(AppTheme.TEXT_DARK);
        tblMovies.setSelectionBackground(AppTheme.SELECTION_BG);
        tblMovies.setSelectionForeground(AppTheme.PRIMARY_BLUE);
        
        // Set custom header and cell renderers
        TableHeaderRenderer headerRenderer = new TableHeaderRenderer();
        for (int i = 0; i < tblMovies.getColumnCount(); i++) {
            tblMovies.getColumn(tblMovies.getColumnName(i)).setHeaderRenderer(headerRenderer);
            tblMovies.getColumn(tblMovies.getColumnName(i)).setCellRenderer(new TableCellRenderer());
        }
        
        JScrollPane spMovies = createStyledScrollPane(tblMovies);

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
        JPanel panel = new JPanel(new BorderLayout(AppTheme.PADDING_SMALL, AppTheme.PADDING_SMALL));
        panel.setBackground(AppTheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_GRAY));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(AppTheme.PADDING_SMALL, AppTheme.PADDING_SMALL, AppTheme.PADDING_SMALL, AppTheme.PADDING_SMALL); g.fill = GridBagConstraints.HORIZONTAL; g.gridx = 0; g.gridy = 0;

        form.add(new JLabel("Phim:"), g);
        g.gridx = 1;
        cbMovieForShow = new JComboBox<>();
        cbMovieForShow.addActionListener(e -> autoFillShowDetails());
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

        RoundedAdminButton btnChooseShowImg = new RoundedAdminButton("Chọn ảnh...");
        styleButton(btnChooseShowImg, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE);
        btnChooseShowImg.setPreferredSize(new Dimension(AppTheme.BUTTON_SMALL_WIDTH, AppTheme.BUTTON_SMALL_HEIGHT));
        btnChooseShowImg.addActionListener(e -> chooseImageFor(tfShowImage));
        g.gridx = 1; g.gridy++;
        form.add(btnChooseShowImg, g);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.PADDING_MEDIUM, 0));
        btns.setBackground(Color.WHITE);
        
        RoundedAdminButton btnAddShow = new RoundedAdminButton("+ Thêm Suất");
        styleButton(btnAddShow, AppTheme.SUCCESS_GREEN, AppTheme.SUCCESS_GREEN_LIGHT, AppTheme.SUCCESS_GREEN_DARK);
        btnAddShow.setPreferredSize(new Dimension(AppTheme.BUTTON_WIDTH, AppTheme.BUTTON_HEIGHT));
        
        RoundedAdminButton btnUpdateShow = new RoundedAdminButton("✎ Sửa Suất");
        styleButton(btnUpdateShow, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE);
        btnUpdateShow.setPreferredSize(new Dimension(AppTheme.BUTTON_WIDTH, AppTheme.BUTTON_HEIGHT));
        
        RoundedAdminButton btnDeleteShow = new RoundedAdminButton("Xóa Suất");
        styleButton(btnDeleteShow, AppTheme.ERROR_RED, AppTheme.ERROR_RED_LIGHT, AppTheme.ERROR_RED_DARK);
        btnDeleteShow.setPreferredSize(new Dimension(AppTheme.BUTTON_WIDTH, AppTheme.BUTTON_HEIGHT));
        
        btns.add(btnAddShow); btns.add(btnUpdateShow); btns.add(btnDeleteShow);
        g.gridx = 1; g.gridy++;
        form.add(btns, g);

        // table: tách riêng ngày và giờ
        modelShows = new DefaultTableModel(new String[]{"id","movie_name","ngay","gio","room_name","tong_ve","gia_ve","anh"},0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblShows = new JTable(modelShows);
        tblShows.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblShows.getTableHeader().setBackground(AppTheme.PRIMARY_BLUE);
        tblShows.getTableHeader().setForeground(Color.WHITE);
        tblShows.getTableHeader().setFont(AppTheme.FONT_TABLE_HEADER);
        tblShows.setRowHeight(28);
        tblShows.setGridColor(AppTheme.BORDER_LIGHT);
        tblShows.setBackground(Color.WHITE);
        tblShows.setForeground(AppTheme.TEXT_DARK);
        tblShows.setSelectionBackground(AppTheme.SELECTION_BG);
        tblShows.setSelectionForeground(AppTheme.TEXT_DARK);
        
        // Set custom header and cell renderers
        for (int i = 0; i < tblShows.getColumnCount(); i++) {
            tblShows.getColumn(tblShows.getColumnName(i)).setHeaderRenderer(new TableHeaderRenderer());
            tblShows.getColumn(tblShows.getColumnName(i)).setCellRenderer(new TableCellRenderer());
        }
        
        JScrollPane spShows = new JScrollPane(tblShows);
        spShows.getViewport().setBackground(Color.WHITE);

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
        JPanel panel = new JPanel(new BorderLayout(AppTheme.PADDING_SMALL, AppTheme.PADDING_SMALL));
        panel.setBackground(AppTheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM));

        // Left: rooms table
        modelRooms = new DefaultTableModel(new String[]{"id","ten_phong","suc_chua","trang_thai"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblRooms = new JTable(modelRooms);
        tblRooms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblRooms.getTableHeader().setBackground(AppTheme.PRIMARY_BLUE);
        tblRooms.getTableHeader().setForeground(Color.WHITE);
        tblRooms.getTableHeader().setFont(AppTheme.FONT_TABLE_HEADER);
        tblRooms.setRowHeight(28);
        tblRooms.setGridColor(AppTheme.BORDER_LIGHT);
        tblRooms.setBackground(Color.WHITE);
        tblRooms.setForeground(AppTheme.TEXT_DARK);
        tblRooms.setSelectionBackground(AppTheme.SELECTION_BG);
        tblRooms.setSelectionForeground(AppTheme.TEXT_DARK);
        
        // Set custom header and cell renderers
        for (int i = 0; i < tblRooms.getColumnCount(); i++) {
            tblRooms.getColumn(tblRooms.getColumnName(i)).setHeaderRenderer(new TableHeaderRenderer());
            tblRooms.getColumn(tblRooms.getColumnName(i)).setCellRenderer(new TableCellRenderer());
        }
        
        JScrollPane spRooms = new JScrollPane(tblRooms);
        spRooms.getViewport().setBackground(Color.WHITE);

        // Right: shows for selected room (with ngay/gio split)
        modelRoomShows = new DefaultTableModel(new String[]{"id","phim","ngay","gio","tong_ve","ve_con","gia_ve"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblRoomShows = new JTable(modelRoomShows);
        tblRoomShows.getTableHeader().setBackground(AppTheme.PRIMARY_BLUE);
        tblRoomShows.getTableHeader().setForeground(Color.WHITE);
        tblRoomShows.getTableHeader().setFont(AppTheme.FONT_TABLE_HEADER);
        tblRoomShows.setRowHeight(28);
        tblRoomShows.setGridColor(AppTheme.BORDER_LIGHT);
        tblRoomShows.setBackground(Color.WHITE);
        tblRoomShows.setForeground(AppTheme.TEXT_DARK);
        tblRoomShows.setSelectionBackground(AppTheme.SELECTION_BG);
        tblRoomShows.setSelectionForeground(AppTheme.TEXT_DARK);
        
        // Set custom header and cell renderers
        for (int i = 0; i < tblRoomShows.getColumnCount(); i++) {
            tblRoomShows.getColumn(tblRoomShows.getColumnName(i)).setHeaderRenderer(new TableHeaderRenderer());
            tblRoomShows.getColumn(tblRoomShows.getColumnName(i)).setCellRenderer(new TableCellRenderer());
        }
        
        JScrollPane spRoomShows = new JScrollPane(tblRoomShows);
        spRoomShows.getViewport().setBackground(Color.WHITE);

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

    // ---------------------- Seats Tab ----------------------
    private void buildSeatsTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(AppTheme.BG_LIGHT);
        p.setBorder(new EmptyBorder(AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(AppTheme.BG_LIGHT);
        
        JLabel lblRoomSelect = new JLabel("Chọn phòng: ");
        lblRoomSelect.setFont(AppTheme.FONT_LABEL_BOLD);
        lblRoomSelect.setForeground(AppTheme.TEXT_DARK);
        JComboBox<RoomItem> cbRoomSelect = new JComboBox<>();
        RoundedAdminButton btnViewSeats = new RoundedAdminButton("Xem ghế");
        styleButton(btnViewSeats, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE);
        btnViewSeats.setPreferredSize(new Dimension(AppTheme.BUTTON_SMALL_WIDTH, AppTheme.BUTTON_SMALL_HEIGHT));
        
        RoundedAdminButton btnLoadSeats = new RoundedAdminButton("↻ Tải lại");
        styleButton(btnLoadSeats, AppTheme.WARNING_ORANGE, AppTheme.WARNING_ORANGE_LIGHT, AppTheme.WARNING_ORANGE_DARK);
        btnLoadSeats.setPreferredSize(new Dimension(AppTheme.BUTTON_SMALL_WIDTH, AppTheme.BUTTON_SMALL_HEIGHT));
        
        top.add(lblRoomSelect);
        top.add(cbRoomSelect);
        top.add(btnViewSeats);
        top.add(btnLoadSeats);
        p.add(top, BorderLayout.NORTH);
        
        // Bảng ghế
        DefaultTableModel modelSeats = new DefaultTableModel(
            new String[]{"Ghế", "Trạng thái", "Người dùng", "Ngày đặt"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblSeats = new JTable(modelSeats);
        tblSeats.getTableHeader().setBackground(AppTheme.PRIMARY_BLUE);
        tblSeats.getTableHeader().setForeground(Color.WHITE);
        tblSeats.getTableHeader().setFont(AppTheme.FONT_TABLE_HEADER);
        tblSeats.setRowHeight(28);
        tblSeats.setGridColor(AppTheme.BORDER_LIGHT);
        tblSeats.setBackground(Color.WHITE);
        tblSeats.setForeground(AppTheme.TEXT_DARK);
        tblSeats.setSelectionBackground(AppTheme.SELECTION_BG);
        tblSeats.setSelectionForeground(AppTheme.TEXT_DARK);
        
        // Set custom header and cell renderers
        for (int i = 0; i < tblSeats.getColumnCount(); i++) {
            tblSeats.getColumn(tblSeats.getColumnName(i)).setHeaderRenderer(new TableHeaderRenderer());
            tblSeats.getColumn(tblSeats.getColumnName(i)).setCellRenderer(new TableCellRenderer());
        }
        
        JScrollPane sp = new JScrollPane(tblSeats);
        sp.getViewport().setBackground(Color.WHITE);
        p.add(sp, BorderLayout.CENTER);
        
        // Load phòng vào combobox
        btnLoadSeats.addActionListener(e -> {
            cbRoomSelect.removeAllItems();
            try (Connection conn = CSDL.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, ten_phong, suc_chua FROM phong_chieu ORDER BY id")) {
                while (rs.next()) {
                    cbRoomSelect.addItem(new RoomItem(rs.getInt("id"), rs.getString("ten_phong"), rs.getInt("suc_chua")));
                }
            } catch (SQLException ex) {
                MessageDialog.showInfo(this, "Lỗi: " + ex.getMessage());
            }
        });
        
        // View ghế
        btnViewSeats.addActionListener(e -> {
            RoomItem room = (RoomItem) cbRoomSelect.getSelectedItem();
            if (room == null) {
                MessageDialog.showInfo(this, "Chọn phòng trước!");
                return;
            }
            
            modelSeats.setRowCount(0);
            try (Connection conn = CSDL.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "SELECT g.ma_ghe, " +
                    "CASE WHEN EXISTS(SELECT 1 FROM ve WHERE ghe_id = g.id AND trang_thai <> 'da_huy') " +
                    "  THEN 'Đã đặt' ELSE 'Trống' END AS trang_thai, " +
                    "COALESCE(u.ho_ten, '') AS user, COALESCE(x.ngay, '') AS date " +
                    "FROM ghe g " +
                    "LEFT JOIN ve v ON g.id = v.ghe_id " +
                    "LEFT JOIN xuat_chieu x ON v.xuat_chieu_id = x.id " +
                    "LEFT JOIN nguoi_dung u ON v.nguoi_dung_id = u.id " +
                    "WHERE g.phong_id = ? " +
                    "ORDER BY g.ma_ghe")) {
                ps.setInt(1, room.id);
                
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    modelSeats.addRow(new Object[]{
                        rs.getString("ma_ghe"),
                        rs.getString("trang_thai"),
                        rs.getString("user"),
                        rs.getString("date")
                    });
                }
            } catch (SQLException ex) {
                MessageDialog.showInfo(this, "Lỗi: " + ex.getMessage());
            }
        });
        
        tabbedPane.addTab("Quản Lý Ghế", p);
        btnLoadSeats.doClick(); // Load ngay khi khởi tạo
    }

    // ---------------------- Users Tab ----------------------
    private void buildUsersTab() {
        JPanel p = new JPanel(new BorderLayout());
        modelUsers = new DefaultTableModel(new String[]{"id","ho_ten","gmail","sdt"}, 0) {
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
        RoundedAdminButton btnLoad = new RoundedAdminButton("Tải đặt vé");
        styleButton(btnLoad, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE_LIGHT, AppTheme.PRIMARY_BLUE);
        btnLoad.setPreferredSize(new Dimension(AppTheme.BUTTON_WIDTH, AppTheme.BUTTON_SMALL_HEIGHT));
        
        RoundedAdminButton btnDelete = new RoundedAdminButton("Xóa vé chọn");
        styleButton(btnDelete, AppTheme.ERROR_RED, AppTheme.ERROR_RED_LIGHT, AppTheme.ERROR_RED_DARK);
        btnDelete.setPreferredSize(new Dimension(AppTheme.BUTTON_WIDTH, AppTheme.BUTTON_SMALL_HEIGHT));
        
        RoundedAdminButton btnRefresh = new RoundedAdminButton("↻ Làm mới");
        styleButton(btnRefresh, AppTheme.WARNING_ORANGE, AppTheme.WARNING_ORANGE_LIGHT, AppTheme.WARNING_ORANGE_DARK);
        btnRefresh.setPreferredSize(new Dimension(AppTheme.BUTTON_SMALL_WIDTH, AppTheme.BUTTON_SMALL_HEIGHT));
        
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
        p.setBackground(AppTheme.BG_LIGHT);
        p.setBorder(new EmptyBorder(AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM, AppTheme.PADDING_MEDIUM));
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(AppTheme.BG_LIGHT);
        RoundedAdminButton btnUpdate = new RoundedAdminButton("Cập nhật Thống kê");
        styleButton(btnUpdate, AppTheme.SUCCESS_GREEN, AppTheme.SUCCESS_GREEN_LIGHT, AppTheme.SUCCESS_GREEN_DARK);
        btnUpdate.setPreferredSize(new Dimension(180, AppTheme.BUTTON_HEIGHT));
        topPanel.add(btnUpdate);
        p.add(topPanel, BorderLayout.NORTH);
        
        // Tạo bảng thống kê
        DefaultTableModel modelStats = new DefaultTableModel(
            new String[]{"Chỉ tiêu", "Giá trị"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblStats = new JTable(modelStats);
        tblStats.getTableHeader().setBackground(AppTheme.PRIMARY_BLUE);
        tblStats.getTableHeader().setForeground(Color.WHITE);
        tblStats.getTableHeader().setFont(AppTheme.FONT_TABLE_HEADER);
        tblStats.setRowHeight(28);
        tblStats.setGridColor(AppTheme.BORDER_LIGHT);
        tblStats.setBackground(Color.WHITE);
        tblStats.setForeground(AppTheme.TEXT_DARK);
        tblStats.setSelectionBackground(AppTheme.SELECTION_BG);
        tblStats.setSelectionForeground(AppTheme.TEXT_DARK);
        
        // Set custom header and cell renderers
        for (int i = 0; i < tblStats.getColumnCount(); i++) {
            tblStats.getColumn(tblStats.getColumnName(i)).setHeaderRenderer(new TableHeaderRenderer());
            tblStats.getColumn(tblStats.getColumnName(i)).setCellRenderer(new TableCellRenderer());
        }
        
        JScrollPane spStats = new JScrollPane(tblStats);
        spStats.getViewport().setBackground(Color.WHITE);
        p.add(spStats, BorderLayout.CENTER);
        
        btnUpdate.addActionListener(e -> {
            modelStats.setRowCount(0);
            
            try (Connection conn = CSDL.getConnection()) {
                // 1. Tổng số vé đã bán
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ve WHERE trang_thai = 'da_thanh_toan'")) {
                    rs.next();
                    modelStats.addRow(new Object[]{"Số vé đã bán", rs.getInt(1)});
                }
                
                // 2. Tổng số vé chưa thanh toán
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ve WHERE trang_thai = 'chua_thanh_toan'")) {
                    rs.next();
                    modelStats.addRow(new Object[]{"Số vé chưa thanh toán", rs.getInt(1)});
                }
                
                // 3. Tổng doanh thu từ vé
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                        "SELECT COALESCE(SUM(v.tong_tien), 0) FROM ve v WHERE v.trang_thai = 'da_thanh_toan'")) {
                    rs.next();
                    long doanhThu = rs.getLong(1);
                    modelStats.addRow(new Object[]{"Tổng doanh thu (Vé)", String.format("%,d VND", doanhThu)});
                }
                
                // 4. Doanh thu combo
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                        "SELECT COALESCE(SUM(c.gia), 0) FROM ve v " +
                        "JOIN combo c ON v.combo_id = c.id " +
                        "WHERE v.trang_thai = 'da_thanh_toan'")) {
                    rs.next();
                    long comboRevenue = rs.getLong(1);
                    modelStats.addRow(new Object[]{"Doanh thu từ Combo", String.format("%,d VND", comboRevenue)});
                }
                
                // 5. Tổng doanh thu
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                        "SELECT COALESCE(SUM(v.tong_tien), 0) FROM ve v WHERE v.trang_thai = 'da_thanh_toan'")) {
                    rs.next();
                    long totalRevenue = rs.getLong(1);
                    modelStats.addRow(new Object[]{"Tổng doanh thu", String.format("%,d VND", totalRevenue)});
                }
                
                // 6. Số người dùng
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM nguoi_dung WHERE vai_tro = 'user'")) {
                    rs.next();
                    modelStats.addRow(new Object[]{"Tổng số người dùng", rs.getInt(1)});
                }
                
                // 7. Số phim
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM phim")) {
                    rs.next();
                    modelStats.addRow(new Object[]{"Số phim trong hệ thống", rs.getInt(1)});
                }
                
                // 8. Số suất chiếu
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM xuat_chieu")) {
                    rs.next();
                    modelStats.addRow(new Object[]{"Số suất chiếu", rs.getInt(1)});
                }
                
                // 9. Ghế trống
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                        "SELECT COUNT(g.id) FROM ghe g " +
                        "WHERE g.id NOT IN (SELECT ghe_id FROM ve WHERE trang_thai <> 'da_huy')")) {
                    rs.next();
                    modelStats.addRow(new Object[]{"Ghế còn trống", rs.getInt(1)});
                }
                
                // 10. Tỉ lệ sử dụng combo
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                        "SELECT COALESCE(ROUND(100.0 * SUM(CASE WHEN combo_id IS NOT NULL THEN 1 ELSE 0 END) / COUNT(*), 2), 0) " +
                        "FROM ve WHERE trang_thai = 'da_thanh_toan'")) {
                    rs.next();
                    modelStats.addRow(new Object[]{"Tỉ lệ sử dụng Combo (%)", rs.getDouble(1) + " %"});
                }
                
            } catch (SQLException ex) {
                MessageDialog.showInfo(null, "Lỗi: " + ex.getMessage());
            }
        });
        
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

            rs = st.executeQuery("SELECT id,ten_phong,suc_chua FROM phong_chieu ORDER BY id");
            while (rs.next()) cbRoomForShow.addItem(new RoomItem(rs.getInt("id"), rs.getString("ten_phong"), rs.getInt("suc_chua")));
            rs.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            MessageDialog.showInfo(this, "Lỗi load combobox: " + ex.getMessage());
        }
    }

    private void autoFillShowDetails() {
        Object selected = cbMovieForShow.getSelectedItem();
        if (selected == null || !(selected instanceof MovieItem)) return;
        
        MovieItem movie = (MovieItem) selected;
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT the_loai, thoi_luong FROM phim WHERE id = ?")) {
            ps.setInt(1, movie.id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Auto-fill các field từ phim được chọn
                // Genre: the_loai, Duration: thoi_luong
                // Không có poster field trong database
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
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
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this, "Lỗi tải phim: "+ex.getMessage()); }
        loadCombos();
    }

    private void addMovie() {
        String ten = tfMovieTitle.getText().trim();
        String theLoai = tfMovieGenre.getText().trim();
        int thoiLuong = (Integer) spnMovieDuration.getValue();
        String anh = tfMovieImage.getText().trim();

        if (ten.isEmpty()) { MessageDialog.showInfo(this, "Tên phim không được để trống"); return; }

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO phim (ten_phim,the_loai,thoi_luong,anh,ngay_tao) VALUES (?,?,?,?,NOW())")) {
            ps.setString(1, ten);
            ps.setString(2, theLoai);
            ps.setInt(3, thoiLuong);
            ps.setString(4, anh);
            ps.executeUpdate();
            MessageDialog.showInfo(this, "Thêm phim thành công");
            loadMovies();
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this, "Lỗi thêm phim: "+ex.getMessage()); }
    }

    private void updateMovie() {
        int r = tblMovies.getSelectedRow();
        if (r < 0) { MessageDialog.showInfo(this,"Chọn phim cần sửa"); return; }
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
            MessageDialog.showInfo(this, "Sửa phim thành công");
            loadMovies();
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this, "Lỗi sửa phim: "+ex.getMessage()); }
    }

    private void deleteMovie() {
        int r = tblMovies.getSelectedRow();
        if (r < 0) { MessageDialog.showInfo(this,"Chọn phim cần xóa"); return; }
        int id = Integer.parseInt(String.valueOf(modelMovies.getValueAt(r,0)));
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa phim id="+id+" ?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM phim WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            MessageDialog.showInfo(this, "Xóa phim thành công");
            loadMovies();
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this, "Lỗi xóa phim: "+ex.getMessage()); }
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
            MessageDialog.showInfo(this, "Lỗi tải suất chiếu: " + ex.getMessage());
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
            MessageDialog.showInfo(this, "Chọn phim & phòng.");
            return;
        }
        if (giaStr.isEmpty()) {
            MessageDialog.showInfo(this, "Nhập giá vé.");
            return;
        }

        double gia;
        try { gia = Double.parseDouble(giaStr); }
        catch (Exception ex){ MessageDialog.showInfo(this,"Giá vé phải là số"); return; }

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
            MessageDialog.showInfo(this, "Thêm suất chiếu thành công");
            loadShows();
        } catch (Exception ex) {
            ex.printStackTrace();
            MessageDialog.showInfo(this, "Lỗi thêm suất: "+ex.getMessage());
        }
    }

    private void updateShow() {
        int r = tblShows.getSelectedRow();
        if (r < 0) { MessageDialog.showInfo(this,"Chọn suất cần sửa"); return; }
        int showId = Integer.parseInt(String.valueOf(modelShows.getValueAt(r,0)));

        MovieItem mi = (MovieItem) cbMovieForShow.getSelectedItem();
        RoomItem ri = (RoomItem) cbRoomForShow.getSelectedItem();
        java.util.Date dt = (java.util.Date) spnShowDateTime.getValue();
        int tongVe = (Integer) spnShowTickets.getValue();
        String giaStr = tfShowPrice.getText().trim();
        String anh = tfShowImage.getText().trim();
        double gia;
        try { gia = Double.parseDouble(giaStr); }
        catch (Exception ex){ MessageDialog.showInfo(this,"Giá vé phải là số"); return; }

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
            MessageDialog.showInfo(this, "Sửa suất thành công");
            loadShows();
        } catch (Exception ex) {
            ex.printStackTrace();
            MessageDialog.showInfo(this, "Lỗi sửa suất: "+ex.getMessage());
        }
    }


    private void deleteShow() {
        int r = tblShows.getSelectedRow();
        if (r < 0) { MessageDialog.showInfo(this,"Chọn suất cần xóa"); return; }
        int showId = Integer.parseInt(String.valueOf(modelShows.getValueAt(r,0)));
        int conf = JOptionPane.showConfirmDialog(this, "Xóa suất id="+showId+" ?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM xuat_chieu WHERE id=?")) {
            ps.setInt(1, showId);
            ps.executeUpdate();
            MessageDialog.showInfo(this, "Xóa suất thành công");
            loadShows();
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this, "Lỗi xóa suất: "+ex.getMessage()); }
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
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this, "Lỗi tải phòng: " + ex.getMessage()); }
    }

    private void loadShowsForRoom(String roomId) {
        modelRoomShows.setRowCount(0);
        String sql = "SELECT s.id, p.ten_phim, s.ngay, s.gio, s.tong_ve, s.ve_con, s.gia_ve FROM xuat_chieu s JOIN phim p ON s.phim_id = p.id WHERE s.phong_id = ? ORDER BY s.ngay, s.gio";
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(roomId));
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String ngay = rs.getDate("ngay") == null ? "" : dateFormat.format(rs.getDate("ngay"));
                    String gio  = rs.getTime("gio") == null ? "" : timeFormat.format(rs.getTime("gio"));

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
                    MessageDialog.showInfo(this, "Phòng này chưa có lịch chiếu.");
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this, "Lỗi tải lịch phòng: " + ex.getMessage()); }
    }

    // ---- Users ----
    private void loadUsers() {
        modelUsers.setRowCount(0);
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id,ho_ten,gmail,sdt FROM nguoi_dung ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                modelUsers.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("ho_ten"),
                        rs.getString("gmail"),
                        rs.getString("sdt")
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this,"Lỗi tải user: "+ex.getMessage()); }
    }

    // ---- Bookings ----
    private void loadBookings() {
        modelBookings.setRowCount(0);
        // Load từ bảng ve thay vì booking_local
        String sql = "SELECT v.id, xc.id AS show_id, nd.ho_ten, g.ma_ghe, v.tong_tien, v.ngay_dat FROM ve v " +
                     "JOIN xuat_chieu xc ON v.xuat_chieu_id = xc.id " +
                     "JOIN nguoi_dung nd ON v.nguoi_dung_id = nd.id " +
                     "JOIN ghe g ON v.ghe_id = g.id " +
                     "ORDER BY v.ngay_dat DESC";

       try (Connection conn = CSDL.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
           while (rs.next()) {
               modelBookings.addRow(new Object[]{
                   rs.getInt("id"),
                   rs.getInt("show_id"),
                   rs.getString("ho_ten"),
                   rs.getString("ma_ghe"),
                   rs.getDouble("tong_tien"),
                   rs.getTimestamp("ngay_dat")
               });
           }
       } catch (Exception ex) {
           ex.printStackTrace();
           MessageDialog.showInfo(this, "Lỗi tải đặt vé: " + ex.getMessage());
       }

    }

    // Delete selected booking row (booking_local.id)
    private void deleteSelectedBooking() {
        int r = tblBookings.getSelectedRow();
        if (r < 0) { MessageDialog.showInfo(this, "Chọn 1 dòng đặt vé để xóa"); return; }
        int id = Integer.parseInt(String.valueOf(modelBookings.getValueAt(r, 0)));
        int conf = JOptionPane.showConfirmDialog(this, "Xóa đặt vé id="+id+" (giải phóng ghế)?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM booking_local WHERE id = ?")) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                MessageDialog.showInfo(this, "Xóa đặt vé thành công. Ghế đã được giải phóng.");
                loadBookings();
            } else {
                MessageDialog.showInfo(this, "Không tìm thấy đặt vé (có thể đã bị xóa trước đó).");
                loadBookings();
            }
        } catch (Exception ex) { ex.printStackTrace(); MessageDialog.showInfo(this, "Lỗi xóa đặt vé: "+ex.getMessage()); }
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
        int id; 
        String name;
        int capacity;
        RoomItem(int id, String name, int capacity) { 
            this.id = id; 
            this.name = name;
            this.capacity = capacity;
        }
        @Override public String toString(){ 
            return name + " (" + capacity + " ghế)"; 
        }
    }

    // -------------------- Button Style Helper --------------------
    private void styleButton(RoundedAdminButton btn, Color normal, Color hover, Color press) {
        btn.setBackground(normal);
        btn.setForeground(Color.WHITE);
        btn.setFont(AppTheme.FONT_BUTTON);
        btn.setHoverColor(hover, press);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    // -------------------- Form Panel Style Helper --------------------
    private JPanel createStyledFormPanel() {
        JPanel form = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Rounded border effect
                g2.setColor(AppTheme.BORDER_GRAY);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, AppTheme.BORDER_RADIUS, AppTheme.BORDER_RADIUS);
            }
        };
        form.setBackground(AppTheme.BG_PANEL);
        form.setBorder(new EmptyBorder(AppTheme.PADDING_LARGE, AppTheme.PADDING_LARGE, AppTheme.PADDING_LARGE, AppTheme.PADDING_LARGE));
        form.setOpaque(true);
        return form;
    }
    
    // -------------------- Styled ScrollPane Helper --------------------
    private JScrollPane createStyledScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(Color.WHITE);
        sp.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, AppTheme.BORDER_GRAY));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // -------------------- main --------------------
    public static void main(String[] args) {
        AppTheme.initializeTheme();
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
