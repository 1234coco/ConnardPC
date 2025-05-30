# ConnardPC

**ConnardPC** là phần mềm hỗ trợ quản lý việc chơi game trên máy tính cá nhân, được phát triển bởi học sinh THCS. Phần mềm giúp phụ huynh giám sát và giới hạn thời gian chơi game, đồng thời ngăn chặn việc chơi ngoài thời gian cho phép thông qua cơ chế xác thực từ điện thoại.

> 🎉 Dự án này đã đạt **Giải Ba** cuộc thi **Tin học trẻ TPHCM năm 2025**.

---

## 🧠 Mục tiêu

- Giúp phụ huynh kiểm soát việc sử dụng máy tính của con em.
- Ngăn chặn chơi game không đúng thời điểm hoặc vượt quá thời lượng cho phép.
- Quản lý từ xa thông qua ứng dụng di động Android.

---

## ⚙️ Tính năng hiện tại

- ✅ Quản lý danh sách trò chơi cài trên máy tính.
- ✅ Cấu hình từng trò chơi với 3 chế độ:
  - **Cho phép** chơi
  - **Giới hạn thời gian** chơi
  - **Tạm ngưng** không được chơi
- ✅ Đồng bộ cấu hình từ điện thoại thông qua backend.
- ✅ Backend kiểm soát thiết bị và cấu hình game theo từng người dùng.
- ✅ Giao diện Android cho phép chỉnh sửa cấu hình và xác thực.

> 🛠️ Các tính năng bảo vệ file, mã hóa, và giám sát phần mềm **chưa được triển khai** trong phiên bản này – đây là định hướng phát triển tương lai.

---

## 🛠️ Công nghệ sử dụng

| Thành phần         | Công nghệ       |
|--------------------|------------------|
| Backend API        | Python + FastAPI |
| Cơ sở dữ liệu      | SQLite           |
| Ứng dụng PC        | Python           |
| Ứng dụng Android   | Java (Android SDK) |

---

## 🏆 Thành tích

- Đạt **Giải Ba** cuộc thi **Tin học trẻ năm 2025** với dự án ConnardPC – một giải pháp quản lý chơi game trong gia đình.

---

## 📄 Giấy phép

Phần mềm này được phát hành dưới giấy phép tùy chỉnh:

> **“Personal Non-Commercial and Non-Competitive License”**

### Bạn được phép:
- Xem mã nguồn và sử dụng cho mục đích học tập hoặc cá nhân.

### Bạn không được phép:
- Sử dụng phần mềm trong bất kỳ cuộc thi, chương trình dự thi hoặc sự kiện cạnh tranh nào khác.
- Thương mại hóa, bán hoặc tích hợp vào sản phẩm thương mại.
- Tái phân phối mã nguồn đã chỉnh sửa mà không có sự đồng ý của tác giả.

**Tác giả giữ toàn quyền.** Vui lòng liên hệ nếu bạn muốn sử dụng phần mềm ngoài mục đích cá nhân.

---

## 📥 Cài đặt cơ bản (PC)

```bash
git clone https://github.com/your-username/ConnardPC.git
cd ConnardPC
pip install -r requirements.txt
python main.py
```
