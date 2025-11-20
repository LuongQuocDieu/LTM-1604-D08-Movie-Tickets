package web;

import mysql.CSDL;
import utils.PasswordUtil;
import utils.AppTheme;
import utils.MessageDialog;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class WebUI extends JFrame {
    private JTextField txtUser, txtEmail, txtPhone, txtLoginUser;
    private JPasswordField txtPass, txtConfirmPass, txtLoginPass;
    private JButton btnRegister, btnLogin;
    private CardLayout cardLayout;
    private JPanel cardPanel;

    public WebUI() {
        setTitle("CinemaX - Đăng ký & Đăng nhập");
        setSize(500, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // --- Login Panel ---
        JPanel loginPanel = createLoginPanel();
        cardPanel.add(loginPanel, "LOGIN");

        // --- Register Panel ---
        JPanel registerPanel = createRegisterPanel();
        cardPanel.add(registerPanel, "REGISTER");

        add(cardPanel);
        cardLayout.show(cardPanel, "LOGIN");
        
        // Event listeners
        btnRegister.addActionListener(e -> registerUser());
        btnLogin.addActionListener(e -> loginUser());
    }

    private JPanel createLoginPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Title
        JLabel lblTitle = new JLabel("Đăng Nhập");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(AppTheme.PRIMARY_BLUE);
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(lblTitle);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        // User field
        gbc.gridy = 0;
        JLabel lblUser = new JLabel("Tên đăng nhập / Email / SĐT:");
        lblUser.setFont(AppTheme.FONT_LABEL_BOLD);
        formPanel.add(lblUser, gbc);

        gbc.gridy = 1;
        txtLoginUser = new JTextField(20);
        txtLoginUser.setFont(AppTheme.FONT_INPUT);
        formPanel.add(txtLoginUser, gbc);

        // Password field
        gbc.gridy = 2;
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(AppTheme.FONT_LABEL_BOLD);
        formPanel.add(lblPass, gbc);

        gbc.gridy = 3;
        txtLoginPass = new JPasswordField(20);
        txtLoginPass.setFont(AppTheme.FONT_INPUT);
        formPanel.add(txtLoginPass, gbc);

        // Login button
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        btnLogin = new JButton("Đăng Nhập");
        btnLogin.setFont(AppTheme.FONT_BUTTON);
        btnLogin.setPreferredSize(new Dimension(200, 35));
        btnLogin.setBackground(AppTheme.PRIMARY_BLUE);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        formPanel.add(btnLogin, gbc);

        // Separator
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator sep = new JSeparator();
        formPanel.add(sep, gbc);

        // Link to register
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        linkPanel.setBackground(Color.WHITE);
        JLabel lblQuestion = new JLabel("Nếu bạn chưa có tài khoản vui lòng ");
        lblQuestion.setFont(AppTheme.FONT_LABEL_REGULAR);
        lblQuestion.setForeground(new Color(100, 100, 100));
        
        JLabel lblRegisterLink = new JLabel("đăng ký");
        lblRegisterLink.setFont(new Font("Segoe UI", Font.BOLD + Font.ITALIC, 12));
        lblRegisterLink.setForeground(AppTheme.PRIMARY_BLUE);
        lblRegisterLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblRegisterLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                cardLayout.show(cardPanel, "REGISTER");
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                lblRegisterLink.setForeground(new Color(0, 100, 200));
                lblRegisterLink.setText("<html><u>đăng ký</u></html>");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                lblRegisterLink.setForeground(AppTheme.PRIMARY_BLUE);
                lblRegisterLink.setText("đăng ký");
            }
        });
        
        linkPanel.add(lblQuestion);
        linkPanel.add(lblRegisterLink);
        formPanel.add(linkPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createRegisterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // Title
        JLabel lblTitle = new JLabel("Đăng Ký");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(AppTheme.PRIMARY_BLUE);
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(lblTitle);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        // Name field
        gbc.gridy = 0;
        JLabel lblName = new JLabel("Họ và tên:");
        lblName.setFont(AppTheme.FONT_LABEL_BOLD);
        formPanel.add(lblName, gbc);

        gbc.gridy = 1;
        txtUser = new JTextField(20);
        txtUser.setFont(AppTheme.FONT_INPUT);
        formPanel.add(txtUser, gbc);

        // Email field
        gbc.gridy = 2;
        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(AppTheme.FONT_LABEL_BOLD);
        formPanel.add(lblEmail, gbc);

        gbc.gridy = 3;
        txtEmail = new JTextField(20);
        txtEmail.setFont(AppTheme.FONT_INPUT);
        formPanel.add(txtEmail, gbc);

        // Phone field
        gbc.gridy = 4;
        JLabel lblPhone = new JLabel("Số điện thoại:");
        lblPhone.setFont(AppTheme.FONT_LABEL_BOLD);
        formPanel.add(lblPhone, gbc);

        gbc.gridy = 5;
        txtPhone = new JTextField(20);
        txtPhone.setFont(AppTheme.FONT_INPUT);
        formPanel.add(txtPhone, gbc);

        // Password field
        gbc.gridy = 6;
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(AppTheme.FONT_LABEL_BOLD);
        formPanel.add(lblPass, gbc);

        gbc.gridy = 7;
        txtPass = new JPasswordField(20);
        txtPass.setFont(AppTheme.FONT_INPUT);
        formPanel.add(txtPass, gbc);

        // Confirm password field
        gbc.gridy = 8;
        JLabel lblConfirm = new JLabel("Nhập lại mật khẩu:");
        lblConfirm.setFont(AppTheme.FONT_LABEL_BOLD);
        formPanel.add(lblConfirm, gbc);

        gbc.gridy = 9;
        txtConfirmPass = new JPasswordField(20);
        txtConfirmPass.setFont(AppTheme.FONT_INPUT);
        formPanel.add(txtConfirmPass, gbc);

        // Register button
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(12, 0, 6, 0);
        btnRegister = new JButton("Đăng Ký");
        btnRegister.setFont(AppTheme.FONT_BUTTON);
        btnRegister.setPreferredSize(new Dimension(200, 35));
        btnRegister.setBackground(AppTheme.SUCCESS_GREEN);
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFocusPainted(false);
        btnRegister.setBorderPainted(false);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        formPanel.add(btnRegister, gbc);

        // Separator
        gbc.gridy = 11;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        JSeparator sep = new JSeparator();
        formPanel.add(sep, gbc);

        // Link to login
        gbc.gridy = 12;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        linkPanel.setBackground(Color.WHITE);
        JLabel lblQuestion = new JLabel("Bạn đã có tài khoản? ");
        lblQuestion.setFont(AppTheme.FONT_LABEL_REGULAR);
        lblQuestion.setForeground(new Color(100, 100, 100));
        
        JLabel lblLoginLink = new JLabel("Đăng nhập");
        lblLoginLink.setFont(new Font("Segoe UI", Font.BOLD + Font.ITALIC, 12));
        lblLoginLink.setForeground(AppTheme.PRIMARY_BLUE);
        lblLoginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblLoginLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                cardLayout.show(cardPanel, "LOGIN");
                clearLoginFields();
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                lblLoginLink.setForeground(new Color(0, 100, 200));
                lblLoginLink.setText("<html><u>Đăng nhập</u></html>");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                lblLoginLink.setForeground(AppTheme.PRIMARY_BLUE);
                lblLoginLink.setText("Đăng nhập");
            }
        });
        
        linkPanel.add(lblQuestion);
        linkPanel.add(lblLoginLink);
        formPanel.add(linkPanel, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private void clearLoginFields() {
        txtLoginUser.setText("");
        txtLoginPass.setText("");
    }

    // --- Xử lý đăng ký ---
    private void registerUser() {
        String hoTen = txtUser.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String password = new String(txtPass.getPassword()).trim();
        String confirm = new String(txtConfirmPass.getPassword()).trim();

        // Validation
        if (hoTen.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            MessageDialog.showWarning(this, "Vui lòng điền đầy đủ thông tin!");
            return;
        }
        if (!password.equals(confirm)) {
            MessageDialog.showError(this, "Mật khẩu nhập lại không khớp!");
            return;
        }
        if (!PasswordUtil.isValidEmail(email)) {
            MessageDialog.showError(this, "Email không hợp lệ!");
            return;
        }
        if (!PasswordUtil.isValidPhone(phone)) {
            MessageDialog.showError(this, "Số điện thoại phải từ 9-11 số!");
            return;
        }
        if (password.length() < 6) {
            MessageDialog.showError(this, "Mật khẩu phải ít nhất 6 ký tự!");
            return;
        }

        // Hash password
        String hashedPassword = PasswordUtil.hashPassword(password);
        if (hashedPassword == null) {
            MessageDialog.showError(this, "Lỗi hash password!");
            return;
        }

        // Lưu vào database
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO nguoi_dung (ho_ten, gmail, so_dien_thoai, mat_khau, vai_tro) VALUES (?, ?, ?, ?, 'user')")) {
            ps.setString(1, hoTen);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, hashedPassword);
            ps.executeUpdate();
            
            MessageDialog.showSuccess(this, "Đăng ký thành công! Vui lòng đăng nhập.");
            txtUser.setText("");
            txtEmail.setText("");
            txtPhone.setText("");
            txtPass.setText("");
            txtConfirmPass.setText("");
            
            // Switch to login
            cardLayout.show(cardPanel, "LOGIN");
        } catch (SQLException ex) {
            MessageDialog.showError(this, "Email đã tồn tại hoặc lỗi database: " + ex.getMessage());
        }
    }

    // --- Xử lý đăng nhập ---
    private void loginUser() {
        String loginUser = txtLoginUser.getText().trim();
        String pass = new String(txtLoginPass.getPassword());

        if (loginUser.isEmpty() || pass.isEmpty()) {
            MessageDialog.showWarning(this, "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id, ho_ten, mat_khau, vai_tro FROM nguoi_dung WHERE gmail = ? OR ho_ten = ? OR so_dien_thoai = ?")) {
            ps.setString(1, loginUser);
            ps.setString(2, loginUser);
            ps.setString(3, loginUser);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("mat_khau");
                String role = rs.getString("vai_tro");
                String userName = rs.getString("ho_ten");
                
                // Verify password
                if (PasswordUtil.verifyPassword(pass, hashedPassword)) {
                    MessageDialog.showSuccess(this, "Đăng nhập thành công!");
                    this.dispose();
                    
                    if ("user".equals(role)) {
                        new MainUI(userName).setVisible(true);
                    } else if ("admin".equals(role)) {
                        new quanly.QuanlyUI(rs.getInt("id"), userName).setVisible(true);
                    }
                } else {
                    MessageDialog.showError(this, "Mật khẩu sai!");
                }
            } else {
                MessageDialog.showError(this, "Tài khoản không tồn tại!");
            }
        } catch (SQLException ex) {
            MessageDialog.showError(this, "Lỗi: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WebUI().setVisible(true));
    }
}
