package server;

import mysql.CSDL;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 2039;

    public static void main(String[] args) {
        System.out.println("✅ Server starting on port " + PORT + " ...");
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client);
                pool.execute(() -> handleClient(client));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Request from " + socket + " : " + line);

                String response = processRequest(line);

                // ✅ luôn gửi END để client biết kết thúc dữ liệu
                if (!response.endsWith("\nEND")) {
                    response += "\nEND";
                }
                out.write(response + "\n");
                out.flush();
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket);
        }
    }

    private static String processRequest(String request) {
        String[] parts = request.split(";", -1);
        String cmd = parts[0].toUpperCase();

        try {
            switch (cmd) {
                case "REGISTER": return handleRegister(parts);
                case "LOGIN": return handleLogin(parts);
                case "ADD_MOVIE": return handleAddMovie(parts);
                case "ADD_SHOW": return handleAddShow(parts);
                case "GET_MOVIES": return handleGetMovies();
                case "GET_SHOWS": return handleGetShows(parts);
                case "GET_SHOWS_ALL": return handleGetShowsAll();
                case "GET_AVAILABLE_SEATS": return handleGetAvailableSeats(parts);
                case "CHECK_ROOM_STATUS": return handleCheckRoomStatus(parts);
                case "BOOK_TICKET": return handleBookTicket(parts);
                case "GET_BOOKINGS": return handleGetBookings(parts);
                case "GET_USERS": return handleGetUsers();
                case "GET_ALL_BOOKINGS": return handleGetAllBookings();
                case "GET_STATS": return handleGetStats();
                default: return "ERR;Unknown command";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "ERR;" + ex.getMessage();
        }
    }

    // ===================== HANDLERS =====================
    
    private static String handleGetAvailableSeats(String[] parts) throws SQLException {
        if (parts.length < 2) return "ERR;GET_AVAILABLE_SEATS REQUIRES suat_id";

        int suatId = Integer.parseInt(parts[1]);
        StringBuilder sb = new StringBuilder();

        try (Connection conn = CSDL.getConnection()) {
            int phongId = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT phong_id FROM suat_chieu WHERE id=?")) {
                ps.setInt(1, suatId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) phongId = rs.getInt(1);
                else return "ERR;Suất chiếu không tồn tại";
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, so_ghe FROM ghe WHERE phong_id=?")) {
                ps.setInt(1, phongId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int gheId = rs.getInt("id");
                    String soGhe = rs.getString("so_ghe");
                    if (!isGheDaDat(conn, suatId, gheId)) {
                        sb.append(gheId).append(";").append(soGhe).append("\n");
                    }
                }
            }
        }
        return sb.toString().isEmpty() ? "ERR;Không còn ghế trống" : sb.toString();
    }

    private static boolean isGheDaDat(Connection conn, int suatId, int gheId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM dat_ve WHERE suat_id=? AND ghe_id=? AND trang_thai <> 'Đã hủy'")) {
            ps.setInt(1, suatId);
            ps.setInt(2, gheId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static String handleRegister(String[] parts) throws SQLException {
        if (parts.length < 5) return "ERR;REGISTER REQUIRES 4 ARGS";
        String hoTen = parts[1], email = parts[2], phone = parts[3], pass = parts[4];
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO nguoi_dung (ho_ten,email,sdt,mat_khau) VALUES (?,?,?,?)")) {
            ps.setString(1, hoTen);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, pass);
            ps.executeUpdate();
        }
        return "OK;Đăng ký thành công!";
    }

    private static String handleLogin(String[] parts) throws SQLException {
        if (parts.length < 3) return "ERR;LOGIN REQUIRES 2 ARGS";
        String user = parts[1], pass = parts[2];
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM nguoi_dung WHERE (ho_ten=? OR email=? OR sdt=?) AND mat_khau=?")) {
            ps.setString(1, user);
            ps.setString(2, user);
            ps.setString(3, user);
            ps.setString(4, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return "OK;Đăng nhập thành công";
        }
        return "ERR;Sai tài khoản hoặc mật khẩu";
    }

    private static String handleAddMovie(String[] parts) throws SQLException {
        if (parts.length < 6) return "ERR;ADD_MOVIE REQUIRES 5 ARGS";
        String ten = parts[1], theLoai = parts[2], thoiLuong = parts[3], anh = parts[4], moTa = parts[5];
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO phim (ten_phim,the_loai,thoi_luong,anh,mo_ta) VALUES (?,?,?,?,?)")) {
            ps.setString(1, ten);
            ps.setString(2, theLoai);
            ps.setInt(3, Integer.parseInt(thoiLuong));
            ps.setString(4, anh);
            ps.setString(5, moTa);
            ps.executeUpdate();
        }
        return "OK;Thêm phim thành công";
    }

    private static String handleAddShow(String[] parts) throws SQLException {
        if (parts.length < 6) return "ERR;ADD_SHOW REQUIRES 5 ARGS";
        int phimId = Integer.parseInt(parts[1]);
        int phongId = Integer.parseInt(parts[2]);
        String ngayGio = parts[3];
        int tongVe = Integer.parseInt(parts[4]);
        double giaVe = Double.parseDouble(parts[5]);
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO suat_chieu (phim_id,phong_id,ngay_gio,tong_ve,ve_con,gia_ve) VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, phimId);
            ps.setInt(2, phongId);
            ps.setString(3, ngayGio);
            ps.setInt(4, tongVe);
            ps.setInt(5, tongVe);
            ps.setDouble(6, giaVe);
            ps.executeUpdate();
        }
        return "OK;Thêm suất chiếu thành công";
    }

    private static String handleGetMovies() throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,ten_phim,the_loai,thoi_luong,anh FROM phim")) {
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(";")
                  .append(rs.getString("ten_phim")).append(";")
                  .append(rs.getString("the_loai")).append(";")
                  .append(rs.getInt("thoi_luong")).append(";")
                  .append(rs.getString("anh")).append("\n");
            }
        }
        return sb.toString().isEmpty() ? "ERR;No movies" : sb.toString();
    }

    private static String handleGetShows(String[] parts) throws SQLException {
        if (parts.length < 2) return "ERR;GET_SHOWS REQUIRES phim_id";
        int phimId = Integer.parseInt(parts[1]);
        StringBuilder sb = new StringBuilder();
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT s.id, p.ten_phim, s.ngay_gio, pc.ten_phong, pc.suc_chua, s.gia_ve " +
                     "FROM suat_chieu s " +
                     "JOIN phim p ON s.phim_id = p.id " +
                     "JOIN phong_chieu pc ON s.phong_id = pc.id " +
                     "WHERE p.id = ?")) {
            ps.setInt(1, phimId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(";")
                  .append(rs.getString("ten_phim")).append(";")
                  .append(rs.getString("ngay_gio")).append(";")
                  .append(rs.getString("ten_phong")).append(";")
                  .append(rs.getInt("suc_chua")).append(";")
                  .append(rs.getDouble("gia_ve")).append("\n");
            }
        }
        return sb.toString().isEmpty() ? "ERR;No shows" : sb.toString();
    }

    private static String handleGetShowsAll() throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT s.id, p.ten_phim, s.ngay_gio, pc.ten_phong, pc.suc_chua, s.gia_ve " +
                     "FROM suat_chieu s " +
                     "JOIN phim p ON s.phim_id = p.id " +
                     "JOIN phong_chieu pc ON s.phong_id = pc.id")) {
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(";")
                  .append(rs.getString("ten_phim")).append(";")
                  .append(rs.getString("ngay_gio")).append(";")
                  .append(rs.getString("ten_phong")).append(";")
                  .append(rs.getInt("suc_chua")).append(";")
                  .append(rs.getDouble("gia_ve")).append("\n");
            }
        }
        return sb.toString().isEmpty() ? "ERR;No shows" : sb.toString();
    }

    private static String handleCheckRoomStatus(String[] parts) {
        if (parts.length < 2) return "ERR;Thiếu tham số suat_id";
        int suatId = Integer.parseInt(parts[1]);
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) AS so_ghe_trong " +
                 "FROM ghe g " +
                 "JOIN suat_chieu s ON g.phong_id = s.phong_id " +
                 "WHERE s.id=? " +
                 "AND g.id NOT IN (SELECT ghe_id FROM dat_ve WHERE suat_id=? AND trang_thai='Đã đặt')")) {
            ps.setInt(1, suatId);
            ps.setInt(2, suatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int soGheTrong = rs.getInt("so_ghe_trong");
                return "OK;" + soGheTrong;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERR;" + e.getMessage();
        }
        return "ERR;Không tìm thấy suất chiếu";
    }

 // Chuyển "A1" → số ghế, "B2" → số ghế...
    private static int convertSeatCode(Connection conn, int suatId, String seatCode) throws SQLException {
        // seatCode ví dụ: "A1", "B2"
        String sql = "SELECT g.id FROM ghe g JOIN suat_chieu s ON g.phong_id = s.phong_id " +
                     "WHERE s.id=? AND g.so_ghe=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, suatId);
            ps.setString(2, seatCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // trả về ghe_id trong DB
            } else {
                throw new SQLException("Ghế " + seatCode + " không tồn tại trong suất này");
            }
        }
    }

    private static String handleBookTicket(String[] parts) throws SQLException {
        if (parts.length < 4) return "ERR;BOOK_TICKET REQUIRES suatId, username, gheCodes";
        int suatId = Integer.parseInt(parts[1]);
        String username = parts[2];
        String[] gheCodes = parts[3].split(",");

        try (Connection conn = CSDL.getConnection()) {
            int userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM nguoi_dung WHERE ho_ten=? OR email=? OR sdt=?")) {
                ps.setString(1, username);
                ps.setString(2, username);
                ps.setString(3, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) userId = rs.getInt("id");
                else return "ERR;User không tồn tại";
            }

            conn.setAutoCommit(false);
            try {
                for (String code : gheCodes) {
                    int gheId = convertSeatCode(conn, suatId, code.trim()); // ✅ đổi A1,B2 thành ghe_id

                    if (isGheDaDat(conn, suatId, gheId)) {
                        conn.rollback();
                        return "ERR;Ghế " + code + " đã được đặt";
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO dat_ve (user_id, suat_id, ghe_id, ngay_dat, trang_thai) VALUES (?, ?, ?, NOW(), 'Đã đặt')")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, suatId);
                        ps.setInt(3, gheId);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return "OK;Đặt vé thành công cho " + gheCodes.length + " ghế";
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    private static boolean isGheThuocSuat(Connection conn, int suatId, int gheId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM ghe g JOIN suat_chieu s ON g.phong_id=s.phong_id WHERE s.id=? AND g.id=?")) {
            ps.setInt(1, suatId);
            ps.setInt(2, gheId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static String handleGetBookings(String[] parts) throws SQLException {
        if (parts.length < 2) return "ERR;GET_BOOKINGS REQUIRES user_id";
        int userId = Integer.parseInt(parts[1]);
        StringBuilder sb = new StringBuilder();
        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT d.id,p.ten_phim,s.ngay_gio,d.so_luong,d.tong_tien,d.trang_thai " +
                     "FROM dat_ve d JOIN suat_chieu s ON d.suat_id=s.id " +
                     "JOIN phim p ON s.phim_id=p.id WHERE d.nguoi_dung_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sb.append(rs.getInt(1)).append(";")
                  .append(rs.getString(2)).append(";")
                  .append(rs.getString(3)).append(";")
                  .append(rs.getInt(4)).append(";")
                  .append(rs.getDouble(5)).append(";")
                  .append(rs.getString(6)).append("\n");
            }
        }
        return sb.toString().isEmpty() ? "ERR;No bookings" : sb.toString();
    }

    private static String handleGetUsers() throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,ho_ten,email,sdt FROM nguoi_dung")) {
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(";")
                  .append(rs.getString("ho_ten")).append(";")
                  .append(rs.getString("email")).append(";")
                  .append(rs.getString("sdt")).append("\n");
            }
        }
        return sb.toString().isEmpty() ? "ERR;No users" : sb.toString();
    }

    private static String handleGetAllBookings() throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT d.id,u.ho_ten,p.ten_phim,s.ngay_gio,d.so_luong,d.tong_tien,d.trang_thai " +
                     "FROM dat_ve d " +
                     "JOIN nguoi_dung u ON d.nguoi_dung_id=u.id " +
                     "JOIN suat_chieu s ON d.suat_id=s.id " +
                     "JOIN phim p ON s.phim_id=p.id")) {
            while (rs.next()) {
                sb.append(rs.getInt(1)).append(";")
                  .append(rs.getString(2)).append(";")
                  .append(rs.getString(3)).append(";")
                  .append(rs.getString(4)).append(";")
                  .append(rs.getInt(5)).append(";")
                  .append(rs.getDouble(6)).append(";")
                  .append(rs.getString(7)).append("\n");
            }
        }
        return sb.toString().isEmpty() ? "ERR;No bookings" : sb.toString();
    }

    private static String handleGetStats() {
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(
                "SELECT COALESCE(SUM(tong_ve - ve_con),0) AS da_ban, " +
                "COALESCE(SUM(ve_con),0) AS con_lai " +
                "FROM suat_chieu");
            if (rs.next()) {
                int daBan = rs.getInt("da_ban");
                int conLai = rs.getInt("con_lai");
                return "OK;DA_BAN=" + daBan + ";CON_LAI=" + conLai;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "ERR;" + e.getMessage();
        }
        return "ERR;Không có dữ liệu";
    }
}
