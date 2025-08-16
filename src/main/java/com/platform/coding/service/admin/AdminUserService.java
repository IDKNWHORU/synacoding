package com.platform.coding.service.admin;

import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.admin.dto.UserManagementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    /**
     * 모든 회원을 페이지네이션하여 조회합니다. 검색 조건이 있을 경우 필터링합니다.
     */
    @Transactional(readOnly = true)
    public Page<UserManagementResponse> getUsers(String searchType, String keyword, Pageable pageable) {
        Page<User> users;
        if (StringUtils.hasText(keyword)) {
            if ("email".equalsIgnoreCase(searchType)) {
                users = userRepository.findByEmailContainingIgnoreCase(keyword, pageable);
            } else if ("userName".equalsIgnoreCase(searchType)) {
                users = userRepository.findByUserNameContainingIgnoreCase(keyword, pageable);
            } else {
                users = userRepository.findAll(pageable);
            }
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(UserManagementResponse::fromEntity);
    }

    /**
     * 특정 회원을 비활성화합니다.
     * @param userId 비활성화할 회원 ID
     * @param admin  요청을 수행하는 관리자
     */
    @Transactional
    public void deactivateUser(Long userId, User admin) {
        User user = findUserById(userId);
        // 자기 자신은 비활성화할 수 없도록 방어
        if (user.getId().equals(admin.getId())) {
            throw new IllegalArgumentException("자기 자신을 비활성화할 수 없습니다.");
        }
        user.updateActiveStatus(false);
    }

    /**
     * 특정 회원을 활성화합니다.
     * @param userId 활성화할 회원 ID
     * @param admin  요청을 수행하는 관리자
     */
    @Transactional
    public void activateUser(Long userId, User admin) {
        User user = findUserById(userId);
        user.updateActiveStatus(true);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }
}