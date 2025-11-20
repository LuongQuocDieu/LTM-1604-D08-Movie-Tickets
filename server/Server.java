package server;

import mysql.CSDL;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 2040;

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

                // luôn gửi END để client biết kết thúc dữ liệu
                if (!response.endsWith("\nEND")) {
                    response = response + "\nEND";
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
        String cmd = parts[0].toUpperCase().trim();

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

    // REGISTER;Ho Ten;email;phone;password
    private static String handleRegister(String[] parts) throws SQLException {
        if (parts.length < 5) return "ERR;REGISTER REQUIRES 4 ARGS";
        String hoTen = parts[1].trim();
        String email = parts[2].trim();
        String phone = parts[3].trim();
        String pass = parts[4].trim(); // recommended: hash before storing

        try (Connection conn = CSDL.getConnection()) {
            // check email unique
            try (PreparedStatement chk = conn.prepareStatement("SELECT id FROM nguoi_dung WHERE gmail=?")) {
                chk.setString(1, email);
                ResultSet rs = chk.executeQuery();
                if (rs.next()) return "ERR;Email đã tồn tại";
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO nguoi_dung (ho_ten, gmail, so_dien_thoai, mat_khau) VALUES (?,?,?,?)")) {
                ps.setString(1, hoTen);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setString(4, pass);
                ps.executeUpdate();
            }
        }
        return "OK;Đăng ký thành công!";
    }

    // LOGIN;username;password  (username có thể là ho_ten hoặc email hoặc sdt)
    private static String handleLogin(String[] parts) throws SQLException {
        if (parts.length < 3) return "ERR;LOGIN REQUIRES 2 ARGS";
        String user = parts[1].trim();
        String pass = parts[2].trim();

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, ho_ten, gmail FROM nguoi_dung WHERE (ho_ten=? OR gmail=? OR so_dien_thoai=?) AND mat_khau=?")) {
            ps.setString(1, user);
            ps.setString(2, user);
            ps.setString(3, user);
            ps.setString(4, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("ho_ten");
                String mail = rs.getString("gmail");
                return "OK;" + id + ";" + name + ";" + mail + ";Đăng nhập thành công";
            }
        }
        return "ERR;Sai tài khoản hoặc mật khẩu";
    }

    // ADD_MOVIE;Ten;TheLoai;ThoiLuong;Anh;MoTa
    private static String handleAddMovie(String[] parts) throws SQLException {
        if (parts.length < 6) return "ERR;ADD_MOVIE REQUIRES 5 ARGS";
        String ten = parts[1].trim();
        String theLoai = parts[2].trim();
        int thoiLuong = Integer.parseInt(parts[3].trim());
        String anh = parts[4].trim();
        String moTa = parts[5].trim();

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO phim (ten_phim, the_loai, thoi_luong, anh, mo_ta) VALUES (?,?,?,?,?)")) {
            ps.setString(1, ten);
            ps.setString(2, theLoai);
            ps.setInt(3, thoiLuong);
            ps.setString(4, anh);
            ps.setString(5, moTa);
            ps.executeUpdate();
        }
        return "OK;Thêm phim thành công";
    }

    // ADD_SHOW;phimId;phongId;ngay;gio;giaVe
    // xuat_chieu.id is VARCHAR(10) generated here as random if you want, but we use AUTO generated random string
    private static String handleAddShow(String[] parts) throws SQLException {
        if (parts.length < 6) return "ERR;ADD_SHOW REQUIRES 5 ARGS";
        int phimId = Integer.parseInt(parts[1].trim());
        int phongId = Integer.parseInt(parts[2].trim());
        String ngay = parts[3].trim();  // YYYY-MM-DD
        String gio = parts[4].trim();   // HH:MM:SS
        double giaVe = Double.parseDouble(parts[5].trim());

        // generate random 10-digit id (string)
        String id = generateRandomNumericId(10);

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO xuat_chieu (id, phim_id, phong_id, ngay, gio, gia_ve) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, id);
            ps.setInt(2, phimId);
            ps.setInt(3, phongId);
            ps.setDate(4, Date.valueOf(ngay));
            ps.setTime(5, Time.valueOf(gio));
            ps.setDouble(6, giaVe);
            ps.executeUpdate();
        }
        return "OK;Thêm suất chiếu thành công;" + id;
    }

    private static String generateRandomNumericId(int len) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < len) {
            sb.append((int)(Math.random() * 10));
        }
        return sb.toString();
    }

    // GET_MOVIES
    private static String handleGetMovies() throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, ten_phim, the_loai, thoi_luong, anh FROM phim")) {
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(";")
                  .append(rs.getString("ten_phim")).append(";")
                  .append(rs.getString("the_loai")).append(";")
                  .append(rs.getInt("thoi_luong")).append(";")
                  .append(rs.getString("anh")).append("\n");
            }
        }
        return sb.length() == 0 ? "ERR;No movies" : sb.toString();
    }

    // GET_SHOWS;phimId  -> danh sách suất chiếu theo phim
    private static String handleGetShows(String[] parts) throws SQLException {
        if (parts.length < 2) return "ERR;GET_SHOWS REQUIRES PHIM_ID";
        int phimId = Integer.parseInt(parts[1].trim());
        StringBuilder sb = new StringBuilder();

        String sql = "SELECT x.id, p.ten_phim, x.ngay, x.gio, pc.ten_phong, pc.suc_chua, x.gia_ve " +
                "FROM xuat_chieu x " +
                "JOIN phim p ON x.phim_id = p.id " +
                "JOIN phong_chieu pc ON x.phong_id = pc.id " +
                "WHERE p.id = ?";

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, phimId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString("id")).append(";")
                  .append(rs.getString("ten_phim")).append(";")
                  .append(rs.getDate("ngay")).append(";")
                  .append(rs.getTime("gio")).append(";")
                  .append(rs.getString("ten_phong")).append(";")
                  .append(rs.getInt("suc_chua")).append(";")
                  .append(rs.getDouble("gia_ve")).append("\n");
            }
        }
        return sb.length() == 0 ? "ERR;No shows for movie" : sb.toString();
    }

    // GET_SHOWS_ALL
    private static String handleGetShowsAll() throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT x.id, p.ten_phim, x.ngay, x.gio, pc.ten_phong, pc.suc_chua, x.gia_ve " +
                "FROM xuat_chieu x " +
                "JOIN phim p ON x.phim_id = p.id " +
                "JOIN phong_chieu pc ON x.phong_id = pc.id ";

        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getString("id")).append(";")
                  .append(rs.getString("ten_phim")).append(";")
                  .append(rs.getDate("ngay")).append(";")
                  .append(rs.getTime("gio")).append(";")
                  .append(rs.getString("ten_phong")).append(";")
                  .append(rs.getInt("suc_chua")).append(";")
                  .append(rs.getDouble("gia_ve")).append("\n");
            }
        }
        return sb.length() == 0 ? "ERR;No shows" : sb.toString();
    }

    // GET_AVAILABLE_SEATS;showId
    private static String handleGetAvailableSeats(String[] parts) throws SQLException {
        if (parts.length < 2) return "ERR;GET_AVAILABLE_SEATS REQUIRES show_id";
        String showId = parts[1].trim();
        StringBuilder sb = new StringBuilder();

        try (Connection conn = CSDL.getConnection()) {
            // find phong_id for show
            int phongId;
            try (PreparedStatement ps = conn.prepareStatement("SELECT phong_id FROM xuat_chieu WHERE id = ?")) {
                ps.setString(1, showId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return "ERR;Suất chiếu không tồn tại";
                phongId = rs.getInt("phong_id");
            }

            // list seats in that room and check booked
            String sql = "SELECT g.id, g.ma_ghe, g.hang_ghe FROM ghe g WHERE g.phong_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, phongId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int gheId = rs.getInt("id");
                    String maGhe = rs.getString("ma_ghe");
                    String hang = rs.getString("hang_ghe");
                    if (!isSeatBooked(conn, showId, gheId)) {
                        sb.append(gheId).append(";").append(maGhe).append(";").append(hang).append("\n");
                    }
                }
            }
        }
        return sb.length() == 0 ? "ERR;No available seats" : sb.toString();
    }

    private static boolean isSeatBooked(Connection conn, String showId, int gheId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM ve WHERE xuat_chieu_id = ? AND ghe_id = ? AND trang_thai <> 'da_huy'")) {
            ps.setString(1, showId);
            ps.setInt(2, gheId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // CHECK_ROOM_STATUS;showId  -> trả số ghế trống
    private static String handleCheckRoomStatus(String[] parts) throws SQLException {
        if (parts.length < 2) return "ERR;Thiếu tham số show_id";
        String showId = parts[1].trim();

        String sql = "SELECT COUNT(*) AS so_ghe_trong " +
                "FROM ghe g JOIN xuat_chieu x ON g.phong_id = x.phong_id " +
                "WHERE x.id = ? AND g.id NOT IN (SELECT ghe_id FROM ve WHERE xuat_chieu_id = ? AND trang_thai <> 'da_huy')";

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, showId);
            ps.setString(2, showId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int soGheTrong = rs.getInt("so_ghe_trong");
                return "OK;" + soGheTrong;
            }
        }
        return "ERR;Không tìm thấy suất chiếu";
    }

    // BOOK_TICKET;showId;username;A1,B2,C3
    private static String handleBookTicket(String[] parts) throws SQLException {
        if (parts.length < 4) return "ERR;BOOK_TICKET REQUIRES showId, username, seatCodes";
        String showId = parts[1].trim();
        String username = parts[2].trim();
        String[] seatCodes = parts[3].split(",");

        try (Connection conn = CSDL.getConnection()) {
            // get user id
            int userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM nguoi_dung WHERE ho_ten = ? OR gmail = ? OR so_dien_thoai = ?")) {
                ps.setString(1, username);
                ps.setString(2, username);
                ps.setString(3, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) userId = rs.getInt("id");
                else return "ERR;User không tồn tại";
            }

            conn.setAutoCommit(false);
            try {
                // for each seat code convert to ghe_id and insert into ve
                for (String code : seatCodes) {
                    String seatCode = code.trim();
                    int gheId = convertSeatCode(conn, showId, seatCode);

                    // check belongs to show
                    if (!isSeatBelongToShow(conn, showId, gheId)) {
                        conn.rollback();
                        return "ERR;Ghế " + seatCode + " không thuộc suất chiếu này";
                    }

                    // check already booked
                    if (isSeatBooked(conn, showId, gheId)) {
                        conn.rollback();
                        return "ERR;Ghế " + seatCode + " đã được đặt";
                    }

                    // get price for show
                    double price = getPriceForShow(conn, showId);

                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO ve (nguoi_dung_id, xuat_chieu_id, ghe_id, gia, trang_thai, ngay_tao) VALUES (?, ?, ?, ?, 'chua_thanh_toan', NOW())")) {
                        ins.setInt(1, userId);
                        ins.setString(2, showId);
                        ins.setInt(3, gheId);
                        ins.setDouble(4, price);
                        ins.executeUpdate();
                    }
                }
                conn.commit();
                return "OK;Đặt vé thành công cho " + seatCodes.length + " ghế";
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // convert seat code like "A1" to ghe.id for a specific show
    private static int convertSeatCode(Connection conn, String showId, String seatCode) throws SQLException {
        String sql = "SELECT g.id FROM ghe g JOIN xuat_chieu x ON g.phong_id = x.phong_id WHERE x.id = ? AND g.ma_ghe = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, showId);
            ps.setString(2, seatCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                throw new SQLException("Ghế " + seatCode + " không tồn tại trong suất này");
            }
        }
    }

    private static boolean isSeatBelongToShow(Connection conn, String showId, int gheId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM ghe g JOIN xuat_chieu x ON g.phong_id = x.phong_id WHERE x.id = ? AND g.id = ?")) {
            ps.setString(1, showId);
            ps.setInt(2, gheId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static double getPriceForShow(Connection conn, String showId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT gia_ve FROM xuat_chieu WHERE id = ?")) {
            ps.setString(1, showId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
            else throw new SQLException("Không tìm thấy giá vé cho suất chiếu");
        }
    }

    // GET_BOOKINGS;userId
    private static String handleGetBookings(String[] parts) throws SQLException {
        if (parts.length < 2) return "ERR;GET_BOOKINGS REQUIRES user_id";
        int userId = Integer.parseInt(parts[1].trim());
        StringBuilder sb = new StringBuilder();

        String sql = "SELECT v.id, p.ten_phim, x.ngay, x.gio, g.ma_ghe, v.gia, v.trang_thai " +
                "FROM ve v " +
                "JOIN xuat_chieu x ON v.xuat_chieu_id = x.id " +
                "JOIN phim p ON x.phim_id = p.id " +
                "JOIN ghe g ON v.ghe_id = g.id " +
                "WHERE v.nguoi_dung_id = ?";

        try (Connection conn = CSDL.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(";")
                  .append(rs.getString("ten_phim")).append(";")
                  .append(rs.getDate("ngay")).append(";")
                  .append(rs.getTime("gio")).append(";")
                  .append(rs.getString("ma_ghe")).append(";")
                  .append(rs.getDouble("gia")).append(";")
                  .append(rs.getString("trang_thai")).append("\n");
            }
        }
        return sb.length() == 0 ? "ERR;No bookings" : sb.toString();
    }

    // GET_USERS
    private static String handleGetUsers() throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, ho_ten, gmail, so_dien_thoai FROM nguoi_dung")) {
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(";")
                  .append(rs.getString("ho_ten")).append(";")
                  .append(rs.getString("gmail")).append(";")
                  .append(rs.getString("so_dien_thoai")).append("\n");
            }
        }
        return sb.length() == 0 ? "ERR;No users" : sb.toString();
    }

    // GET_ALL_BOOKINGS (admin)
    private static String handleGetAllBookings() throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT v.id, u.ho_ten, p.ten_phim, x.ngay, x.gio, g.ma_ghe, v.gia, v.trang_thai " +
                "FROM ve v " +
                "JOIN nguoi_dung u ON v.nguoi_dung_id = u.id " +
                "JOIN xuat_chieu x ON v.xuat_chieu_id = x.id " +
                "JOIN phim p ON x.phim_id = p.id " +
                "JOIN ghe g ON v.ghe_id = g.id";

        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getInt(1)).append(";")
                  .append(rs.getString(2)).append(";")
                  .append(rs.getString(3)).append(";")
                  .append(rs.getDate(4)).append(";")
                  .append(rs.getTime(5)).append(";")
                  .append(rs.getString(6)).append(";")
                  .append(rs.getDouble(7)).append(";")
                  .append(rs.getString(8)).append("\n");
            }
        }
        return sb.length() == 0 ? "ERR;No bookings" : sb.toString();
    }

    // GET_STATS -> tổng vé đã bán và còn lại
    private static String handleGetStats() throws SQLException {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM ve WHERE trang_thai <> 'da_huy') AS da_ban, " +
                "(SELECT COUNT(*) FROM ghe) - (SELECT COUNT(*) FROM ve WHERE trang_thai <> 'da_huy') AS con_lai";
        try (Connection conn = CSDL.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int daBan = rs.getInt("da_ban");
                int conLai = rs.getInt("con_lai");  
                return "OK;DA_BAN=" + daBan + ";CON_LAI=" + conLai;
            }
        }
        return "ERR;Không có dữ liệu thống kê";
    }
}
