package com.dinh.gocnho.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dinh.gocnho.databinding.ItemStoryBinding
import com.dinh.gocnho.model.Story

class StoryAdapter(
    private val onItemClick: (Story) -> Unit
) : ListAdapter<Story, StoryAdapter.StoryViewHolder>(StoryDiffCallback) {

    inner class StoryViewHolder(
        private val binding: ItemStoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(story: Story) {
            // 1. Title (Non-null)
            binding.tvTitle.text = story.title.ifBlank { "Truyện chưa đặt tên" }

            // 2. Author (Nullable: xử lý an toàn Null Safety)
            if (!story.author.isNullOrBlank()) {
                binding.tvAuthor.visibility = View.VISIBLE
                binding.tvAuthor.text = "Tác giả: ${story.author}"
            } else {
                // Fallback nếu không có tên tác giả
                binding.tvAuthor.visibility = View.VISIBLE
                binding.tvAuthor.text = "Tác giả: Đang cập nhật"
            }

            // 3. Thời gian cập nhật / tạo
            binding.tvUpdatedAt.text = "Cập nhật: ${story.getFormattedTime()}"

            // 4. Sự kiện click item -> lấy documentId
            binding.root.setOnClickListener {
                onItemClick(story)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val StoryDiffCallback = object : DiffUtil.ItemCallback<Story>() {
            override fun areItemsTheSame(oldItem: Story, newItem: Story): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Story, newItem: Story): Boolean {
                return oldItem == newItem
            }
        }
    }
}
