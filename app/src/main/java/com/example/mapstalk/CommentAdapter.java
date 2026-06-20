package com.example.mapstalk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private List<Map<String, Object>> commentList;

    public CommentAdapter(List<Map<String, Object>> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> comment = commentList.get(position);
        
        holder.tvCommentAuthor.setText("불러오는 중...");
        String authorUid = (String) comment.get("authorUid");
        if (authorUid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("Users").document(authorUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        holder.tvCommentAuthor.setText(doc.getString("name"));
                    } else {
                        holder.tvCommentAuthor.setText("이름 없음");
                    }
                });
        } else {
            String author = (String) comment.get("authorEmail");
            if (author == null) author = "익명";
            holder.tvCommentAuthor.setText(author);
        }

        String content = (String) comment.get("content");
        holder.tvCommentContent.setText(content);

        Object timestampObj = comment.get("timestamp");
        if (timestampObj instanceof com.google.firebase.Timestamp) {
            com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) timestampObj;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.tvCommentDate.setText(sdf.format(ts.toDate()));
        } else {
            holder.tvCommentDate.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommentAuthor, tvCommentContent, tvCommentDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentDate = itemView.findViewById(R.id.tvCommentDate);
        }
    }
}
