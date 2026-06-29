# Cờ E-ink — Cờ vua / Cờ tướng / Cờ vây

Ứng dụng 3-trong-1 chơi cờ với máy, thiết kế riêng cho **máy đọc sách màn e-ink**
(thử nghiệm trên **Likebook Mars 7.8", Android 8.1**). Giao diện đen–trắng tương phản
cao, **không animation**, có nút **Làm mới** để xóa bóng mờ (ghosting).

## Tính năng

- **3 loại cờ** chọn ngay ở màn hình chính:
  - ♟️ **Cờ vua** — đủ luật (nhập thành, bắt tốt qua đường, phong hậu), AI minimax + alpha-beta.
  - 🩠 **Cờ tướng** — đủ luật (mã cản chân, tượng cản mắt, pháo cần ngòi, lộ mặt tướng), AI minimax + alpha-beta.
  - ⚫ **Cờ vây** — bàn 9×9 / 13×13 / 19×19, luật vây bắt, cấm tự sát, luật **ko**, tính điểm theo khu vực (có komi). AI theo heuristic.
- **3 mức độ khó**: Dễ / Vừa / Khó.
- Người chơi cầm **Trắng** (cờ vua), **Đỏ** (cờ tướng), **Đen** (cờ vây) — đều đi trước.
- Nút **Ván mới / Đi lại / Bỏ lượt (cờ vây) / Làm mới màn hình**.
- Chạy **100% offline**, không quảng cáo, không quyền truy cập gì.

## Cách build ra file APK

Cần **Android Studio** (hoặc Android SDK + JDK 17).

```bash
# Mở project bằng Android Studio rồi bấm Run/Build, HOẶC build bằng dòng lệnh:
./gradlew assembleDebug
# File APK nằm ở: app/build/outputs/apk/debug/app-debug.apk
```

Chép `app-debug.apk` vào máy Likebook Mars và cài (bật "Cài app từ nguồn không xác định").

> Lưu ý: minSdk = 26 (Android 8.0), targetSdk = 34. Máy Likebook Mars (Android 8.1) chạy tốt.

## Cấu trúc mã nguồn

```
app/src/main/java/com/eink/boardgames/
├── MainActivity.kt        # Màn hình chọn cờ + độ khó + cỡ bàn
├── GameActivity.kt        # Khung chơi: bàn cờ + thanh nút
├── GameView.kt            # Lớp View nền chung (tối ưu e-ink)
├── chess/                 # Cờ vua: Logic + AI + View
├── xiangqi/               # Cờ tướng: Logic + AI + View
└── go/                    # Cờ vây: Logic + AI + View
```

## Ghi chú & hướng phát triển tiếp

- **AI cờ vua/cờ tướng** dùng engine Kotlin tự viết (minimax + alpha-beta). Có thể cắm thêm
  **Stockfish** (qua giao thức UCI) cho cờ vua nếu muốn mạnh hơn — kiến trúc đã tách sẵn lớp logic/AI.
- **AI cờ vây** là heuristic (bắt quân/atari/ảnh hưởng), chơi vui mức giải trí; muốn mạnh cần engine
  ngoài (vd. KataGo) — vượt phạm vi ứng dụng nhẹ này.
- Tính điểm cờ vây dùng luật khu vực bản đơn giản, **chưa tự loại quân chết**; kết thúc ván bằng
  hai lần **Bỏ lượt** liên tiếp.
