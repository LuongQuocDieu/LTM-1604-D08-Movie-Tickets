package mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CSDL {
    private static final String URL = "jdbc:mysql://localhost:3306/btl-phan-mem-dat-ve-xem-phim1";
    private static final String USER = "root";
    private static final String PASSWORD = "Peakk4dieu@"; // đổi nếu cần

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void taoBang() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // =============================
            // BẢNG NGƯỜI DÙNG
            // =============================
            String sqlNguoiDung = "CREATE TABLE IF NOT EXISTS nguoi_dung (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "ho_ten VARCHAR(100) NOT NULL," +
                    "gmail VARCHAR(100) UNIQUE NOT NULL," +
                    "mat_khau VARCHAR(255) NOT NULL," +
                    "so_dien_thoai VARCHAR(15)," +
                    "vai_tro ENUM('user','admin') DEFAULT 'user'," +
                    "ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB";

            // =============================
            // BẢNG PHIM
            // =============================
            String sqlPhim = "CREATE TABLE IF NOT EXISTS phim (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "ten_phim VARCHAR(200) NOT NULL," +
                    "the_loai VARCHAR(100)," +
                    "thoi_luong INT NOT NULL," +
                    "mo_ta TEXT," +
                    "ngon_ngu VARCHAR(50)," +
                    "ngay_khoi_chieu DATE," +
                    "ngay_ket_thuc DATE," +
                    "anh VARCHAR(255)," +
                    "ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB";

            // =============================
            // BẢNG PHÒNG CHIẾU
            // =============================
            String sqlPhongChieu = "CREATE TABLE IF NOT EXISTS phong_chieu (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "ten_phong VARCHAR(50) NOT NULL," +
                    "suc_chua INT NOT NULL," +
                    "trang_thai ENUM('hoat_dong','bao_tri') DEFAULT 'hoat_dong'" +
                    ") ENGINE=InnoDB";

            // =============================
            // BẢNG GHẾ
            // =============================
            String sqlGhe = "CREATE TABLE IF NOT EXISTS ghe (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "phong_id INT NOT NULL," +
                    "ma_ghe VARCHAR(10) NOT NULL," +
                    "hang_ghe ENUM('thuong','vip','couple') DEFAULT 'thuong'," +
                    "FOREIGN KEY (phong_id) REFERENCES phong_chieu(id) " +
                    "ON DELETE CASCADE ON UPDATE CASCADE" +
                    ") ENGINE=InnoDB";

         // =============================
         // BẢNG XUẤT CHIẾU
         // =============================
         String sqlXuatChieu = "CREATE TABLE IF NOT EXISTS xuat_chieu (" +
                 "id INT AUTO_INCREMENT PRIMARY KEY," +   // đổi từ VARCHAR(10) sang INT AUTO_INCREMENT để dễ quản lý
                 "phim_id INT NOT NULL," +
                 "phong_id INT NOT NULL," +
                 "ngay DATE NOT NULL," +
                 "gio TIME NOT NULL," +
                 "tong_ve INT NOT NULL," +                // tổng vé
                 "ve_con INT NOT NULL," +                 // vé còn lại
                 "gia_ve DECIMAL(10,2) NOT NULL," +
                 "anh VARCHAR(255)," +                    // ảnh poster suất chiếu
                 "ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                 "FOREIGN KEY (phim_id) REFERENCES phim(id) " +
                 "ON DELETE CASCADE ON UPDATE CASCADE," +
                 "FOREIGN KEY (phong_id) REFERENCES phong_chieu(id) " +
                 "ON DELETE CASCADE ON UPDATE CASCADE" +
                 ") ENGINE=InnoDB";


      // =============================
      // BẢNG VÉ
      // =============================
      String sqlVe = "CREATE TABLE IF NOT EXISTS ve (" +
              "id INT AUTO_INCREMENT PRIMARY KEY," +
              "nguoi_dung_id INT NOT NULL," +
              "xuat_chieu_id INT NOT NULL," +         // sửa từ VARCHAR(10) -> INT
              "ghe_id INT NOT NULL," +
              "gia DECIMAL(10,2)," +
              "trang_thai ENUM('da_thanh_toan','chua_thanh_toan','da_huy') DEFAULT 'chua_thanh_toan'," +
              "ngay_tao TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
              "FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) " +
              "ON DELETE CASCADE ON UPDATE CASCADE," +
              "FOREIGN KEY (xuat_chieu_id) REFERENCES xuat_chieu(id) " +  // giờ đã cùng kiểu INT
              "ON DELETE CASCADE ON UPDATE CASCADE," +
              "FOREIGN KEY (ghe_id) REFERENCES ghe(id) " +
              "ON DELETE CASCADE ON UPDATE CASCADE," +
              "UNIQUE (xuat_chieu_id, ghe_id)" +
              ") ENGINE=InnoDB";

            // Thực thi tạo bảng
            stmt.execute(sqlNguoiDung);
            stmt.execute(sqlPhim);
            stmt.execute(sqlPhongChieu);
            stmt.execute(sqlGhe);
            stmt.execute(sqlXuatChieu);
            stmt.execute(sqlVe);

            // =============================
            // CHÈN DỮ LIỆU PHÒNG CHIẾU
            // =============================
            stmt.execute("DELETE FROM phong_chieu");
            stmt.execute("ALTER TABLE phong_chieu AUTO_INCREMENT = 1");

            stmt.execute("INSERT INTO phong_chieu (ten_phong, suc_chua) VALUES " +
                    "('Phòng 1',100),('Phòng 2',100),('Phòng 3',100),('Phòng 4',100),('Phòng 5',100)," +
                    "('Phòng 6',150),('Phòng 7',150),('Phòng 8',150)," +
                    "('Phòng 9',200),('Phòng 10',200)");

            // =============================
            // CHÈN GHẾ MẪU (đơn giản hóa)
            // =============================
            stmt.execute("DELETE FROM ghe");
            stmt.execute("ALTER TABLE ghe AUTO_INCREMENT = 1");

            // Ví dụ: sinh 100 ghế cho phòng 1 (A1..J10)
            for (int phong = 1; phong <= 10; phong++) {
                int soGhe = (phong <= 5) ? 100 : (phong <= 8 ? 150 : 200);
                for (int i = 1; i <= soGhe; i++) {
                    char row = (char) ('A' + (i - 1) / 10);
                    int col = ((i - 1) % 10) + 1;
                    String maGhe = row + String.valueOf(col);

                    String hang = "thuong";
                    if (i > soGhe * 0.7 && i <= soGhe * 0.9) hang = "vip";
                    else if (i > soGhe * 0.9) hang = "couple";

                    stmt.execute("INSERT INTO ghe (phong_id, ma_ghe, hang_ghe) VALUES (" +
                            phong + ",'" + maGhe + "','" + hang + "')");
                }
            }

            System.out.println("✅ Tạo bảng + dữ liệu phòng & ghế thành công!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Test tạo bảng
    public static void main(String[] args) {
        taoBang();
    }
}
