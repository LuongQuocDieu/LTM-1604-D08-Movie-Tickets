package mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CSDL {
    private static final String URL = "jdbc:mysql://localhost:3306/btl_phan_mem_dat_ve_xem_phim1";
    private static final String USER = "root";
    private static final String PASSWORD = "Peakk4dieu@"; // đổi nếu cần

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void taoBang() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // ==================== Bảng Quản lý ====================
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS quan_ly (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    ho_ten VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    mat_khau VARCHAR(255) NOT NULL,
                    ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ==================== Bảng Người dùng ====================
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nguoi_dung (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    ho_ten VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE,
                    sdt VARCHAR(20) UNIQUE,
                    mat_khau VARCHAR(255) NOT NULL,
                    ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ==================== Bảng Phim ====================
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS phim (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    ten_phim VARCHAR(200) NOT NULL,
                    the_loai VARCHAR(100) NOT NULL,
                    thoi_luong INT,
                    anh VARCHAR(255),
                    mo_ta TEXT,
                    thoi_gian_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

         // ==================== Bảng Phòng chiếu ====================
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS phong_chieu (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    ten_phong VARCHAR(100) NOT NULL,
                    suc_chua INT NOT NULL,
                    trang_thai ENUM('active','maintenance') DEFAULT 'active'
                )
            """);


            // ==================== Bảng Ghế (chỉ định nghĩa ghế, không chứa trạng thái) ====================
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ghe (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    phong_id INT NOT NULL,
                    so_ghe VARCHAR(10) NOT NULL,
                    FOREIGN KEY (phong_id) REFERENCES phong_chieu(id) ON DELETE CASCADE
                )
            """);

            // ==================== Bảng Suất chiếu ====================
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS suat_chieu (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    phim_id INT NOT NULL,
                    phong_id INT NOT NULL,
                    ngay_gio DATETIME NOT NULL,
                    gia_ve DECIMAL(10,2) NOT NULL,
                    tong_ve INT NOT NULL DEFAULT 0,
                    ve_con INT NOT NULL DEFAULT 0,
                    thoi_gian_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (phim_id) REFERENCES phim(id) ON DELETE CASCADE,
                    FOREIGN KEY (phong_id) REFERENCES phong_chieu(id) ON DELETE CASCADE
                )
            """);

            // ==================== Bảng Đặt vé ====================
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dat_ve (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nguoi_dung_id INT NOT NULL,
                    suat_id INT NOT NULL,
                    ghe_id INT NOT NULL,
                    tong_tien DECIMAL(10,2) NOT NULL,
                    trang_thai ENUM('Chưa thanh toán','Đã thanh toán','Đã hủy') DEFAULT 'Chưa thanh toán',
                    thoi_gian_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE,
                    FOREIGN KEY (suat_id) REFERENCES suat_chieu(id) ON DELETE CASCADE,
                    FOREIGN KEY (ghe_id) REFERENCES ghe(id) ON DELETE CASCADE
                )
            """);

            // ==================== Bảng Thanh toán ====================
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS thanh_toan (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    dat_ve_id INT NOT NULL,
                    hinh_thuc VARCHAR(100),
                    so_tien DECIMAL(10,2) NOT NULL,
                    ngay_thanh_toan TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    ghi_chu TEXT,
                    FOREIGN KEY (dat_ve_id) REFERENCES dat_ve(id) ON DELETE CASCADE
                )
            """);

         // ==================== Dữ liệu mẫu phòng chiếu ====================
            stmt.executeUpdate("""
                INSERT IGNORE INTO phong_chieu (id, ten_phong, suc_chua, trang_thai) VALUES
                (1,'Phòng 1',100,'active'),
                (2,'Phòng 2',100,'active'),
                (3,'Phòng 3',100,'active'),
                (4,'Phòng 4',100,'active'),
                (5,'Phòng 5',100,'active'),
                (6,'Phòng 6',150,'active'),
                (7,'Phòng 7',150,'active'),
                (8,'Phòng 8',150,'active'),
                (9,'Phòng 9',200,'active'),
                (10,'Phòng 10',200,'active')
            """);


            System.out.println("✅ Tạo tất cả bảng & dữ liệu mẫu thành công!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        taoBang();
    }
}
