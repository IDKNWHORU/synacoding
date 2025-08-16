// 모든 페이지에서 공통으로 사용될 JavaScript 코드를 작성합니다.
async function logout() {
    try {
        const response = await fetch('/api/users/logout', {
            method: 'POST'
        });

        if (response.ok) {
            // localStorage에 저장된 다른 정보가 있다면 여기서 삭제
            // localStorage.removeItem('someOtherItem');
            alert('로그아웃 되었습니다.');
            window.location.href = '/';
        } else {
            alert('로그아웃에 실패했습니다.');
        }
    } catch (error) {
        console.error('Logout error:', error);
        alert('로그아웃 중 오류가 발생했습니다.');
    }
}

/**
 * 동적 헤더 업데이트를 위한 스크립트
 */
document.addEventListener("DOMContentLoaded", function() {
    const notificationBadge = document.getElementById('notification-badge');

    // 알림 뱃지가 페이지에 존재하는 경우 (즉, 로그인한 상태)에만 주기적 업데이트 로직을 실행합니다.
    if (notificationBadge) {
        let intervalId = null; // interval을 제어하기 위한 변수

        const updateNotificationCount = async () => {
            try {
                const response = await fetch('/api/notifications/unread-count');
                if (!response.ok) {
                    // 인증이 만료(401)되는 등 에러 발생 시 더 이상 API를 호출하지 않도록 중단합니다.
                    if (intervalId) {
                        clearInterval(intervalId);
                    }
                    return;
                }

                const data = await response.json();
                const count = data.count;

                // 개수에 따라 뱃지의 내용과 표시 여부를 업데이트합니다.
                if (count > 0) {
                    notificationBadge.textContent = count;
                    notificationBadge.style.display = 'inline-block';
                } else {
                    notificationBadge.textContent = '';
                    notificationBadge.style.display = 'none';
                }
            } catch (error) {
                console.error('Failed to fetch notification count:', error);
                // 네트워크 오류 등이 발생해도 주기적 호출을 중단합니다.
                if (intervalId) {
                    clearInterval(intervalId);
                }
            }
        };

        // 페이지 로드 시 즉시 1회 실행
        updateNotificationCount();

        // 이후 60초(1분)마다 주기적으로 실행
        intervalId = setInterval(updateNotificationCount, 60000);
    }
});