// 로그아웃
function logOut() {
    fetch('/api/members/logout', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then(response => response.text())
        .then(text => {
            if (text === "로그아웃 성공") {
                alert(text);
                window.location.replace("/");
            } else {
                alert(text);
            }
        })
        .catch(error => {
            console.error('로그아웃 중 오류 발생:', error);
            alert('로그아웃 중 오류가 발생했습니다.');
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
        .then(response => response.text())
        .then(text => {
            if (text === "수정 성공") {
                alert(text);
                window.location.reload();
            } else {
                alert(text);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('서버와의 연결에 실패했습니다.');
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