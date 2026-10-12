# 🧪 BẢNG TEST CASES CHI TIẾT: TÍNH NĂNG NOVEL AUDIO & CHẠY NGẦM

> **Tài liệu tham chiếu:** [NOVEL_AUDIO_FEATURE_ROADMAP.md](file:///Users/dinh/AndroidStudioProjects/GcNh/.claude/roadmap/NOVEL_AUDIO_FEATURE_ROADMAP.md)  
> **Quy trình làm việc:** Viết Test Case -> Code từng bước -> Kiểm thử theo Test Case ID tương ứng (Check vào ô vuông khi pass).

---

## 📋 MỤC LỤC THEO MÔ-ĐUN / ROADMAP

- [Mô-đun 1: Data Models & Cấu Trúc Lưu Trữ (Bước 1)](#-mô-đun-1-data-models--cấu-trúc-lưu-trữ-bước-1)
- [Mô-đun 2: Web Novel Content Parser (Bước 2)](#-mô-đun-2-web-novel-content-parser-bước-2)
- [Mô-đun 3: AI Text-to-Speech Engine & Quản Lý API Key (Bước 3)](#-mô-đun-3-ai-text-to-speech-engine--quản-lý-api-key-bước-3)
- [Mô-đun 4: Background Audio Player Service (Bước 4)](#-mô-đun-4-background-audio-player-service-bước-4)
- [Mô-đun 5: Audio Player UI/UX & Flow (Bước 5)](#-mô-đun-5-audio-player-uiux--flow-bước-5)
- [Mô-đun 6: Quản Lý Tải Offline - Chữ & Audio (Bước 6)](#-mô-đun-6-quản-lý-tải-offline---chữ--audio-bước-6)
- [Mô-đun 7: Màn Hình Quản Lý Audio, API Key & Tính Năng Nâng Cao (Bước 7)](#-mô-đun-7-màn-hình-quản-lý-audio-api-key--tính-năng-nâng-cao-bước-7)
- [Mô-đun 8: Kiểm Thử Xử Lý Lỗi & Bảo Trì (Bước 8)](#-mô-đun-8-kiểm-thử-xử-lý-lỗi--bảo-trì-bước-8)

---

## 📌 MÔ-ĐUN 1: DATA MODELS & CẤU TRÚC LƯU TRỮ (Bước 1)

- [ ] **TC-MOD1-01** - _Happy Path_: **Tạo mới và lưu trữ thực thể Novel, Chapter, ApiKeyInfo, AudioPlayerState**
  - **Steps**: 1. Khởi chạy ứng dụng hoặc chạy unit test tạo các đối tượng `Novel`, `Chapter`, `ApiKeyInfo`, `AudioPlayerState`, `ParserConfig`. 2. Lưu vào Database (Room/Local DB). 3. Truy vấn lại từ DB.
  - **Expected Result**: Dữ liệu được ghi và đọc thành công, không mất mát các trường thông tin mới thêm vào.

- [ ] **TC-MOD1-02** - _Happy Path_: **Lưu và phục hồi tiến độ nghe/đọc (Progress Saver)**
  - **Steps**: 1. Lưu trạng thái `AudioPlayerState` (ví dụ: `currentChapterId = 2`, `currentChunkIndex = 5`). 2. Đóng ứng dụng hoàn toàn (Kill app) rồi mở lại. 3. Đọc dữ liệu tiến độ đã lưu.
  - **Expected Result**: App đọc đúng vị trí chương và vị trí chunk đã lưu trước đó.

- [x] **TC-MOD1-03** - _Regression_: **Kiểm tra tương thích hồi quy ứng dụng hiện tại**
  - **Steps**: 1. Biên dịch và khởi chạy app trên thiết bị/máy ảo. 2. Điều hướng vào các màn hình hiện có của app.
  - **Expected Result**: App build thành công (không lỗi compile), các màn hình và chức năng cũ vẫn hoạt động trơn tru.

---

## 📌 MÔ-ĐUN 2: WEB NOVEL CONTENT PARSER (Bước 2)

- [x] **TC-MOD2-01** - _Happy Path_: **Parse nội dung chương từ truyenfull**
  - **Steps**: 1. Nhập/Dán URL hợp lệ 1 chương từ `truyenfull.live` vào Modal/Màn hình test. 2. Nhấn nút "Parse/Trích xuất".
  - **Expected Result**: Bóc tách chính xác: **Tên truyện**, **Tên chương**, **Nội dung chữ**, và **Link chương kế tiếp** (`nextChapterUrl`).

- [x] **TC-MOD2-02** - _Happy Path_: **Làm sạch văn bản (Sanitize Text)**
  - **Steps**: 1. Parse một chương truyện có chứa quảng cáo xen kẽ. 2. Kiểm tra chuỗi `sanitizedContent`.
  - **Expected Result**: Các đoạn quảng cáo, ký tự rác được lọc sạch; dấu ngắt câu, ký tự đặc biệt được chuẩn hóa.

- [x] **TC-MOD2-03** - _Edge Case_: **Parse chương cuối cùng của truyện**
  - **Steps**: 1. Nhập URL của chương cuối cùng trong một bộ truyện. 2. Nhấn "Parse/Trích xuất".
  - **Expected Result**: Bóc tách thành công nội dung chương; trường `nextChapterUrl` trả về `null` hoặc cờ nhận biết đã hết.

- [x] **TC-MOD2-04** - _Error Handling_: **Nhập URL không hợp lệ hoặc link lỗi 404**
  - **Steps**: 1. Nhập link hỏng (ví dụ: link 404 hoặc text không phải URL). 2. Nhấn "Parse/Trích xuất".
  - **Expected Result**: App hiển thị thông báo lỗi thân thiện, không bị crash.

- [x] **TC-MOD2-05** - _Error Handling_: **Nhập URL từ domain chưa hỗ trợ**
  - **Steps**: 1. Nhập URL từ một trang web lạ chưa cấu hình trong `ParserConfig`. 2. Nhấn "Parse/Trích xuất".
  - **Expected Result**: Hiển thị thông báo: "Trang web này chưa được hỗ trợ bóc tách tự động".

---

## 📌 MÔ-ĐUN 3: AI TEXT-TO-SPEECH ENGINE & QUẢN LÝ API KEY (Bước 3)

- [ ] **TC-MOD3-01** - _Happy Path_: **Chia nhỏ văn bản thành các chunks thông minh**
  - **Steps**: 1. Đưa vào nội dung 1 chương dài. 2. Chạy thuật toán chia đoạn.
  - **Expected Result**: Văn bản được chia thành các đoạn 200 - 400 từ; các vết cắt nằm ở cuối câu, không bị đứt đoạn giữa từ.

- [ ] **TC-MOD3-02** - _Happy Path_: **Chuyển đổi TTS và phát âm thanh chunk đầu tiên**
  - **Steps**: 1. Chọn nội dung đã trích xuất, nhấn "Tạo Audio". 2. Đợi phản hồi từ Gemini API.
  - **Expected Result**: App nhận được luồng/file âm thanh và phát ra loa rõ ràng.

- [ ] **TC-MOD3-03** - _Happy Path_: **Auto Rotation - Tự động xoay vòng API Key khi hết Quota**
  - **Steps**: 1. Thiết lập 2 API Key (Key 1 cố tình chọn key hết quota/lỗi 429, Key 2 hoạt động tốt). 2. Bấm "Tạo Audio".
  - **Expected Result**: App phát hiện Key 1 lỗi/hết Quota, tự động mark `QUOTA_EXCEEDED`, xoay sang Key 2 và phát âm thanh bình thường mà không dừng luồng.

- [ ] **TC-MOD3-04** - _Happy Path_: **Cơ chế Pre-buffering gối đầu giữa các chunks**
  - **Steps**: 1. Bắt đầu phát `chunk[0]`. 2. Quan sát log tải nền. 3. Để âm thanh phát hết `chunk[0]`.
  - **Expected Result**: `chunk[1]` được tự động gọi API chuyển đổi ngầm trước khi `chunk[0]` kết thúc. Âm thanh tự nối tiếp mượt mà.

- [ ] **TC-MOD3-05** - _Happy Path_: **Quản lý Cache tạm các file audio**
  - **Steps**: 1. Nghe qua đoạn `chunk[0]`. 2. Bấm phát lại đoạn `chunk[0]`.
  - **Expected Result**: App phát ngay từ file cache trong bộ nhớ tạm mà không gọi lại API TTS.

- [ ] **TC-MOD3-06** - _Edge Case_: **Đọc văn bản chứa nhiều từ Hán Việt, số và hội thoại**
  - **Steps**: 1. Nhập đoạn văn có nhiều số thập phân, đối thoại ngoặc kép. 2. Tạo audio và lắng nghe.
  - **Expected Result**: AI TTS đọc trôi chảy, không bị nuốt chữ hoặc phát âm sai lệch.

---

## 📌 MÔ-ĐUN 4: BACKGROUND AUDIO PLAYER SERVICE (Bước 4)

- [ ] **TC-MOD4-01** - _Happy Path_: **Phát audio khi thoát ra Home Screen**
  - **Steps**: 1. Bấm phát audio. 2. Nhấn nút Home để về chạy nền.
  - **Expected Result**: Audio vẫn tiếp tục phát liên tục.

- [ ] **TC-MOD4-02** - _Happy Path_: **Phát audio khi khóa màn hình (Lockscreen)**
  - **Steps**: 1. Đang phát audio, bấm nút nguồn khóa màn hình. 2. Màn hình tắt.
  - **Expected Result**: Âm thanh vẫn tiếp tục phát, màn hình khóa hiển thị widget trình phát.

- [ ] **TC-MOD4-03** - _Happy Path_: **Điều khiển trên Notification & Màn hình khóa**
  - **Steps**: 1. Khóa màn hình hoặc kéo thanh thông báo. 2. Bấm Pause, Play, Next Chapter.
  - **Expected Result**: Trình phát phản hồi chính xác, Notification hiển thị đúng Tên truyện, Tên chương.

- [ ] **TC-MOD4-04** - _Edge Case_: **Xử lý khi ngắt kết nối tai nghe**
  - **Steps**: 1. Cắm tai nghe/Bluetooth và đang phát truyện. 2. Rút tai nghe hoặc tắt bluetooth.
  - **Expected Result**: Trình phát tự động **Pause** ngay lập tức.

- [ ] **TC-MOD4-05** - _Edge Case_: **Xử lý gián đoạn khi có cuộc gọi đến**
  - **Steps**: 1. Đang nghe truyện, có cuộc gọi đến. 2. Nhấc máy. 3. Kết thúc cuộc gọi.
  - **Expected Result**: Audio tự động Pause khi chuông reo. Sau khi kết thúc, audio tự động resume (hoặc chờ người dùng bấm play).

---

## 📌 MÔ-ĐUN 5: AUDIO PLAYER UI/UX & FLOW (Bước 5)

- [ ] **TC-MOD5-01** - _Happy Path_: **Màn hình Full Player hiển thị đầy đủ thông tin**
  - **Steps**: 1. Mở màn hình Trình phát Audio.
  - **Expected Result**: Hiển thị chuẩn: Ảnh bìa, Tên truyện, Tên chương, Thanh trượt Seekbar, cụm nút Play/Pause/Next/Prev/Tốc độ.

- [ ] **TC-MOD5-02** - _Happy Path_: **Thu nhỏ thành Mini Player ghim đáy màn hình**
  - **Steps**: 1. Đang mở Full Player, vuốt xuống/thu nhỏ.
  - **Expected Result**: Mini Player xuất hiện ở đáy màn hình (Tên chương, nút Play/Pause), duyệt các trang khác không mất nhạc.

- [ ] **TC-MOD5-03** - _Happy Path_: **Tự động chuyển chương và đánh dấu "Đã nghe"**
  - **Steps**: 1. Nghe chương truyện đến hết chunk cuối cùng.
  - **Expected Result**: App tự động đánh dấu `listenStatus = 'LISTENED'`, gọi Parser lấy chương mới và tự động phát tiếp tục.

- [ ] **TC-MOD5-04** - _Edge Case_: **Đọc hết chương cuối cùng của bộ truyện**
  - **Steps**: 1. Nghe đến hết chunk cuối của chương cuối cùng (không còn next url).
  - **Expected Result**: Trình phát dừng lại êm ái, hiển thị thông báo "Đã hoàn thành bộ truyện".

---

## 📌 MÔ-ĐUN 6: QUẢN LÝ TẢI OFFLINE - CHỮ & AUDIO (Bước 6)

- [ ] **TC-MOD6-01** - _Happy Path_: **Tải toàn bộ truyện chữ (Batch Download Text)**
  - **Steps**: 1. Tại màn hình thông tin truyện, nhấn "Tải toàn bộ truyện".
  - **Expected Result**: Thanh tiến trình cập nhật đều đặn, hỗ trợ tạm dừng. Tắt mạng vẫn mở đọc được nội dung chữ.

- [ ] **TC-MOD6-02** - _Happy Path_: **Tải file Audio của một chương tùy chọn**
  - **Steps**: 1. Chọn 1 chương và bấm nút "Tải Audio".
  - **Expected Result**: Nút tải đổi sang trạng thái đang tải kèm tiến trình %. Hệ thống lưu thành 1 file MP3 hoàn chỉnh. Trạng thái đổi thành "Đã tải".

- [ ] **TC-MOD6-03** - _Happy Path_: **Cơ chế ưu tiên phát Audio Offline**
  - **Steps**: 1. Tắt mạng (Wifi/4G). 2. Bấm phát chương đã "Đã tải audio".
  - **Expected Result**: Trình phát nhận diện file local và phát ngay lập tức, không báo lỗi mạng, không gọi API.

- [ ] **TC-MOD6-04** - _Error Handling_: **Lỗi mạng khi đang tải Audio/Truyện chữ**
  - **Steps**: 1. Đang tải, ngắt mạng đột ngột.
  - **Expected Result**: Trạng thái tải chuyển sang FAILED hoặc Tạm dừng. Khi có mạng, có thể bấm thử lại (Retry).

---

## 📌 MÔ-ĐUN 7: MÀN HÌNH QUẢN LÝ AUDIO, API KEY & TÍNH NĂNG NÂNG CAO (Bước 7)

- [ ] **TC-MOD7-01** - _Happy Path_: **Thêm và Quản lý Gemini API Key**
  - **Steps**: 1. Vào giao diện Quản lý API Key, thêm thử 3-4 Key. 2. Bấm "Kiểm tra Key".
  - **Expected Result**: Form hỗ trợ dán nhiều Key. Danh sách hiển thị đúng trạng thái (Active, Quota Exceeded, Invalid) bằng Badge. Có nút Đặt lại trạng thái Quota.

- [ ] **TC-MOD7-02** - _Happy Path_: **Màn hình Quản lý Audio Tải về (Downloading, Downloaded, Listened)**
  - **Steps**: 1. Mở Quản lý Audio Tải về. 2. Kiểm tra tab "Đang tải", "Đã tải", "Đã nghe". 3. Chọn nhiều file và xóa hàng loạt.
  - **Expected Result**: Tab Đang tải có % tiến trình. Tab Đã tải hiển thị dung lượng MB và xóa thành công. Tab Đã nghe hiển thị đúng nhãn.

- [ ] **TC-MOD7-03** - _Happy Path_: **Bộ hẹn giờ tắt (Sleep Timer)**
  - **Steps**: 1. Chọn Hẹn giờ tắt (15m hoặc "Hết chương hiện tại"). 2. Chờ hết thời gian/hết chương.
  - **Expected Result**: Âm thanh tự động dừng hẳn, Service dừng chạy ngầm đúng thời điểm.

- [ ] **TC-MOD7-04** - _Happy Path_: **Điều chỉnh tốc độ đọc (Playback Speed)**
  - **Steps**: 1. Thay đổi tốc độ phát: 0.75x, 1.0x, 1.25x, 1.5x, 2.0x.
  - **Expected Result**: Tốc độ đọc thay đổi tương ứng, âm thanh chuẩn xác.

- [ ] **TC-MOD7-05** - _Edge Case_: **Cấp quyền miễn tối ưu pin (Disable Battery Optimization)**
  - **Steps**: 1. Nhấn nút yêu cầu "Tắt tối ưu hóa pin" (trên Android).
  - **Expected Result**: Ứng dụng điều hướng đúng màn hình thiết lập hệ thống.

---

## 📌 MÔ-ĐUN 8: KIỂM THỬ XỬ LÝ LỖI & BẢO TRÌ (Bước 8)

- [ ] **TC-MOD8-01** - _Error Handling_: **Toàn bộ API Key đều hết Quota**
  - **Steps**: 1. Cố tình nhập tất cả các Key đều là Key đã hết Quota/Lỗi. 2. Bấm Tạo Audio.
  - **Expected Result**: App hiển thị thông báo rõ ràng "Tất cả API Key hiện tại đã đạt giới hạn sử dụng, vui lòng thêm Key mới hoặc đợi hệ thống tự làm mới".

- [ ] **TC-MOD8-02** - _Error Handling_: **Mất kết nối Internet (đối với nội dung chưa tải)**
  - **Steps**: 1. Tắt kết nối mạng, bấm nghe chương chưa tải.
  - **Expected Result**: Hiển thị thông báo "Không có kết nối mạng", tự động thử lại khi có mạng.

- [ ] **TC-MOD8-03** - _Error Handling_: **Fallback API dự phòng**
  - **Steps**: 1. Giả lập tất cả Gemini Key đều lỗi. 2. Chờ luồng xử lý Fallback.
  - **Expected Result**: Hệ thống tự động chuyển sang API dự phòng (Edge-TTS hoặc Native Device Speech) (nếu được hỗ trợ).

- [ ] **TC-MOD8-04** - _Error Handling_: **Link truyện bị thay đổi / lỗi 404**
  - **Steps**: 1. Bấm vào một truyện có cấu trúc URL đã bị đổi hoặc không tồn tại.
  - **Expected Result**: Báo lỗi thân thiện, không bị crash app.

---

## 🔄 QUY TRÌNH PHỐI HỢP & MAPPING KIỂM THỬ

- 🏁 **Hoàn thành Bước 1** -> Chạy kiểm thử: `TC-MOD1-01` đến `TC-MOD1-03`.
- 🏁 **Hoàn thành Bước 2** -> Chạy kiểm thử: `TC-MOD2-01` đến `TC-MOD2-05`.
- 🏁 **Hoàn thành Bước 3** -> Chạy kiểm thử: `TC-MOD3-01` đến `TC-MOD3-06`.
- 🏁 **Hoàn thành Bước 4** -> Chạy kiểm thử: `TC-MOD4-01` đến `TC-MOD4-05`.
- 🏁 **Hoàn thành Bước 5** -> Chạy kiểm thử: `TC-MOD5-01` đến `TC-MOD5-04`.
- 🏁 **Hoàn thành Bước 6** -> Chạy kiểm thử: `TC-MOD6-01` đến `TC-MOD6-04`.
- 🏁 **Hoàn thành Bước 7** -> Chạy kiểm thử: `TC-MOD7-01` đến `TC-MOD7-05`.
- 🏁 **Hoàn thành Bước 8** -> Chạy kiểm thử: `TC-MOD8-01` đến `TC-MOD8-04`.
