# FF-AI-BOT

Ứng dụng Android thử nghiệm trợ lý nổi phân tích hình ảnh màn hình.

## Tính năng thử nghiệm
- Người dùng chủ động cấp quyền MediaProjection để chia sẻ màn hình.
- Dùng mô hình MediaPipe EfficientDet-Lite0 (COCO) chạy trên thiết bị để tìm nhãn `person`.
- Vẽ khung đỏ và điểm tin cậy quanh vật thể được mô hình phân loại là người.
- Có cửa sổ nổi và nút dừng phân tích.

## Giới hạn quan trọng
Mô hình COCO là mô hình nhận diện vật thể phổ thông, **không được huấn luyện riêng cho Free Fire**. Khung đỏ chỉ biểu thị hình ảnh giống người; ứng dụng không thể xác định chắc chắn đó là địch hay đồng đội, và có thể bỏ sót hoặc nhận nhầm nhân vật trong game. Muốn nhận diện địch đáng tin cậy hơn cần dữ liệu gán nhãn phù hợp và mô hình chuyên biệt.

Ứng dụng chỉ phân tích và hiển thị trực quan; không tự ngắm, tự bắn, di chuyển hoặc gửi thao tác vào trò chơi.

## Build APK
GitHub Actions tự chạy khi push lên nhánh `main` hoặc có thể chạy thủ công ở tab Actions. Sau khi build thành công, tải artifact `FF-AI-BOT-debug`.
