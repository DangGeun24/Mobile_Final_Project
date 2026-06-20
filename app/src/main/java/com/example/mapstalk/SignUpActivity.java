package com.example.mapstalk;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mapstalk.databinding.ActivitySignUpBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String generatedCaptcha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        generateCaptcha();

        binding.btnSignUp.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String passwordConfirm = binding.etPasswordConfirm.getText().toString().trim();
            String captchaInput = binding.etCaptchaInput.getText().toString().trim();

            boolean[] hasError = {false};

            if (name.isEmpty()) {
                binding.etName.setError("이름(닉네임)을 입력해주세요.");
                hasError[0] = true;
            }
            if (email.isEmpty()) {
                binding.etEmail.setError("이메일을 입력해주세요.");
                hasError[0] = true;
            }
            
            if (password.isEmpty()) {
                binding.etPassword.setError("비밀번호를 입력해주세요.");
                hasError[0] = true;
            } else if (password.length() < 6) {
                binding.etPassword.setError("비밀번호는 6자리 이상이어야 합니다.");
                hasError[0] = true;
            }

            if (passwordConfirm.isEmpty()) {
                binding.etPasswordConfirm.setError("비밀번호 확인을 입력해주세요.");
                hasError[0] = true;
            } else if (!password.equals(passwordConfirm)) {
                binding.etPasswordConfirm.setError("비밀번호가 일치하지 않습니다.");
                hasError[0] = true;
            }

            if (captchaInput.isEmpty() || !captchaInput.equals(generatedCaptcha)) {
                binding.etCaptchaInput.setError("자동가입방지 번호가 틀렸습니다.");
                binding.etCaptchaInput.setText("");
                generateCaptcha(); // 실패 시 번호 재생성
                hasError[0] = true;
            }

            // DB 통신을 하기 위해 기본적으로 값이 있어야 하는 것들 체크 (이메일, 닉네임)
            if (name.isEmpty() || email.isEmpty()) {
                if (hasError[0]) return; 
            }

            // 1. 비동기 닉네임 중복 검사
            db.collection("Users").whereEqualTo("name", name).get().addOnCompleteListener(nameTask -> {
                // 2. 비동기 이메일 중복 검사 (Firestore에서 직접 조회)
                db.collection("Users").whereEqualTo("email", email).get().addOnCompleteListener(emailTask -> {
                    
                    boolean isNameDuplicated = nameTask.isSuccessful() && !nameTask.getResult().isEmpty();
                    boolean isEmailDuplicated = emailTask.isSuccessful() && !emailTask.getResult().isEmpty();

                    // 에러가 있다면 한 번에 UI에 띄움
                    if (isNameDuplicated) {
                        binding.etName.setError("이미 사용 중인 닉네임입니다.");
                    }
                    if (isEmailDuplicated) {
                        binding.etEmail.setError("이미 사용 중인 이메일입니다.");
                    }

                    // 동기 에러(빈칸/비번 등)나 비동기 에러(중복)가 하나라도 있으면 가입 중단
                    if (hasError[0] || isNameDuplicated || isEmailDuplicated) {
                        return; 
                    }

                    // 3. 모든 에러가 없으면 진짜 파이어베이스 회원가입 진행
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        Map<String, Object> userInfo = new HashMap<>();
                                        userInfo.put("uid", user.getUid());
                                        userInfo.put("name", name);
                                        userInfo.put("email", email);

                                        com.google.firebase.auth.UserProfileChangeRequest profileUpdates = 
                                                new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                        .setDisplayName(name)
                                                        .build();
                                        user.updateProfile(profileUpdates);

                                        db.collection("Users").document(user.getUid()).set(userInfo)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "회원가입 완료!", Toast.LENGTH_SHORT).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(this, "DB 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                } else {
                                    if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                        binding.etEmail.setError("이미 사용 중인 이메일입니다.");
                                        binding.etEmail.requestFocus();
                                    } else {
                                        Toast.makeText(this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                });
            });
        });
    }

    private void generateCaptcha() {
        int randomNum = (int) (Math.random() * 9000) + 1000; // 1000 ~ 9999 (4자리 숫자)
        generatedCaptcha = String.valueOf(randomNum);
        binding.tvCaptcha.setText(generatedCaptcha);
    }
}

