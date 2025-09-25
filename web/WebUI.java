package web;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class WebUI extends JFrame {
    private JTextField txtUser, txtEmail, txtPhone, txtLoginUser;
    private JPasswordField txtPass, txtConfirmPass, txtLoginPass;
    private JButton btnRegister, btnLogin;

    public WebUI() {
        setTitle("WebUI - Đăng ký & Đăng nhập");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Đăng ký ---
        JPanel registerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        registerPanel.add(new JLabel("Tên:"), gbc);
        gbc.gridx = 1;
        txtUser = new JTextField(15);
        registerPanel.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy++;
        registerPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        txtEmail = new JTextField(15);
        registerPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy++;
        registerPanel.add(new JLabel("Số điện thoại:"), gbc);
        gbc.gridx = 1;
        txtPhone = new JTextField(15);
        registerPanel.add(txtPhone, gbc);

        gbc.gridx = 0; gbc.gridy++;
        registerPanel.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1;
        txtPass = new JPasswordField(15);
        registerPanel.add(txtPass, gbc);

        gbc.gridx = 0; gbc.gridy++;
        registerPanel.add(new JLabel("Nhập lại mật khẩu:"), gbc);
        gbc.gridx = 1;
        txtConfirmPass = new JPasswordField(15);
        registerPanel.add(txtConfirmPass, gbc);

        // nút đăng ký nhỏ và căn giữa
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnRegister = new JButton("Đăng ký");
        btnRegister.setPreferredSize(new Dimension(100, 30));
        registerPanel.add(btnRegister, gbc);

        tabbedPane.add("Đăng ký", registerPanel);

        // --- Đăng nhập ---
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(8, 8, 8, 8);
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        gbc2.gridx = 0; gbc2.gridy = 0;
        loginPanel.add(new JLabel("Tên/Email/SĐT:"), gbc2);
        gbc2.gridx = 1;
        txtLoginUser = new JTextField(12); // nhỏ gọn hơn
        loginPanel.add(txtLoginUser, gbc2);

        gbc2.gridx = 0; gbc2.gridy++;
        loginPanel.add(new JLabel("Mật khẩu:"), gbc2);
        gbc2.gridx = 1;
        txtLoginPass = new JPasswordField(12);
        loginPanel.add(txtLoginPass, gbc2);

        gbc2.gridx = 0; gbc2.gridy++;
        gbc2.gridwidth = 2;
        gbc2.anchor = GridBagConstraints.CENTER;
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setPreferredSize(new Dimension(100, 30));
        loginPanel.add(btnLogin, gbc2);

        tabbedPane.add("Đăng nhập", loginPanel);

        add(tabbedPane);

        // Xử lý sự kiện
        btnRegister.addActionListener(e -> registerUser());
        btnLogin.addActionListener(e -> loginUser());
    }

    private void registerUser() {
        String hoTen = txtUser.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String password = new String(txtPass.getPassword()).trim();
        String confirm = new String(txtConfirmPass.getPassword()).trim();

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu nhập lại không khớp!");
            return;
        }
        if (hoTen.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin!");
            return;
        }

        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("REGISTER;" + hoTen + ";" + email + ";" + phone + ";" + password);

            String response = in.readLine();
            if (response != null) {
                JOptionPane.showMessageDialog(this, response);
            } else {
                JOptionPane.showMessageDialog(this, "Server không phản hồi!");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi kết nối server: " + ex.getMessage());
        }
    }

    private void loginUser() {
        String loginUser = txtLoginUser.getText();
        String pass = new String(txtLoginPass.getPassword());

        try (Socket socket = new Socket("localhost", 2039);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LOGIN;" + loginUser + ";" + pass);
            String response = in.readLine();
            if (response != null && response.startsWith("OK")) {
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công!");
                this.dispose();
                new MainUI(txtLoginUser.getText()).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WebUI().setVisible(true));
    }
}
