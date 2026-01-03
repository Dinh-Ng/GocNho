package com.dinh.gocnho

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class ScheduleData(
    val id: Long = System.currentTimeMillis(),
    val location: String, // e.g. "Phòng F407 - Ca 2"
    val date: String,     // e.g. "07/12/2025"
    val subject: String,  // e.g. "Nhập môn CN.. - ITI101"
    
    // New fields for expansion
    val campus: String = "TVB",
    val room: String = "F407",
    val subjectCode: String = "ITI101",
    val time: String = "09:25 - 11:25",
    val clazz: String = "MD21302",
    val teacher: String = "hoangvt16"
)

@Composable
fun ScheduleScreen() {
    // Use a mutable state list for dynamic data
    val scheduleList = remember { mutableStateListOf<ScheduleData>() }
    
    // State for Dialog
    var showDialog by remember { mutableStateOf(false) }
    var currentEditingItem: ScheduleData? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            // "7 ngày tới" Button area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "7 ngày tới", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // List of items
            if (scheduleList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(text = "Chưa có lịch học", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    items(scheduleList) { item ->
                        ScheduleItemCard(
                            item = item,
                            onEdit = {
                                currentEditingItem = item
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                currentEditingItem = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Thêm lịch học")
        }

        // Dialog for Add/Edit
        if (showDialog) {
            ScheduleEntryDialog(
                initialData = currentEditingItem,
                onDismiss = { showDialog = false },
                onSave = { newData ->
                    if (currentEditingItem != null) {
                        // Edit existing
                        val index = scheduleList.indexOfFirst { it.id == currentEditingItem!!.id }
                        if (index != -1) {
                            scheduleList[index] = newData.copy(id = currentEditingItem!!.id)
                        }
                    } else {
                        // Add new
                        scheduleList.add(newData)
                    }
                    showDialog = false
                },
                onDelete = {
                    if (currentEditingItem != null) {
                        scheduleList.removeIf { it.id == currentEditingItem!!.id }
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ScheduleItemCard(item: ScheduleData, onEdit: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Always visible)
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Room/Shift Box with Primary Border
                Box(
                    modifier = Modifier
                        .border(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .width(140.dp)
                ) {
                    Text(
                        text = item.location,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Date and Subject info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.date,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.subject,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Arrow Icon
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                )
            }

            // Expanded Details
            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Left Column
                    Column(modifier = Modifier.weight(1f)) {
                        DetailText("Giảng đường", item.campus)
                        DetailText("Phòng", item.room)
                        DetailText("Mã môn", item.subjectCode)
                        DetailText("Thời gian", item.time)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Column
                    Column(modifier = Modifier.weight(1f)) {
                        DetailText("Lớp", item.clazz)
                        DetailText("Giảng viên", item.teacher)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        // Edit Button inside expanded view
                         Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa", modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sửa", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailText(label: String, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append("$label: ")
            }
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                append(value)
            }
        },
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun ScheduleEntryDialog(
    initialData: ScheduleData?,
    onDismiss: () -> Unit,
    onSave: (ScheduleData) -> Unit,
    onDelete: () -> Unit
) {
    var location by remember { mutableStateOf(initialData?.location ?: "") }
    var date by remember { mutableStateOf(initialData?.date ?: "") }
    var subject by remember { mutableStateOf(initialData?.subject ?: "") }
    
    // New fields
    var campus by remember { mutableStateOf(initialData?.campus ?: "") }
    var room by remember { mutableStateOf(initialData?.room ?: "") }
    var subjectCode by remember { mutableStateOf(initialData?.subjectCode ?: "") }
    var time by remember { mutableStateOf(initialData?.time ?: "") }
    var clazz by remember { mutableStateOf(initialData?.clazz ?: "") }
    var teacher by remember { mutableStateOf(initialData?.teacher ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (initialData == null) "Thêm lịch học" else "Cập nhật lịch học",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Tiêu đề (VD: Phòng F407 - Ca 2)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Ngày (VD: 07/12/2025)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Môn học (VD: Nhập môn... - ITI101)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Additional Fields
                OutlinedTextField(value = campus, onValueChange = { campus = it }, label = { Text("Giảng đường (VD: TVB)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Phòng (VD: F407)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subjectCode, onValueChange = { subjectCode = it }, label = { Text("Mã môn (VD: ITI101)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Thời gian (VD: 09:25 - 11:25)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = clazz, onValueChange = { clazz = it }, label = { Text("Lớp (VD: MD21302)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("Giảng viên (VD: hoangvt16)") }, modifier = Modifier.fillMaxWidth())


                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialData != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Xoá", tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    TextButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = {
                            if (location.isNotBlank()) {
                                onSave(ScheduleData(
                                    location = location,
                                    date = date,
                                    subject = subject,
                                    campus = campus,
                                    room = room,
                                    subjectCode = subjectCode,
                                    time = time,
                                    clazz = clazz,
                                    teacher = teacher
                                ))
                            }
                        }
                    ) {
                        Text(if (initialData == null) "Thêm" else "Lưu")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ScheduleScreenPreview() {
    MaterialTheme {
        ScheduleScreen()
    }
}
