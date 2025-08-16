package com.platform.coding.service.user;

import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.user.dto.PasswordUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyInfoService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(User user, PasswordUpdateRequest request) {
        // 1. 새 비밀번호와 확인용 비밀번호가 일치하는지 확인
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        // 2. 현재 비밀번호가 올바른지 확인
        User persistentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.currentPassword(), persistentUser.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 3. 새 비밀번호를 암호화하여 업데이트
        persistentUser.updatePassword(passwordEncoder.encode(request.newPassword()));
    }
}