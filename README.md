<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   ỨNG DỤNG ĐẶT VÉ XEM PHIM
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>
---

## 1. Giới thiệu hệ thống

<tb>Hệ thống Quản lý Đặt vé Xem phim được xây dựng nhằm hỗ trợ người dùng dễ dàng tra cứu thông tin phim, lịch chiếu, đặt vé trực tuyến và quản lý vé đã đặt một cách thuận tiện.
Đối với quản trị viên, hệ thống cung cấp các chức năng quản lý dữ liệu phim, lịch chiếu, phòng chiếu, vé và theo dõi doanh thu, giúp việc vận hành rạp phim hiệu quả và chính xác hơn.

**Các chức năng chính**

Người dùng:
- Đăng ký / đăng nhập
 - Xem danh sách phim và lịch chiếu
 - Đặt vé xem phim và tra cứu vé đã đặt
- Quản trị viên:
 - Quản lý thông tin phim
 - Quản lý lịch chiếu, phòng chiếu
 - Quản lý vé và doanh thu

### 2. Ngôn ngữ & Công nghệ chính
<div align="center">

[![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![Socket](https://img.shields.io/badge/Socket-6DB33F?style=for-the-badge)](https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/net/Socket.html)
[![JDBC](https://img.shields.io/badge/JDBC-007396?style=for-the-badge)](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/)

### Cơ sở dữ liệu
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)

### Môi trường chạy
[![JDK](https://img.shields.io/badge/JDK-007396?style=for-the-badge)](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
[![NetBeans](https://img.shields.io/badge/NetBeans-FCFDFD?style=for-the-badge&logo=netbeans&logoColor=black)](https://netbeans.apache.org/)

</div>

---
## 3. Hình ảnh các chức năng

--- Chức năng đăng nhập , đăng ký ---
<img width="1920" height="919" alt="image" src="https://github.com/LuongQuocDieu/LTM-1604-D08-Movie-Tickets/blob/main/docs/dangky.png" />
<img width="1920" height="919" alt="image" src="https://github.com/LuongQuocDieu/LTM-1604-D08-Movie-Tickets/blob/main/docs/dangnhap.png" />
--- Giao diện trang chủ ---
<img width="1910" height="921" alt="image" src="https://github.com/LuongQuocDieu/LTM-1604-D08-Movie-Tickets/blob/main/docs/giaodienchinh.png" />
--- Giao diện trang quản lý ---
<img width="1920" height="923" alt="image" src="https://github.com/user-attachments/assets/e7554fab-9f6b-4150-8586-359cea51f592" />
--- Quản lý phim ---
<img width="1920" height="921" alt="image" src="https://github.com/LuongQuocDieu/LTM-1604-D08-Movie-Tickets/blob/main/docs/quanlyphim.png" />
--- Quản lý xuất chiếu ---
<img width="1920" height="435" alt="image" src="https://github.com/LuongQuocDieu/LTM-1604-D08-Movie-Tickets/blob/main/docs/quanlyxuatchieu.png" />
--- Quản lý phòng chiếu ---
<img width="1397" height="693" alt="image" src="https://github.com/LuongQuocDieu/LTM-1604-D08-Movie-Tickets/blob/main/docs/quanlyphongchieu.png" />
--- Cơ sở dữ liệu ---
<img width="1397" height="693" alt="image" src="https://github.com/LuongQuocDieu/LTM-1604-D08-Movie-Tickets/blob/main/docs/SQL.png" />

## 4. Các bước cài đặt

**Cài đặt JDK & NetBeans**  
   - Tải và cài đặt JDK: [https://www.oracle.com/java/technologies/javase-jdk11-downloads.html](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)  
   - Tải và cài đặt NetBeans IDE: [https://netbeans.apache.org/](https://netbeans.apache.org/)

**Cài đặt MySQL**  
   - Cài đặt MySQL : [https://www.mysql.com/](https://www.mysql.com/) / [https://www.apachefriends.org/](https://www.apachefriends.org/)  
   - Khởi động MySQL.

**Tạo cơ sở dữ liệu**  
   - Mở phpMyAdmin: [http://localhost/phpmyadmin](http://localhost/phpmyadmin)  
   - Tạo database mới, ví dụ: `movie_booking`  
   - Import file `db_movie.sql` trong thư mục dự án vào database vừa tạo.

**Cấu hình kết nối database**  
   - Mở file `Server.java` hoặc file cấu hình JDBC  
   - Chỉnh sửa thông tin nếu cần:
   ```java
   String url = "jdbc:mysql://localhost:3306/movie_booking";
   String user = "root";
   String password = "";
---

## 📬 **5. Liên hệ**
- **Sinh viên thực hiện:** **Lương Quốc Diệu**
- **Khoa công nghệ thông tin – Đại học Đại Nam**  
- 🌐 Website: [https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)  
- 📧 Email: [peakk4@gmail.com]
- 📱 Fanpage: [AIoTLab - FIT DNU](https://www.facebook.com/DNUAIoTLab) 