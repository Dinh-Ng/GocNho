# 🗺️ LỘ TRÌNH PHÁT TRIỂN TÍNH NĂNG: CHUYỂN TRUYỆN CHỮ THÀNH AUDIO & CHẠY NGẦM

> **Ghi chú dành cho Claude AI:**
>
> * Đây là **tính năng mới bổ sung vào một ứng dụng đã có sẵn**.
>
> * Trước khi triển khai bất kỳ bước nào, hãy đọc kỹ cấu trúc thư mục hiện tại của dự án, tuân thủ các quy chuẩn về đặt tên, quản lý trạng thái (state management), và thành phần giao diện (UI components) đang được áp dụng trong codebase này.
>
> * Yêu cầu thực hiện **đúng và duy nhất bước được người dùng chỉ định**. Sau khi hoàn thành một bước, hãy dừng lại để người dùng tự kiểm thử (test) trước khi đi tiếp.

---

## 📌 BƯỚC 1: ĐỊNH NGHĨA DATA MODELS VÀ CẤU TRÚC LƯU TRỮ

### 🎯 Mục tiêu:
Thiết lập các kiểu dữ liệu (Types/Interfaces) và Schema lưu trữ thông tin về Truyện, Chương, Trạng thái Player, Cấu hình Parser và Trạng thái Tải Offline (Chữ & Audio).

### 📝 Nhiệm vụ cho Claude:
1. Tạo file chứa các Type/Interface cơ bản cho tính năng mới:
   * `Novel`: `id`, `title`, `author`, `sourceUrl`, `coverUrl`, `currentChapterId`, `totalChapters`, `isFullTextDownloaded`, `downloadProgress`.
   * `Chapter`: `id`, `novelId`, `chapterIndex`, `title`, `sourceUrl`, `rawContent`, `sanitizedContent`, `isAudioDownloaded`, `localAudioPath`.
   * `AudioPlayerState`: `isPlaying`, `currentChapterId`, `currentChunkIndex`, `playbackSpeed`, `sleepTimerMinutes`, `remainingSleepTime`, `isOfflineMode`.
   * `ParserConfig`: `domain`, `titleSelector`, `contentSelector`, `nextChapterSelector`.
2. Khởi tạo service/helper để lưu trữ trạng thái nghe (Progress Saver) và dữ liệu tải về vào Local Storage / DB hiện tại của app (như AsyncStorage, Room, Realm, MMKV, Hive, SQLite...).

### 🧪 Hướng dẫn Kiểm thử (User Checklist):
* [ ] Code không bị lỗi biên dịch (Compile Error).
* [ ] Mở app hiện tại, các tính năng cũ vẫn hoạt động bình thường mà không bị ảnh hưởng.

---

## 📌 BƯỚC 2: WEB NOVEL CONTENT PARSER (TRÍCH XUẤT NỘI DUNG TRUYỆN)

### 🎯 Mục tiêu:
Xây dựng module cào và lọc văn bản từ các trang truyện web phổ biến (`truyenfull`, `bachngocsach`,...) thành dữ liệu chữ sạch.

### 📝 Nhiệm vụ cho Claude:
1. Tạo service `NovelParserService`:
   * Hàm `parseChapter(url)`: Nhận vào link web chương truyện, nhận diện domain để chọn selector phù hợp (ví dụ: `truyenfull.vn` dùng `#truyen-noidung`, `bachngocsach.com.vn` dùng `#noi-dung`).
   * Tải HTML từ URL và trích xuất: Tên truyện, Tên chương, Nội dung chính, Link chương tiếp theo (`nextChapterUrl`).
2. Tạo hàm `sanitizeText(rawText)`:
   * Lọc bỏ các đoạn văn bản rác (quảng cáo chèn giữa trang, link giới thiệu).
   * Chuẩn hóa các từ viết tắt, hán việt, ký tự đặc biệt để phục vụ cho bộ đọc AI.
3. Dựng một màn hình UI đơn giản hoặc một ô Modal nhập link URL để chạy thử hàm Parser.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):
* [ ] Dán link 1 chương từ `truyenfull` hoặc `bachngocsach` vào app.
* [ ] Kiểm tra xem app có bóc tách được đúng **Tên chương**, **Nội dung chữ sạch** (không dính quảng cáo) và **Link chương tiếp theo** hay không.

---

## 📌 BƯỚC 3: AI TEXT-TO-SPEECH ENGINE & CƠ CHẾ CHIA ĐOẠN (CHUNKING)

### 🎯 Mục tiêu:
Chuyển đổi văn bản chữ thành file/luồng âm thanh (Audio) thông qua AI (Gemini TTS API / Edge-TTS) và xử lý phát gối đầu (Pre-buffering) giúp âm thanh chạy mượt mà.

### 📝 Nhiệm vụ cho Claude:
1. Tạo `TTSService`:
   * Tích hợp API Gemini / Google Cloud TTS / Edge-TTS để gửi text và nhận về file audio (.mp3/stream).
   * Xử lý chia văn bản thành từng đoạn nhỏ (`chunks`) từ 200 - 400 từ để tối ưu thời gian phản hồi của AI.
2. Thiết lập cơ chế **Queue & Pre-buffering**:
   * Khi đoạn `chunk[i]` đang phát, tự động gọi API chuyển đổi sẵn đoạn `chunk[i+1]` ở background.
   * Quản lý Cache các file mp3 tạm thời vào bộ nhớ đệm thiết bị.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):
* [ ] Bấm nút "Tạo Audio" từ nội dung đã parse ở Bước 2.
* [ ] App nhận được âm thanh và phát ra tiếng.
* [ ] Âm thanh phát hết đoạn 1 tự động chuyển sang đoạn 2 mượt mà, không bị khựng hoặc dừng lâu.

---

## 📌 BƯỚC 4: BACKGROUND AUDIO PLAYER SERVICE (CHẠY NGẦM KHI TẮT MÀN HÌNH)

### 🎯 Mục tiêu:
Cho phép âm thanh phát liên tục khi người dùng khóa màn hình, thoát app ra Home screen hoặc chuyển sang app khác.

### 📝 Nhiệm vụ cho Claude:
1. Triển khai Background Service cho âm thanh dựa trên nền tảng hiện tại của app (Android `Foreground Service` + `MediaSession` / iOS `AVAudioSession` / Flutter `audio_service` / React Native `react-native-track-player`).
2. Thiết lập Notification Control & Lockscreen Media Controls:
   * Hiển thị Tên truyện, Tên chương, Thanh thời lượng (Progress bar).
   * Cung cấp các nút bấm: Play, Pause, Next Chapter, Previous Chapter.
3. Xử lý các sự kiện âm thanh hệ thống (Interruption Handling):
   * Tự động Pause khi có cuộc gọi đến hoặc thiết bị ngắt kết nối Bluetooth.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):
* [ ] Bấm phát audio, bấm nút Home thoát ra ngoài màn hình chính ➔ Audio vẫn tiếp tục chạy.
* [ ] Tắt/Khóa màn hình điện thoại ➔ Audio vẫn phát, màn hình khóa hiển thị widget trình phát audio.
* [ ] Bấm Play/Pause trên màn hình khóa hoặc Notification ➔ Trạng thái audio phản hồi đúng.

---

## 📌 BƯỚC 5: XÂY DỰNG GIAO DIỆN NGUYÊN BẢN (UI/UX TRÌNH PHÁT AUDIO)

### 🎯 Mục tiêu:
Gọt giũa giao diện người dùng, tích hợp trình phát audio hoàn chỉnh vào dòng chảy chính của ứng dụng.

### 📝 Nhiệm vụ cho Claude:
1. Thiết kế Giao diện Trình phát Audio (Audio Player Screen / Bottom Sheet Player):
   * Màn hình Player đầy đủ: Artwork/Ảnh bìa truyện, Tên chương, Slider thời lượng, Nút Play/Pause, Next/Prev, Nút chỉnh Tốc độ đọc.
   * Thanh Player nhỏ (Mini Player Widget) ghim ở đáy màn hình giúp người dùng duyệt các trang khác trong app vẫn điều khiển được nhạc.
2. Thêm tính năng **Tự động chuyển chương (Auto Next Chapter)**:
   * Khi phát đến cuối đoạn chunk cuối cùng của chương hiện tại ➔ Tự động gọi Parser lấy dữ liệu chương mới và tiếp tục phát.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):
* [ ] Mở app, mở trình phát audio ➔ Giao diện hiển thị chuẩn đẹp, đồng bộ với phong cách thiết kế hiện tại của app.
* [ ] Thu nhỏ Player thành Mini Player ➔ Duyệt chuyển giữa các màn hình trong app không bị mất nhạc.
* [ ] Nghe hết 1 chương ➔ App tự động chuyển sang chương tiếp theo và tiếp tục đọc.

---

## 📌 BƯỚC 6: QUẢN LÝ TẢI TRUYỆN CHỮ VÀ TẢI AUDIO OFFLINE

### 🎯 Mục tiêu:
Cho phép người dùng tải toàn bộ nội dung chữ của một bộ truyện và tải file Audio của từng chương tùy chọn để đọc/nghe không cần mạng Internet.

### 📝 Nhiệm vụ cho Claude:
1. **Tính năng Tải toàn bộ Truyện chữ (Full Novel Text Batch Download):**
   * Thêm nút **"Tải toàn bộ truyện"** ở màn hình Chi tiết Truyện (Novel Detail).
   * Xây dựng tiến trình tải ngầm (`BatchDownloadService`): Lần lượt gọi `NovelParserService` lấy nội dung từ chương 1 đến chương cuối, lưu trực tiếp vào cơ sở dữ liệu local (SQLite/Realm/Hive...).
   * Hiển thị thanh tiến trình (Progress Bar: e.g., *"Đã tải 45/120 chương"*), hỗ trợ tạm dừng / tiếp tục tải.
2. **Tính năng Tải File Audio của Chap Tùy chọn (Offline Audio Download):**
   * Thêm nút icon **"Tải Audio"** bên cạnh danh sách chương và trên màn hình Player.
   * Gọi `TTSService` tổng hợp tất cả các đoạn audio chunk của chương đó, ghép lại thành 1 file MP3 duy nhất và lưu vào thư mục đệm của app (`localAudioPath`).
   * **Cơ chế Ưu tiên phát Offline:** Khi người dùng mở chương đã tải, Player sẽ tự động phát từ file âm thanh local mà không gọi API Gemini hay tốn dung lượng mạng.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):
* [ ] Bấm nút "Tải toàn bộ truyện" ➔ Kiểm tra tiến trình chạy, tắt Wifi/4G và mở từng chương ra vẫn hiển thị đầy đủ nội dung chữ.
* [ ] Chọn 1 chương bất kỳ và bấm "Tải Audio" ➔ File MP3 được lưu vào máy.
* [ ] Tắt Wifi/4G, bấm phát Audio chương đã tải ➔ Trình phát vẫn chạy bình thường không báo lỗi mạng.

---

## 📌 BƯỚC 7: TÍNH NĂNG NÂNG CAO (HẸN GIỜ, TỐC ĐỘ, BỘ NHỚ CACHE)

### 🎯 Mục tiêu:
Hoàn thiện các tiện ích mở rộng nhằm tối ưu hóa trải nghiệm nghe truyện audio.

### 📝 Nhiệm vụ cho Claude:
1. Triển khai **Bộ hẹn giờ tắt (Sleep Timer)**:
   * Tùy chọn tắt sau 15m, 30m, 60m hoặc khi phát hết chương hiện tại.
2. Triển khai **Điều chỉnh Tốc độ đọc (Playback Speed)**:
   * Hỗ trợ các mốc tốc độ: 0.75x, 1.0x, 1.25x, 1.5x, 2.0x.
3. **Quản lý Bộ nhớ & File Tải về**:
   * Màn hình quản lý dung lượng: Hiển thị danh sách các chương/file audio đã tải về.
   * Cung cấp nút xóa file audio từng chương hoặc xóa toàn bộ cache âm thanh để giải phóng bộ nhớ.
4. Thêm cảnh báo/hướng dẫn người dùng cấp quyền "Miễn tối ưu hóa pin" (Disable Battery Optimization) trên Android để tránh hệ điều hành tự diệt app ngầm.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):
* [ ] Chỉnh tốc độ 1.5x ➔ Âm thanh đọc nhanh hơn rõ rệt.
* [ ] Hẹn giờ 1 phút ➔ Đúng 1 phút sau audio tự động dừng.
* [ ] Xóa file audio đã tải trong màn hình Quản lý ➔ File trong bộ nhớ máy bị xóa, dung lượng giảm xuống.

---

## 📌 BƯỚC 8: KIỂM THỬ XỬ LÝ LỖI (ERROR HANDLING) & BẢO TRÌ

### 🎯 Mục tiêu:
Đảm bảo tính ổn định tối đa khi xảy ra sự cố mất mạng, lỗi API hoặc link truyện bị thay đổi.

### 📝 Nhiệm vụ cho Claude:
1. Xử lý khi mất kết nối Internet (đối với nội dung chưa tải offline): Hiển thị thông báo "Không có kết nối mạng", tự động thử lại khi có mạng trở lại.
2. Xử lý khi API Gemini/TTS bị quá tải hoặc hết Quota: Tự động fallback chuyển sang API dự phòng (Edge-TTS hoặc Native Device Speech).
3. Xử lý khi link web truyện bị thay đổi cấu trúc/lỗi 404: Báo lỗi thân thiện cho người dùng thay vì làm app bị crash.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):
* [ ] Tắt Wifi/4G khi đang nghe chương chưa tải offline ➔ App hiển thị thông báo lỗi êm ái, không crash.
* [ ] Nhập 1 URL lỗi/không tồn tại ➔ App hiển thị thông báo lỗi đúng quy định.