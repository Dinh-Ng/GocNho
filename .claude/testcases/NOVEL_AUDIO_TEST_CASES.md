# 🧪 BẢNG TEST CASES CHI TIẾT: TÍNH NĂNG NOVEL AUDIO & CHẠY NGẦM

> **Tài liệu tham chiếu:** [NOVEL_AUDIO_FEATURE_ROADMAP.md](file:///Users/dinh/AndroidStudioProjects/GcNh/.claude/roadmap/NOVEL_AUDIO_FEATURE_ROADMAP.md)  
> **Quy trình làm việc:** Viết Test Case -> Code từng bước -> Kiểm thử theo Test Case ID tương ứng.

---

## 📋 MỤC LỤC THEO MÔ-ĐUN / ROADMAP

- [Mô-đun 1: Data Models & Cấu Trúc Lưu Trữ (Bước 1)](#-mô-đun-1-data-models--cấu-trúc-lưu-trữ-bước-1)
- [Mô-đun 2: Web Novel Content Parser (Bước 2)](#-mô-đun-2-web-novel-content-parser-bước-2)
- [Mô-đun 3: AI Text-to-Speech Engine & Chunking (Bước 3)](#-mô-đun-3-ai-text-to-speech-engine--chunking-bước-3)
- [Mô-đun 4: Background Audio Player Service (Bước 4)](#-mô-đun-4-background-audio-player-service-bước-4)
- [Mô-đun 5: Audio Player UI/UX & Flow (Bước 5)](#-mô-đun-5-audio-player-uiux--flow-bước-5)
- [Mô-đun 6: Quản Lý Tải Offline - Chữ & Audio (Bước 6)](#-mô-đun-6-quản-lý-tải-offline---chữ--audio-bước-6)
- [Mô-đun 7: Tính Năng Nâng Cao (Sleep Timer, Speed, Cache) (Bước 7)](#-mô-đun-7-tính-năng-nâng-cao-sleep-timer-speed-cache-bước-7)
- [Mô-đun 8: Kiểm Thử Xử Lý Lỗi & Fallback (Bước 8)](#-mô-đun-8-kiểm-thử-xử-lý-lỗi--fallback-bước-8)

---

## 📌 MÔ-ĐUN 1: DATA MODELS & CẤU TRÚC LƯU TRỮ (Bước 1)

| ID | Phân loại | Tên Test Case | Các bước thực hiện (Steps) | Kết quả mong đợi (Expected Result) |
| :--- | :--- | :--- | :--- | :--- |
| **TC-MOD1-01** | Happy Path | Tạo mới và lưu trữ thực thể Novel & Chapter | 1. Khởi chạy ứng dụng hoặc chạy unit test tạo 1 đối tượng `Novel` và danh sách `Chapter`.<br>2. Lưu vào Database (Room/Local DB).<br>3. Truy vấn lại từ DB. | Dữ liệu được ghi và đọc thành công, không mất mát trường thông tin (`title`, `sourceUrl`, `totalChapters`...). |
| **TC-MOD1-02** | Happy Path | Lưu và phục hồi tiến độ nghe/đọc (Progress Saver) | 1. Lưu trạng thái `AudioPlayerState` (ví dụ: `currentChapterId = 2`, `currentChunkIndex = 5`).<br>2. Đóng ứng dụng hoàn toàn (Kill app) rồi mở lại.<br>3. Đọc dữ liệu tiến độ đã lưu. | App đọc đúng vị trí chương và vị trí chunk đã lưu trước đó. |
| **TC-MOD1-03** | Regression | Kiểm tra tương thích hồi quy ứng dụng hiện tại | 1. Biên dịch và khởi chạy app trên thiết bị/máy ảo.<br>2. Điều hướng vào các màn hình hiện có của app (như Tetris, Home...). | App build thành công (không lỗi compile), các màn hình và chức năng cũ vẫn hoạt động trơn tru. |

---

## 📌 MÔ-ĐUN 2: WEB NOVEL CONTENT PARSER (Bước 2)

| ID | Phân loại | Tên Test Case | Các bước thực hiện (Steps) | Kết quả mong đợi (Expected Result) |
| :--- | :--- | :--- | :--- | :--- |
| **TC-MOD2-01** | Happy Path | Parse nội dung chương từ truyenfull | 1. Nhập URL hợp lệ 1 chương từ `truyenfull.vn`.<br>2. Nhấn nút "Parse/Trích xuất". | Bóc tách chính xác: **Tên truyện**, **Tên chương**, **Nội dung chữ**, và **Link chương kế tiếp** (`nextChapterUrl`). |
| **TC-MOD2-02** | Happy Path | Parse nội dung chương từ bachngocsach | 1. Nhập URL hợp lệ 1 chương từ `bachngocsach.com.vn`.<br>2. Nhấn nút "Parse/Trích xuất". | Trích xuất đúng tên chương, nội dung chữ và URL chương kế tiếp tương ứng với selector của bachngocsach. |
| **TC-MOD2-03** | Happy Path | Làm sạch văn bản (Sanitize Text) | 1. Parse một chương truyện có chứa quảng cáo xen kẽ (ví dụ: "Đọc truyện tại...", các banner text, link giới thiệu).<br>2. Kiểm tra chuỗi `sanitizedContent`. | Các đoạn quảng cáo, ký tự rác được lọc sạch; dấu ngắt câu, ký tự đặc biệt được chuẩn hóa mượt mà cho bộ đọc TTS. |
| **TC-MOD2-04** | Edge Case | Parse chương cuối cùng của truyện | 1. Nhập URL của chương cuối cùng trong một bộ truyện.<br>2. Nhấn "Parse/Trích xuất". | Bóc tách thành công nội dung chương; trường `nextChapterUrl` trả về `null` hoặc cờ nhận biết đã hết truyện mà không gây crash app. |
| **TC-MOD2-05** | Error Handling | Nhập URL không hợp lệ hoặc link lỗi 404 | 1. Nhập link hỏng (ví dụ: link 404 hoặc chuỗi text không phải URL hợp lệ).<br>2. Nhấn "Parse/Trích xuất". | App hiển thị thông báo lỗi thân thiện (ví dụ: "Không tìm thấy nội dung hoặc link lỗi"), không bị crash đột ngột. |
| **TC-MOD2-06** | Error Handling | Nhập URL từ domain chưa hỗ trợ | 1. Nhập URL từ một trang web lạ chưa cấu hình trong `ParserConfig`.<br>2. Nhấn "Parse/Trích xuất". | Hiển thị thông báo: "Trang web này chưa được hỗ trợ bóc tách tự động". |

---

## 📌 MÔ-ĐUN 3: AI TEXT-TO-SPEECH ENGINE & CHUNKING (Bước 3)

| ID | Phân loại | Tên Test Case | Các bước thực hiện (Steps) | Kết quả mong đợi (Expected Result) |
| :--- | :--- | :--- | :--- | :--- |
| **TC-MOD3-01** | Happy Path | Chia nhỏ văn bản thành các chunks thông minh | 1. Đưa vào nội dung 1 chương dài (~3000 từ).<br>2. Chạy thuật toán chia đoạn (`chunking`). | Văn bản được chia thành các đoạn 200 - 400 từ; các vết cắt luôn nằm ở cuối câu (dấu chấm, chấm lửng, xuống dòng), không bị đứt đoạn giữa từ hoặc giữa câu. |
| **TC-MOD3-02** | Happy Path | Chuyển đổi TTS và phát âm thanh chunk đầu tiên | 1. Chọn nội dung đã trích xuất, nhấn "Tạo & Phát Audio".<br>2. Đợi phản hồi từ API TTS. | App nhận được luồng/file âm thanh (.mp3) và phát ra loa rõ ràng, giọng đọc tiếng Việt chuẩn xác. |
| **TC-MOD3-03** | Happy Path | Cơ chế Pre-buffering gối đầu giữa các chunks | 1. Bắt đầu phát `chunk[0]`.<br>2. Quan sát log / trạng thái tải nền.<br>3. Để âm thanh phát hết `chunk[0]`. | `chunk[1]` được tự động tải về ngầm trước khi `chunk[0]` kết thúc. Khi hết `chunk[0]`, âm thanh tự nối tiếp sang `chunk[1]` trơn tru, không có khoảng lặng khựng giật. |
| **TC-MOD3-04** | Happy Path | Quản lý Cache tạm các file audio | 1. Nghe qua đoạn `chunk[0]`.<br>2. Bấm phát lại đoạn `chunk[0]`. | App phát ngay từ file cache trong bộ nhớ tạm mà không gọi lại API TTS. |
| **TC-MOD3-05** | Edge Case | Đọc văn bản chứa nhiều từ Hán Việt, số và hội thoại | 1. Nhập đoạn văn có nhiều số thập phân, đối thoại ngoặc kép và từ vựng đặc thù kiếm hiệp.<br>2. Tạo audio và lắng nghe. | AI TTS đọc trôi chảy, không bị nuốt chữ hoặc phát âm sai lệch nghiêm trọng. |
| **TC-MOD3-06** | Error Handling | Xử lý khi API TTS quá tải / Timeout | 1. Giả lập mất mạng hoặc API TTS trả về mã lỗi 429/500/timeout.<br>2. Bấm phát audio. | App hiển thị thông báo "Không thể tạo âm thanh lúc này, đang thử lại..." và có cơ chế retry thay vì dừng văng app. |

---

## 📌 MÔ-ĐUN 4: BACKGROUND AUDIO PLAYER SERVICE (Bước 4)

| ID | Phân loại | Tên Test Case | Các bước thực hiện (Steps) | Kết quả mong đợi (Expected Result) |
| :--- | :--- | :--- | :--- | :--- |
| **TC-MOD4-01** | Happy Path | Phát audio khi thoát ra Home Screen | 1. Bấm phát một chương audio.<br>2. Nhấn nút Home để đưa ứng dụng về chạy nền. | Audio vẫn tiếp tục phát liên tục, không bị gián đoạn hay bị hệ điều hành tắt. |
| **TC-MOD4-02** | Happy Path | Phát audio khi khóa màn hình (Lockscreen) | 1. Đang phát audio, bấm nút nguồn khóa màn hình thiết bị.<br>2. Màn hình tắt hoàn toàn. | Âm thanh vẫn tiếp tục phát bình thường. |
| **TC-MOD4-03** | Happy Path | Điều khiển trên Notification & Màn hình khóa | 1. Khóa màn hình hoặc kéo thanh thông báo (Notification Bar).<br>2. Thao tác lần lượt: Bấm Pause -> Bấm Play -> Bấm Next Chapter. | Trình phát phản hồi chính xác: dừng khi Pause, tiếp tục khi Play, chuyển đúng chương khi Next. Notification hiển thị đúng Tên truyện, Tên chương. |
| **TC-MOD4-04** | Edge Case | Xử lý khi ngắt kết nối tai nghe (Audio Becoming Noisy) | 1. Cắm tai nghe (dây hoặc bluetooth) và đang phát truyện.<br>2. Rút tai nghe hoặc tắt bluetooth. | Trình phát tự động **Pause** ngay lập tức, không phát âm thanh ầm ĩ ra loa ngoài. |
| **TC-MOD4-05** | Edge Case | Xử lý gián đoạn khi có cuộc gọi đến (Audio Focus Loss) | 1. Đang nghe truyện, thực hiện cuộc gọi đến thiết bị.<br>2. Nhấc máy đàm thoại.<br>3. Kết thúc cuộc gọi. | Audio tự động Pause khi chuông reo/bắt máy. Sau khi kết thúc cuộc gọi, audio tự động resume tiếp tục đọc (hoặc giữ ở trạng thái pause chờ người dùng bấm play tùy cấu hình focus). |
| **TC-MOD4-06** | Edge Case | Ứng dụng khác phát âm thanh chiếm Audio Focus | 1. Đang nghe audio truyện, mở YouTube hoặc Spotify lên phát nhạc. | Audio truyện tự động dừng lại nhường kênh âm thanh cho ứng dụng mới. |

---

## 📌 MÔ-ĐUN 5: AUDIO PLAYER UI/UX & FLOW (Bước 5)

| ID | Phân loại | Tên Test Case | Các bước thực hiện (Steps) | Kết quả mong đợi (Expected Result) |
| :--- | :--- | :--- | :--- | :--- |
| **TC-MOD5-01** | Happy Path | Màn hình Full Player hiển thị đầy đủ thông tin | 1. Mở màn hình Trình phát Audio chi tiết (Full Player Screen). | Hiển thị chuẩn đẹp: Ảnh bìa (Artwork), Tên truyện, Tên chương, Thanh trượt Seekbar thời gian, cụm nút Play/Pause/Next/Prev/Tốc độ. |
| **TC-MOD5-02** | Happy Path | Kéo tua thanh thời lượng (Seekbar) | 1. Kéo thanh trượt đến một mốc thời gian khác trong chương/chunk.<br>2. Thả tay ra. | Âm thanh nhảy tới đúng vị trí được chọn và tiếp tục phát mượt mà. |
| **TC-MOD5-03** | Happy Path | Thu nhỏ thành Mini Player ghim đáy màn hình | 1. Đang mở Full Player, vuốt xuống hoặc bấm nút back/thu nhỏ. | Xuất hiện Mini Player thanh ngang ghim ở đáy màn hình, chứa: Tên chương, nút Play/Pause, nút đóng/mở lại Full Player. |
| **TC-MOD5-04** | Happy Path | Duyệt các màn hình khác trong khi Mini Player vẫn phát | 1. Mini Player đang phát.<br>2. Chuyển sang màn hình khác trong app (danh sách truyện, cài đặt, v.v.). | Mini Player vẫn duy trì cố định dưới đáy, âm thanh tiếp tục chạy, không bị reload hay giật lag. |
| **TC-MOD5-05** | Happy Path | Tự động chuyển chương (Auto Next Chapter) | 1. Nghe chương truyện đến đoạn chunk cuối cùng.<br>2. Chờ kết thúc chương. | App tự động gọi hàm lấy nội dung chương kế tiếp, cập nhật lại tiêu đề và tiếp tục đọc chương mới mà không cần thao tác tay. |
| **TC-MOD5-06** | Edge Case | Đọc hết chương cuối cùng của bộ truyện | 1. Nghe đến hết chunk cuối của chương cuối cùng (không còn `nextChapterUrl`). | Trình phát dừng lại êm ái, chuyển nút sang Play, hiển thị thông báo "Đã hoàn thành bộ truyện". |

---

## 📌 MÔ-ĐUN 6: QUẢN LÝ TẢI OFFLINE - CHỮ & AUDIO (Bước 6)

| ID | Phân loại | Tên Test Case | Các bước thực hiện (Steps) | Kết quả mong đợi (Expected Result) |
| :--- | :--- | :--- | :--- | :--- |
| **TC-MOD6-01** | Happy Path | Tải toàn bộ truyện chữ (Batch Download Text) | 1. Tại màn hình thông tin truyện, nhấn **"Tải toàn bộ truyện"**.<br>2. Quan sát tiến trình tải. | Thanh tiến trình cập nhật đều đặn (e.g. "Đã tải 15/100 chương"). Toàn bộ text được lưu vào DB nội bộ. |
| **TC-MOD6-02** | Happy Path | Đọc truyện chữ khi không có kết nối mạng | 1. Bật chế độ máy bay (Airplane Mode) / Tắt Wifi & 4G.<br>2. Mở các chương truyện đã tải ở TC-MOD6-01. | Nội dung chữ hiển thị đầy đủ, ngay lập tức mà không gặp bất kỳ lỗi kết nối nào. |
| **TC-MOD6-03** | Happy Path | Tải file Audio hoàn chỉnh của một chương tùy chọn | 1. Kết nối mạng, chọn 1 chương và bấm nút **"Tải Audio"**.<br>2. Chờ tiến trình tổng hợp hoàn tất. | Hệ thống chuyển đổi các chunk và nối thành 1 file MP3 lưu vào bộ nhớ trong máy (`localAudioPath`). Biểu tượng chương chuyển sang trạng thái "Đã tải audio". |
| **TC-MOD6-04** | Happy Path | Phát Audio chương đã tải trong trạng thái Offline | 1. Tắt toàn bộ Wifi/4G.<br>2. Bấm phát chương đã tải audio ở TC-MOD6-03. | Trình phát tự động nhận diện file local và phát ngay lập tức, không tiêu tốn lưu lượng mạng, không gọi API. |
| **TC-MOD6-05** | Edge Case | Tạm dừng & Tiếp tục tải khi mất mạng giữa chừng | 1. Đang tải toàn bộ truyện (chương 20/100), tắt mạng đột ngột.<br>2. Bật mạng lại và bấm "Tiếp tục tải". | Quá trình tải tiếp tục từ chương 21, không tải lại từ đầu các chương đã hoàn thành trước đó. |
| **TC-MOD6-06** | Error Handling | Thiết bị không đủ bộ nhớ trống | 1. Giả lập thiết bị đầy bộ nhớ.<br>2. Bấm tải audio hoặc tải toàn bộ truyện. | App thông báo rõ ràng "Bộ nhớ thiết bị không đủ để tải" và tự hủy tác vụ an toàn. |

---

## 📌 MÔ-ĐUN 7: TÍNH NĂNG NÂNG CAO (SLEEP TIMER, SPEED, CACHE) (Bước 7)

| ID | Phân loại | Tên Test Case | Các bước thực hiện (Steps) | Kết quả mong đợi (Expected Result) |
| :--- | :--- | :--- | :--- | :--- |
| **TC-MOD7-01** | Happy Path | Hẹn giờ tắt theo thời gian (1 phút / 15 phút / 30 phút) | 1. Đang phát audio, chọn Hẹn giờ tắt (ví dụ đặt 1 phút để test).<br>2. Chờ đồng hồ đếm ngược về 0. | Đúng 1 phút, âm thanh tự động nhỏ dần (fade out) và dừng hẳn; Service dừng chạy ngầm. |
| **TC-MOD7-02** | Happy Path | Hẹn giờ tắt: "Hết chương hiện tại" | 1. Đang nghe giữa chương, chọn chế độ hẹn giờ "Hết chương hiện tại".<br>2. Để âm thanh phát đến cuối chương. | Khi kết thúc chương, audio tự động ngưng, không tự động nhảy sang chương tiếp theo. |
| **TC-MOD7-03** | Happy Path | Điều chỉnh tốc độ đọc (Playback Speed) | 1. Thay đổi tốc độ phát lần lượt sang: 0.75x -> 1.0x -> 1.25x -> 1.5x -> 2.0x.<br>2. Lắng nghe âm thanh. | Tốc độ đọc thay đổi rõ rệt tương ứng với mức đã chọn, cao độ giọng nói (pitch) được giữ tự nhiên, không bị méo tiếng quá mức. |
| **TC-MOD7-04** | Happy Path | Màn hình quản lý bộ nhớ & Xóa file audio đã tải | 1. Mở màn hình Cài đặt / Quản lý dung lượng truyện.<br>2. Kiểm tra danh sách file đã tải và tổng dung lượng chiếm dụng.<br>3. Chọn xóa 1 chương hoặc chọn "Xóa toàn bộ cache". | File MP3 bị xóa khỏi bộ nhớ máy, dung lượng báo giảm tương ứng, trạng thái chương chuyển về "Chưa tải". |
| **TC-MOD7-05** | Edge Case | Hướng dẫn cấp quyền bỏ tối ưu pin (Battery Optimization) | 1. Vào mục Hướng dẫn / Thiết lập chạy ngầm của app.<br>2. Nhấn nút yêu cầu "Tắt tối ưu hóa pin". | Ứng dụng mở đúng màn hình cài đặt hệ thống của Android hoặc hiển thị dialog xin quyền miễn tối ưu pin để không bị kill ngầm. |

---

## 📌 MÔ-ĐUN 8: KIỂM THỬ XỬ LÝ LỖI & FALLBACK (Bước 8)

| ID | Phân loại | Tên Test Case | Các bước thực hiện (Steps) | Kết quả mong đợi (Expected Result) |
| :--- | :--- | :--- | :--- | :--- |
| **TC-MOD8-01** | Error Handling | Mất mạng khi đang nghe chương chưa tải offline | 1. Đang phát chương online (chưa tải offline), tắt kết nối mạng.<br>2. Chờ phát hết chunk hiện tại trong buffer. | App hiển thị thông báo "Mất kết nối mạng, vui lòng kiểm tra lại", giữ nguyên vị trí đang đọc dở và tự động khôi phục khi có mạng trở lại. |
| **TC-MOD8-02** | Error Handling | API TTS chính hết Quota / Fallback tự động | 1. Giả lập API Gemini TTS trả về lỗi Quota Exceeded (429).<br>2. Nhấn tiếp tục nghe. | App tự động chuyển sang giải pháp Fallback (Edge-TTS hoặc TextToSpeech có sẵn trên thiết bị Android) để tiếp tục phát mà không ngắt quãng trải nghiệm. |
| **TC-MOD8-03** | Error Handling | Website truyện thay đổi cấu trúc trang / HTML bị lỗi | 1. Parse link một trang web bị thay đổi cấu trúc class/id hoặc trang đang bảo trì. | App bắt ngoại lệ (Exception Handling), hiển thị thông báo "Không thể phân tích trang truyện này" mà không bị crash app (ANR/Force Close). |

---

## 🔄 QUY TRÌNH PHỐI HỢP & MAPPING KIỂM THỬ

Mỗi khi hoàn thành code cho một bước, danh sách Test Case tương ứng dưới đây sẽ được gọi ra để tiến hành nghiệm thu:

- 🏁 **Hoàn thành Bước 1** -> Chạy kiểm thử: `TC-MOD1-01`, `TC-MOD1-02`, `TC-MOD1-03`.
- 🏁 **Hoàn thành Bước 2** -> Chạy kiểm thử: `TC-MOD2-01`, `TC-MOD2-02`, `TC-MOD2-03`, `TC-MOD2-04`, `TC-MOD2-05`, `TC-MOD2-06`.
- 🏁 **Hoàn thành Bước 3** -> Chạy kiểm thử: `TC-MOD3-01`, `TC-MOD3-02`, `TC-MOD3-03`, `TC-MOD3-04`, `TC-MOD3-05`, `TC-MOD3-06`.
- 🏁 **Hoàn thành Bước 4** -> Chạy kiểm thử: `TC-MOD4-01`, `TC-MOD4-02`, `TC-MOD4-03`, `TC-MOD4-04`, `TC-MOD4-05`, `TC-MOD4-06`.
- 🏁 **Hoàn thành Bước 5** -> Chạy kiểm thử: `TC-MOD5-01`, `TC-MOD5-02`, `TC-MOD5-03`, `TC-MOD5-04`, `TC-MOD5-05`, `TC-MOD5-06`.
- 🏁 **Hoàn thành Bước 6** -> Chạy kiểm thử: `TC-MOD6-01`, `TC-MOD6-02`, `TC-MOD6-03`, `TC-MOD6-04`, `TC-MOD6-05`, `TC-MOD6-06`.
- 🏁 **Hoàn thành Bước 7** -> Chạy kiểm thử: `TC-MOD7-01`, `TC-MOD7-02`, `TC-MOD7-03`, `TC-MOD7-04`, `TC-MOD7-05`.
- 🏁 **Hoàn thành Bước 8** -> Chạy kiểm thử: `TC-MOD8-01`, `TC-MOD8-02`, `TC-MOD8-03`.
