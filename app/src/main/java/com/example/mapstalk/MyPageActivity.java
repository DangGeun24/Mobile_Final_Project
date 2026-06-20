package com.example.mapstalk;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPageActivity extends AppCompatActivity {

    private TextView tvUserNickname, tvUserEmail;
    private Button btnChangeNickname, btnChangePassword, btnLogout, btnDeleteAccount;
    private TextView tvTabMyPosts, tvTabMyPlaces;
    private RecyclerView rvMyPosts, rvMyPlaces;
    private PostAdapter adapter;
    private PlaceAdapter placeAdapter;
    private List<Map<String, Object>> myPostList = new ArrayList<>();
    private List<PlaceItem> myPlaceList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvUserNickname = findViewById(R.id.tvUserNickname);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        btnChangeNickname = findViewById(R.id.btnChangeNickname);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        
        tvTabMyPosts = findViewById(R.id.tvTabMyPosts);
        tvTabMyPlaces = findViewById(R.id.tvTabMyPlaces);
        rvMyPosts = findViewById(R.id.rvMyPosts);
        rvMyPlaces = findViewById(R.id.rvMyPlaces);

        tvUserEmail.setText(currentUser.getEmail());
        loadUserProfile();

        adapter = new PostAdapter(myPostList);
        rvMyPosts.setLayoutManager(new LinearLayoutManager(this));
        rvMyPosts.setAdapter(adapter);

        placeAdapter = new PlaceAdapter(myPlaceList, item -> {
            Intent intent = new Intent(MyPageActivity.this, StoreInfoActivity.class);
            intent.putExtra("PLACE_ID", item.getPlaceId());
            intent.putExtra("PLACE_NAME", item.getName());
            intent.putExtra("ADDRESS", item.getAddress());
            intent.putExtra("LATITUDE", item.getLatitude());
            intent.putExtra("LONGITUDE", item.getLongitude());
            startActivity(intent);
        });
        placeAdapter.setOnItemLongClickListener(item -> {
            CharSequence[] options = {"장소 이름 수정", "장소 삭제"};
            new android.app.AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditPlaceDialog(item);
                    } else if (which == 1) {
                        showDeletePlaceDialog(item);
                    }
                })
                .show();
            return true;
        });
        rvMyPlaces.setLayoutManager(new LinearLayoutManager(this));
        rvMyPlaces.setAdapter(placeAdapter);

        tvTabMyPosts.setOnClickListener(v -> {
            tvTabMyPosts.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTabMyPosts.setTextColor(android.graphics.Color.parseColor("#333333"));
            tvTabMyPlaces.setTypeface(null, android.graphics.Typeface.NORMAL);
            tvTabMyPlaces.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
            rvMyPosts.setVisibility(View.VISIBLE);
            rvMyPlaces.setVisibility(View.GONE);
        });

        tvTabMyPlaces.setOnClickListener(v -> {
            tvTabMyPlaces.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTabMyPlaces.setTextColor(android.graphics.Color.parseColor("#333333"));
            tvTabMyPosts.setTypeface(null, android.graphics.Typeface.NORMAL);
            tvTabMyPosts.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
            rvMyPosts.setVisibility(View.GONE);
            rvMyPlaces.setVisibility(View.VISIBLE);
            if (myPlaceList.isEmpty()) {
                loadMyPlaces();
            }
        });

        btnChangeNickname.setOnClickListener(v -> showChangeNicknameDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadMyPosts();
            if (rvMyPlaces.getVisibility() == View.VISIBLE) {
                loadMyPlaces();
            }
        }
    }

    private void loadMyPlaces() {
        db.collection("CustomPlaces")
                .whereEqualTo("creatorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myPlaceList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String pId = doc.getString("placeId");
                        String pName = doc.getString("name");
                        String pAddress = doc.getString("address");
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        if (lat != null && lng != null) {
                            myPlaceList.add(new PlaceItem(pId, pName, pAddress, lat, lng));
                        }
                    }
                    // 최신순 정렬
                    myPlaceList.sort((p1, p2) -> {
                        Object t1 = null;
                        Object t2 = null;
                        for(QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            if(doc.getString("placeId").equals(p1.getPlaceId())) t1 = doc.get("timestamp");
                            if(doc.getString("placeId").equals(p2.getPlaceId())) t2 = doc.get("timestamp");
                        }
                        if (t1 instanceof com.google.firebase.Timestamp && t2 instanceof com.google.firebase.Timestamp) {
                            return ((com.google.firebase.Timestamp) t2).compareTo((com.google.firebase.Timestamp) t1);
                        }
                        return 0;
                    });

                    placeAdapter.notifyDataSetChanged();
                    if (myPlaceList.isEmpty() && rvMyPlaces.getVisibility() == View.VISIBLE) {
                        Toast.makeText(this, "추가한 장소가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "장소를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditPlaceDialog(PlaceItem item) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("장소 이름 수정");
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(item.getName());
        input.setSelection(input.getText().length());
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("수정", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(item.getName())) {
                db.collection("CustomPlaces").document(item.getPlaceId())
                        .update("name", newName)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "장소 이름이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                            loadMyPlaces();
                        });
            }
        });
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeletePlaceDialog(PlaceItem item) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("장소 삭제")
            .setMessage("정말로 이 장소를 삭제하시겠습니까?\n(이 장소의 게시판은 검색되지 않습니다.)")
            .setPositiveButton("삭제", (dialog, which) -> {
                db.collection("CustomPlaces").document(item.getPlaceId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "장소가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            loadMyPlaces();
                        });
            })
            .setNegativeButton("취소", (dialog, which) -> dialog.cancel())
            .show();
    }

    private void loadUserProfile() {
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            tvUserNickname.setText(currentUser.getDisplayName());
        } else {
            // 과거 계정 대응: Auth 프로필에 이름이 없으면 Firestore에서 조회
            db.collection("Users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("name") != null) {
                        tvUserNickname.setText(doc.getString("name"));
                    } else {
                        tvUserNickname.setText("이름 없음 (수정해주세요)");
                    }
                });
        }
    }

    private void showChangeNicknameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("닉네임 변경");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        final EditText input = new EditText(this);
        input.setHint("새 닉네임을 입력하세요");
        input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        rowLayout.addView(input);

        Button btnCheck = new Button(this);
        btnCheck.setText("중복 확인");
        rowLayout.addView(btnCheck);

        layout.addView(rowLayout);
        builder.setView(layout);

        builder.setPositiveButton("변경", null);
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        final boolean[] isNicknameChecked = {false};

        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNicknameChecked[0] = false;
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnCheck.setOnClickListener(v -> {
            String newNickname = input.getText().toString().trim();
            if (newNickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("Users").whereEqualTo("name", newNickname).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "이미 사용 중인 닉네임입니다.", Toast.LENGTH_SHORT).show();
                        isNicknameChecked[0] = false;
                    } else {
                        Toast.makeText(this, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show();
                        isNicknameChecked[0] = true;
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newNickname = input.getText().toString().trim();
            if (newNickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isNicknameChecked[0]) {
                Toast.makeText(this, "먼저 닉네임 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateNickname(newNickname);
            dialog.dismiss();
        });
    }

    private void updateNickname(String newName) {
        // 1. Firebase Auth 프로필 업데이트
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
        currentUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 2. Firestore Users 컬렉션 업데이트 (없으면 생성)
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", newName);
                updates.put("uid", currentUser.getUid());
                updates.put("email", currentUser.getEmail());
                
                db.collection("Users").document(currentUser.getUid()).set(updates) // set으로 덮어씌움 (새 계정인 경우 대비)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "닉네임이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                        tvUserNickname.setText(newName);
                        // 게시글 목록 갱신 (닉네임 즉시 반영)
                        adapter.notifyDataSetChanged();
                    });
            } else {
                Toast.makeText(this, "닉네임 변경 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("비밀번호 변경");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputCurrent = new EditText(this);
        inputCurrent.setHint("현재 비밀번호");
        inputCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputCurrent);

        final EditText input1 = new EditText(this);
        input1.setHint("새 비밀번호 (6자리 이상)");
        input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(input1);

        final EditText input2 = new EditText(this);
        input2.setHint("새 비밀번호 확인");
        input2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(input2);

        builder.setView(layout);

        builder.setPositiveButton("변경", null);
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPwd = inputCurrent.getText().toString();
            String pwd1 = input1.getText().toString();
            String pwd2 = input2.getText().toString();

            if (currentPwd.isEmpty() || pwd1.isEmpty() || pwd2.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pwd1.length() < 6) {
                Toast.makeText(this, "새 비밀번호는 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pwd1.equals(pwd2)) {
                Toast.makeText(this, "새 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.firebase.auth.AuthCredential credential = 
                com.google.firebase.auth.EmailAuthProvider.getCredential(currentUser.getEmail(), currentPwd);

            currentUser.reauthenticate(credential).addOnCompleteListener(authTask -> {
                if (authTask.isSuccessful()) {
                    currentUser.updatePassword(pwd1).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this, "비밀번호 변경 실패", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "현재 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("회원 탈퇴");
        builder.setMessage("정말 탈퇴하시겠습니까? 본인 확인을 위해 현재 비밀번호를 입력해주세요.");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputCurrent = new EditText(this);
        inputCurrent.setHint("현재 비밀번호");
        inputCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputCurrent);

        builder.setView(layout);

        builder.setPositiveButton("탈퇴", null);
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPwd = inputCurrent.getText().toString();
            if (currentPwd.isEmpty()) {
                Toast.makeText(this, "현재 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.firebase.auth.AuthCredential credential = 
                com.google.firebase.auth.EmailAuthProvider.getCredential(currentUser.getEmail(), currentPwd);

            currentUser.reauthenticate(credential).addOnCompleteListener(authTask -> {
                if (authTask.isSuccessful()) {
                    currentUser.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MyPageActivity.this, "회원탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MyPageActivity.this, "탈퇴 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "현재 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void loadMyPosts() {
        db.collection("Posts")
                .whereEqualTo("authorUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myPostList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = document.getData();
                        data.put("postId", document.getId());
                        myPostList.add(data);
                    }
                    
                    // 메모리상 최신순 정렬 (인덱스 에러 방지)
                    myPostList.sort((p1, p2) -> {
                        Object t1 = p1.get("timestamp");
                        Object t2 = p2.get("timestamp");
                        if (t1 instanceof com.google.firebase.Timestamp && t2 instanceof com.google.firebase.Timestamp) {
                            return ((com.google.firebase.Timestamp) t2).compareTo((com.google.firebase.Timestamp) t1);
                        }
                        return 0;
                    });
                    
                    adapter.notifyDataSetChanged();
                    
                    if (myPostList.isEmpty()) {
                        Toast.makeText(this, "작성한 글이 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("MyPageActivity", "Error loading my posts", e);
                });
    }
}
