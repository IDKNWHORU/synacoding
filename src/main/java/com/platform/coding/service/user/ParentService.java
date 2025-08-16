package com.platform.coding.service.user;

import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.user.dto.ChildAccountCreateRequest;
import com.platform.coding.service.user.dto.ChildAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 자녀 계정을 생성하고 현재 로그인된 학부모 계정과 연동한다.
     * @param request 자녀의 닉네임, 비밀번호 정보
     * @param detachedParent  현재 인증된 학부모 사용자 정보 (Detached 상태)
     * @return 생성된 자녀 계정 정보
     */
    @Transactional
    public ChildAccountResponse createChildAccount(ChildAccountCreateRequest request, User detachedParent) {
        // LazyInitializationException 방지를 위해 영속성 컨텍스트에 있는 부모 엔티티를 조회
        User parent = userRepository.findById(detachedParent.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학부모 계정입니다."));

        // 요청자가 학부모(PARENT)인지 확인
        if (parent.getUserType() != UserType.PARENT) {
            throw new IllegalStateException("자녀 계정은 학부모만 생성할 수 있습니다.");
        }
        
        // 이메일 중복 확인
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        });

        // 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(request.password());

        // DTO를 학생(STUDENT) User 엔티티로 변환
        User child = request.toEntity(encryptedPassword, parent);
        User savedChild = userRepository.save(child);

        // 부모-자식 연관관계 업데이트 (JPA의 변경 감지)
        parent.addChild(savedChild);

        return ChildAccountResponse.fromEntity(savedChild);
    }

    /**
     * 현재 로그인한 학부모의 모든 자녀 목록을 조회한다.
     * @param parent 현재 로그인한 학부모 User 객체
     * @return 자녀 정보 목록
     */
    @Transactional(readOnly = true)
    public List<ChildAccountResponse> getMyChildren(User parent) {
        // Lazy Loading으로 설정되어 있으므로, 자녀 목록을 직접 조회
        List<User> children = userRepository.findByParent(parent);
        return children.stream()
                .map(ChildAccountResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
