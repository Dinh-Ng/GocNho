package com.dinh.gocnho.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Chapter(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val index: Int = 0,
    val content: String = ""
)
