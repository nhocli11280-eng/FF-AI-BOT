# FF-AI-BOT

Ứng dụng Android thử nghiệm trợ lý nổi cho Free Fire.

## Trạng thái hiện tại
- Android Kotlin, minSdk 26.
- Xin quyền MediaProjection để người dùng chủ động chia sẻ màn hình.
- Hiển thị cửa sổ nổi và thông báo foreground khi đang chia sẻ.
- Có nút dừng chia sẻ.
- **Chưa tích hợp mô hình AI nhận diện kẻ địch hoặc gợi ý chiến thuật.** Bản này là nền tảng kỹ thuật ban đầu.

Ứng dụng không tự ngắm, tự bắn, di chuyển hoặc gửi thao tác vào trò chơi.

## Build APK
GitHub Actions tự chạy khi push lên nhánh `main` hoặc có thể chạy thủ công ở tab Actions.
Sau khi build thành công, mở workflow run và tải artifact `FF-AI-BOT-debug`.
