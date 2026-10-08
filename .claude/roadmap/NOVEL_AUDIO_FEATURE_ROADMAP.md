# 🗺️ LỘ TRÌNH PHÁT TRIỂN TÍNH NĂNG: CHUYỂN TRUYỆN CHỮ THÀNH AUDIO & CHẠY NGẦM

> **Ghi chú dành cho Claude AI:**
>
> * Đây là **tính năng mới bổ sung vào một ứng dụng đã có sẵn**.
>
> * Trước khi triển khai bất kỳ bước nào, hãy đọc kỹ cấu trúc thư mục hiện tại của dự án, tuân thủ các quy chuẩn về đặt tên, quản lý trạng thái (state management), và thành phần giao diện (UI components) đang được áp dụng trong codebase này.
>
> * Yêu cầu thực hiện **đúng và duy nhất bước được người dùng chỉ định**. Sau khi hoàn thành một bước, hãy dừng lại để người dùng tự kiểm thử (test) trước khi đi tiếp.

## 📌 BƯỚC 1: ĐỊNH NGHĨA DATA MODELS VÀ CẤU TRÚC LƯU TRỮ

### 🎯 Mục tiêu:

Thiết lập các kiểu dữ liệu (Types/Interfaces) và Schema lưu trữ thông tin về Truyện, Chương, Trạng thái Player, Cấu hình Parser, Trạng thái Tải Offline, Trạng thái Đọc/Nghe và Quản lý Gemini API Key.

### 📝 Nhiệm vụ cho Claude:

1. Tạo file chứa các Type/Interface cơ bản cho tính năng mới:

   * `Novel`: `id`, `title`, `author`, `sourceUrl`, `coverUrl`, `currentChapterId`, `totalChapters`, `isFullTextDownloaded`, `downloadProgress`.

   * `DownloadStatus`: Enum hoặc Union Type (`'IDLE'` | `'DOWNLOADING'` | `'DOWNLOADED'` | `'FAILED'`).

   * `ListenStatus`: Enum hoặc Union Type (`'UNLISTENED'` | `'LISTENING'` | `'LISTENED'`).

   * `Chapter`: `id`, `novelId`, `chapterIndex`, `title`, `sourceUrl`, `rawContent`, `sanitizedContent`, `downloadStatus`, `downloadProgress` (0-100), `listenStatus`, `lastListenedPosition` (giây), `localAudioPath`, `audioFileSize`.

   * `ApiKeyInfo`: `id`, `key`, `label` (ví dụ: "Key 1 - Main", "Key 2 - Backup"), `status` (`'ACTIVE'` | `'QUOTA_EXCEEDED'` | `'INVALID'`), `lastUsedAt`, `exceededAt` (timestamp thời điểm hết quota).

   * `AudioPlayerState`: `isPlaying`, `currentChapterId`, `currentChunkIndex`, `playbackSpeed`, `sleepTimerMinutes`, `remainingSleepTime`, `isOfflineMode`, `activeApiKeyId`.

   * `ParserConfig`: `domain`, `titleSelector`, `contentSelector`, `nextChapterSelector`.

2. Khởi tạo service/helper để lưu trữ trạng thái nghe (Progress Saver), danh sách API Key mã hóa an toàn và dữ liệu tải về vào Local Storage / DB hiện tại của app (AsyncStorage, Room, Realm, MMKV, Hive, SQLite...).

### 🧪 Hướng dẫn Kiểm thử (User Checklist):

* \[ \] Code không bị lỗi biên dịch (Compile Error).

* \[ \] Mở app hiện tại, các tính năng cũ vẫn hoạt động bình thường mà không bị ảnh hưởng.

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

* \[ \] Dán link 1 chương từ `truyenfull` hoặc `bachngocsach` vào app.

* \[ \] Kiểm tra xem app có bóc tách được đúng **Tên chương**, **Nội dung chữ sạch** (không dính quảng cáo) và **Link chương tiếp theo** hay không.

## 📌 BƯỚC 3: AI TTS ENGINE & QUẢN LÝ DÂN API KEY (AUTO ROTATION)

### 🎯 Mục tiêu:

Chuyển đổi văn bản chữ thành file/luồng âm thanh (Audio) thông qua AI Gemini API với cơ chế xoay vòng Key tự động khi gặp giới hạn Quota (429 Rate Limit) và xử lý phát gối đầu (Pre-buffering).

### 📝 Nhiệm vụ for Claude:

1. Tạo `ApiKeyManagerService`:

   * Quản lý danh sách các `ApiKeyInfo`.

   * Hàm `getActiveKey()`: Lấy ra Key khả dụng đầu tiên có trạng thái `'ACTIVE'`.

   * Hàm `markKeyQuotaExceeded(keyId)`: Khi API trả về lỗi 429/Resource Exhausted, tự động đổi trạng thái Key thành `'QUOTA_EXCEEDED'`, ghi lại `exceededAt` và tự động tìm Key tiếp theo để retry ngay lập tức.

   * Hàm `autoResetQuota()`: Tự động chuyển các Key bị `'QUOTA_EXCEEDED'` trở lại trạng thái `'ACTIVE'` sau thời gian cooldown quy định (ví dụ: 1 giờ hoặc sau 24h).

2. Tạo `TTSService`:

   * Tích hợp Gemini API (Audio output / TTS) với việc inject `ApiKeyManagerService`.

   * Xử lý chia văn bản thành từng đoạn nhỏ (`chunks`) từ 200 - 400 từ để tối ưu thời gian phản hồi của AI.

   * Khi gửi request đến Gemini bị lỗi Quota ➔ Tự động bắt exception, gọi `markKeyQuotaExceeded()`, đổi sang Key mới và thực hiện lại request mà không ngắt dòng phát âm thanh của người dùng.

3. Thiết lập cơ chế **Queue & Pre-buffering**:

   * Khi đoạn `chunk[i]` đang phát, tự động gọi API chuyển đổi sẵn đoạn `chunk[i+1]` ở background.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):

* \[ \] Nhập 2 API Key (với Key 1 cố tình chọn key đã hết quota hoặc rác để giả lập lỗi 429).

* \[ \] Bấm nút "Tạo Audio" ➔ App phát hiện Key 1 lỗi/hết Quota, tự động xoay sang Key 2 và phát âm thanh bình thường.

* \[ \] Âm thanh phát hết đoạn 1 tự động chuyển sang đoạn 2 mượt mà, không bị dừng lâu.

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

* \[ \] Bấm phát audio, bấm nút Home thoát ra ngoài màn hình chính ➔ Audio vẫn tiếp tục chạy.

* \[ \] Tắt/Khóa màn hình điện thoại ➔ Audio vẫn phát, màn hình khóa hiển thị widget trình phát audio.

* \[ \] Bấm Play/Pause trên màn hình khóa hoặc Notification ➔ Trạng thái audio phản hồi đúng.

## 📌 BƯỚC 5: XÂY DỰNG GIAO DIỆN NGUYÊN BẢN (UI/UX TRÌNH PHÁT AUDIO)

### 🎯 Mục tiêu:

Gọt giũa giao diện người dùng, tích hợp trình phát audio hoàn chỉnh vào dòng chảy chính của ứng dụng.

### 📝 Nhiệm vụ cho Claude:

1. Thiết kế Giao diện Trình phát Audio (Audio Player Screen / Bottom Sheet Player):

   * Màn hình Player đầy đủ: Artwork/Ảnh bìa truyện, Tên chương, Slider thời lượng, Nút Play/Pause, Next/Prev, Nút chỉnh Tốc độ đọc.

   * Thanh Player nhỏ (Mini Player Widget) ghim ở đáy màn hình giúp người dùng duyệt các trang khác trong app vẫn điều khiển được nhạc.

2. Thêm tính năng **Tự động chuyển chương (Auto Next Chapter)** và **Cập nhật trạng thái "Đã nghe"**:

   * Khi phát đến cuối đoạn chunk cuối cùng của chương hiện tại ➔ Tự động đánh dấu `listenStatus = 'LISTENED'` cho chương vừa nghe xong, sau đó tự động gọi Parser lấy dữ liệu chương mới và tiếp tục phát.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):

* \[ \] Mở app, mở trình phát audio ➔ Giao diện hiển thị chuẩn đẹp, đồng bộ với phong cách thiết kế hiện tại của app.

* \[ \] Thu nhỏ Player thành Mini Player ➔ Duyệt chuyển giữa các màn hình trong app không bị mất nhạc.

* \[ \] Nghe hết 1 chương ➔ App tự động đánh dấu "Đã nghe", chuyển sang chương tiếp theo và tiếp tục đọc.

## 📌 BƯỚC 6: QUẢN LÝ TẢI TRUYỆN CHỮ VÀ TẢI AUDIO OFFLINE

### 🎯 Mục tiêu:

Cho phép người dùng tải toàn bộ nội dung chữ của một bộ truyện và tải file Audio của từng chương tùy chọn với việc quản lý tiến trình tải real-time.

### 📝 Nhiệm vụ cho Claude:

1. **Tính năng Tải toàn bộ Truyện chữ (Full Novel Text Batch Download):**

   * Thêm nút **"Tải toàn bộ truyện"** ở màn hình Chi tiết Truyện (Novel Detail).

   * Xây dựng tiến trình tải ngầm (`BatchDownloadService`): Lần lượt gọi `NovelParserService` lấy nội dung từ chương 1 đến chương cuối, lưu trực tiếp vào cơ sở dữ liệu local (SQLite/Realm/Hive...).

   * Hiển thị thanh tiến trình (Progress Bar: e.g., *"Đã tải 45/120 chương"*), hỗ trợ tạm dừng / tiếp tục tải.

2. **Tính năng Tải File Audio của Chap Tùy chọn (Offline Audio Download & Status Tracking):**

   * Thêm nút/icon **"Tải Audio"** bên cạnh danh sách chương và trên màn hình Player.

   * Khi bắt đầu tải ➔ Chuyển `downloadStatus` của chương thành `'DOWNLOADING'`, cập nhật `% downloadProgress` liên tục.

   * Gọi `TTSService` (sử dụng luồng xoay vòng Key ở Bước 3) tổng hợp các đoạn audio chunk thành 1 file MP3 hoàn chỉnh, lưu vào thư mục đệm (`localAudioPath`).

   * Sau khi hoàn tất ➔ Chuyển `downloadStatus` thành `'DOWNLOADED'`. Nếu có lỗi ➔ Chuyển thành `'FAILED'` và cho phép bấm tải lại (Retry).

   * **Cơ chế Ưu tiên phát Offline:** Khi phát chương có `downloadStatus == 'DOWNLOADED'`, Player tự động phát trực tiếp từ file MP3 local không gọi API.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):

* \[ \] Bấm nút "Tải toàn bộ truyện" ➔ Tắt Wifi/4G và mở từng chương ra vẫn hiển thị đầy đủ nội dung chữ.

* \[ \] Chọn 1 chương bấm "Tải Audio" ➔ Nút tải đổi sang trạng thái đang tải kèm tiến trình (%), sau đó đổi thành trạng thái "Đã tải".

* \[ \] Tắt Wifi/4G, bấm phát Audio chương đã tải ➔ Trình phát chạy file local bình thường không báo lỗi mạng.

## 📌 BƯỚC 7: MÀN HÌNH QUẢN LÝ AUDIO, API KEY & TÍNH NĂNG NÂNG CAO

### 🎯 Mục tiêu:

Tạo màn hình quản lý tập trung các file audio đã tải với các bộ lọc trạng thái, giao diện quản lý Gemini API Key đa luồng và các công cụ tùy chỉnh phát âm thanh.

### 📝 Nhiệm vụ cho Claude:

1. **Giao diện Quản lý Gemini API Keys (API Key Manager Screen / Section):**

   * Form thêm Key mới: Cho phép dán văn bản (hỗ trợ nhập 1 hoặc dán nhiều Key cách nhau bằng dấu phẩy/dòng mới), đặt nhãn gợi nhớ.

   * Danh sách API Keys đã thêm kèm Badge trạng thái trực quan:

     * 🟢 **Hoạt động (Active):** Key sẵn sàng sử dụng.

     * 🔴 **Hết Quota (Quota Exceeded):** Đang bị giới hạn rate limit, hiển thị đếm ngược thời gian chờ cooldown reset.

     * ⚠️ **Lỗi (Invalid):** Key sai hoặc bị hỏng.

   * Các nút thao tác: Nút "Kiểm tra Key" (Test Key), Nút "Đặt lại trạng thái Quota", Nút "Xóa Key".

2. **Xây dựng Màn hình "Quản lý Audio Tải về" (Downloaded Audio Manager Screen):**

   * **Tabs / Bộ lọc lọc theo Trạng thái:**

     * ⏳ **Đang tải (Downloading):** Hiển thị danh sách file đang trong tiến trình download, kèm thanh % tiến trình, nút "Tạm dừng" hoặc "Hủy tải".

     * ✅ **Đã tải (Downloaded):** Hiển thị danh sách các chương audio đã tải thành công, tổng dung lượng MB chiếm dụng. Mỗi item có nút "Phát ngay" và nút "Xóa file".

     * 🎧 **Đã nghe (Listened):** Hiển thị các chương audio đã phát xong (hoặc đạt >90% thời lượng). Cho phép đánh dấu lại thành "Chưa nghe" hoặc lọc danh sách riêng.

   * **Hành động hàng loạt (Batch Actions):** Cho phép chọn nhiều file để xóa hàng loạt giải phóng bộ nhớ.

3. **Bộ hẹn giờ tắt (Sleep Timer) & Adjust Speed:**

   * Hẹn giờ 15m, 30m, 60m hoặc hết chương.

   * Tốc độ đọc: 0.75x, 1.0x, 1.25x, 1.5x, 2.0x.

4. Hướng dẫn/Cảnh báo cấp quyền "Miễn tối ưu hóa pin" (Disable Battery Optimization) trên Android.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):

* \[ \] Mở phần Quản lý API Key ➔ Thêm thử 3-4 Key, bấm "Kiểm tra Key" xem hệ thống phản hồi trạng thái đúng không.

* \[ \] Mở màn hình Quản lý Audio ➔ Kiểm tra tab **Đang tải** hiển thị đúng file đang download kèm %.

* \[ \] Kiểm tra tab **Đã tải** hiển thị dung lượng file, bấm xóa ➔ File bị xóa và dung lượng bộ nhớ giảm xuống.

* \[ \] Kiểm tra tab **Đã nghe** ➔ Các chương đã phát hết hiển thị đúng nhãn.

* \[ \] Hẹn giờ & chỉnh tốc độ đọc ➔ Phản hồi chuẩn xác.

## 📌 BƯỚC 8: KIỂM THỬ XỬ LÝ LỖI (ERROR HANDLING) & BẢO TRÌ

### 🎯 Mục tiêu:

Đảm bảo tính ổn định tối đa khi xảy ra sự cố mất mạng, tất cả API Key đều hết quota hoặc link truyện bị thay đổi.

### 📝 Nhiệm vụ cho Claude:

1. Xử lý khi **Toàn bộ API Key đều hết Quota**: Thông báo rõ ràng cho người dùng *"Tất cả API Key hiện tại đã đạt giới hạn sử dụng, vui lòng thêm Key mới hoặc đợi hệ thống tự làm mới"*.

2. Xử lý khi mất kết nối Internet (đối với nội dung chưa tải offline): Hiển thị thông báo "Không có kết nối mạng", tự động thử lại khi có mạng trở lại.

3. Xử lý Fallback: Nếu tất cả Gemini Key đều lỗi ➔ Tự động chuyển sang API dự phòng (Edge-TTS hoặc Native Device Speech).

4. Xử lý khi link web truyện bị thay đổi cấu trúc/lỗi 404: Báo lỗi thân thiện cho người dùng thay vì làm app bị crash.

### 🧪 Hướng dẫn Kiểm thử (User Checklist):

* \[ \] Thử tắt Wifi/4G khi đang nghe chương chưa tải offline ➔ App hiển thị thông báo lỗi êm ái, không crash.

* \[ \] Nhập 1 URL lỗi/không tồn tại ➔ App hiển thị thông báo lỗi đúng quy định.