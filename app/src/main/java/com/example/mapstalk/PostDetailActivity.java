package com.example.mapstalk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvAuthor, tvContent, tvDate;
    private Button btnEdit, btnDelete, btnSubmitComment;
    private EditText etComment;
    private RecyclerView rvComments;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String postId;
    private String currentUid;
    private String currentUserEmail;

    private CommentAdapter commentAdapter;
    private List<Map<String, Object>> commentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
            currentUserEmail = mAuth.getCurrentUser().getEmail();
        }

        postId = getIntent().getStringExtra("EXTRA_POST_ID");
        if (postId == null) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvContent = findViewById(R.id.tvContent);
        tvDate = findViewById(R.id.tvDate);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        
        etComment = findViewById(R.id.etComment);
        btnSubmitComment = findViewById(R.id.btnSubmitComment);
        rvComments = findViewById(R.id.rvComments);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(commentList);
        rvComments.setAdapter(commentAdapter);

        loadPostData();
        loadComments();

        btnDelete.setOnClickListener(v -> deletePost());
        
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(PostDetailActivity.this, WritePostActivity.class);
            intent.putExtra("EXTRA_POST_ID", postId);
            startActivity(intent);
        });

        btnSubmitComment.setOnClickListener(v -> submitComment());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPostData(); // 수정 후 돌아왔을 때 대비
    }

    private void loadPostData() {
        db.collection("Posts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String content = documentSnapshot.getString("content");
                        String authorUid = documentSnapshot.getString("authorUid");

                        tvTitle.setText(title);
                        tvContent.setText(content);

                        if (authorUid != null) {
                            db.collection("Users").document(authorUid).get().addOnSuccessListener(userDoc -> {
                                if (userDoc.exists() && userDoc.getString("name") != null) {
                                    tvAuthor.setText("작성자: " + userDoc.getString("name"));
                                } else {
                                    tvAuthor.setText("작성자: 이름 없음");
                                }
                            });
                        } else {
                            tvAuthor.setText("작성자: 익명");
                        }

                        com.google.firebase.Timestamp ts = documentSnapshot.getTimestamp("timestamp");
                        if (ts != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            tvDate.setText(sdf.format(ts.toDate()));
                        }

                        // 본인 확인 후 수정/삭제 버튼 노출
                        if (currentUid != null && currentUid.equals(authorUid)) {
                            btnEdit.setVisibility(View.VISIBLE);
                            btnDelete.setVisibility(View.VISIBLE);
                        } else {
                            btnEdit.setVisibility(View.GONE);
                            btnDelete.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(this, "삭제된 게시글입니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "게시글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePost() {
        db.collection("Posts").document(postId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show());
    }

    private void loadComments() {
        db.collection("Posts").document(postId).collection("Comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("PostDetail", "Listen failed.", error);
                        return;
                    }
                    commentList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            commentList.add(doc.getData());
                        }
                    }
                    commentAdapter.notifyDataSetChanged();
                });
    }

    private void submitComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "댓글을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> comment = new HashMap<>();
        comment.put("content", text);
        comment.put("authorUid", currentUid);
        comment.put("authorEmail", currentUserEmail);
        comment.put("timestamp", new com.google.firebase.Timestamp(new java.util.Date()));

        btnSubmitComment.setEnabled(false);
        db.collection("Posts").document(postId).collection("Comments").add(comment)
                .addOnSuccessListener(documentReference -> {
                    etComment.setText("");
                    btnSubmitComment.setEnabled(true);
                    // loadComments()는 snapshotListener가 알아서 갱신해줍니다.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "댓글 작성 실패", Toast.LENGTH_SHORT).show();
                    btnSubmitComment.setEnabled(true);
                });
    }
}
