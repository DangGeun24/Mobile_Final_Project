package com.example.mapstalk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private List<Map<String, Object>> postList;

    public PostAdapter(List<Map<String, Object>> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_board_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> post = postList.get(position);

        String title = (String) post.get("title");
        String content = (String) post.get("content");
        
        holder.tvPostTitle.setText(title != null ? title : "제목 없음");
        holder.tvPostContent.setText(content != null ? content : "내용 없음");

        Object timestampObj = post.get("timestamp");
        if (timestampObj instanceof Timestamp) {
            Date date = ((Timestamp) timestampObj).toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.tvPostDate.setText(sdf.format(date));
        } else {
            holder.tvPostDate.setText("");
        }

        Object numObj = post.get("postNumber");
        if (numObj != null) {
            holder.tvPostNumber.setText("No. " + numObj);
            holder.tvPostNumber.setVisibility(View.VISIBLE);
        } else {
            holder.tvPostNumber.setVisibility(View.GONE);
        }

        String authorUid = (String) post.get("authorUid");
        if (authorUid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("Users").document(authorUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        holder.tvPostAuthor.setText(doc.getString("name"));
                    } else {
                        holder.tvPostAuthor.setText("이름 없음");
                    }
                });
        } else {
            holder.tvPostAuthor.setText("익명");
        }

        holder.itemView.setOnClickListener(v -> {
            String postId = (String) post.get("postId");
            if (postId != null) {
                android.content.Intent intent = new android.content.Intent(v.getContext(), PostDetailActivity.class);
                intent.putExtra("EXTRA_POST_ID", postId);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPostTitle, tvPostContent, tvPostDate, tvPostNumber, tvPostAuthor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvPostDate = itemView.findViewById(R.id.tvPostDate);
            tvPostNumber = itemView.findViewById(R.id.tvPostNumber);
            tvPostAuthor = itemView.findViewById(R.id.tvPostAuthor);
        }
    }
}
