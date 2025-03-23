// 로그아웃
function logOut() {
    fetch('/api/members/logout', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then(response => {
            if (!response.ok) {
                // 응답 상태 코드가 200번대가 아닌 경우
                return response.text().then(text => { throw new Error(text); });
            }
            return response.text(); // 로그아웃 성공 메시지
        })
        .then(text => {
            alert(text); // 성공 메시지 표시
            window.location.replace("/"); // 메인 페이지로 리다이렉션
        })
        .catch(error => {
            console.error('로그아웃 중 오류 발생:', error);
            alert(error.message); // 서버에서 보낸 오류 메시지 표시
            window.location.replace("/"); // 메인 페이지로 리다이렉션
        });
}

// 닉네임 추천받기
function generateNickname() {
    fetch('/api/members/nickname', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('닉네임 추천 실패');
            }
            return response.text();
        })
        .then(nickname => {
            document.getElementById('nickname').value = nickname;
        })
        .catch(error => {
            console.error(error);
            alert('닉네임 추천에 실패했습니다. 다시 시도해주세요.');
        });
}

// 수정
function updateAccount() {
    const nickname = document.getElementById('nickname').value;
    const updatedProfile = {
        name: profile.name,
        email: profile.email,
        nickname,
        providerType: profile.providerType,
        role: profile.role
    };
    console.log("updatedProfile", updatedProfile)

    fetch('/api/members', {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(updatedProfile)
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(error => {
                    throw new Error(error.message);
                });
            }
            return response.text();
        })
        .then(text => {
            alert(text);
            if (text === "수정 성공") {
                window.location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert(error.message);
        });
}

// 탈퇴
function deleteAccount() {
    fetch('/api/members', {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(profile)
    })
        .then(response => response.text())
        .then(text => {
            if (text === "탈퇴 성공") {
                alert(text);
                window.location.href = "/";
            } else {
                alert(text);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('서버와의 연결에 실패했습니다.');
        });
}

function redirectToHome() {
    window.location.href = "/";
}