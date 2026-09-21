package com.dinh.gocnho.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.Locale

@IgnoreExtraProperties
data class Story(
    // Tự động map Document ID từ Firestore
    @DocumentId
    val id: String = "",

    // Bắt buộc (Non-nullable) - luôn có giá trị mặc định để Firestore parse an toàn
    val title: String = "",

    // Không bắt buộc (Nullable)
    val author: String? = null,
    val authorLink: String? = null,
    val source: String? = null,
    val sourceLink: String? = null,
    val createdAt: Timestamp? = null,
    val createdBy: String? = null,
    val updatedAt: Timestamp? = null
) {
    /**
     * Helper lấy URL source của truyện (từ sourceLink hoặc source)
     */
    val getSourceUrl: String?
        get() {
            val raw = sourceLink?.ifBlank { null } ?: source?.ifBlank { null } ?: return null
            val trimmed = raw.trim()
            return if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                (trimmed.contains(".") && !trimmed.contains(" "))
            ) {
                trimmed
            } else {
                null
            }
        }

    /**
     * Helper định dạng ngày giờ hiển thị lên UI (dành cho updatedAt hoặc createdAt)
     */
    fun getFormattedTime(): String {
        val targetTimestamp = updatedAt ?: createdAt ?: return "Không rõ thời gian"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(targetTimestamp.toDate())
    }
}
