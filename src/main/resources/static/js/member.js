// 로그아웃
function logOut() {
    fetch('/users/logout', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then(response => response.text())
        .then(text => {
            if (text === "로그아웃 성공") {
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

// 수정
function updateAccount() {
    var nickname = document.getElementById('nickname').value;
    var updatedProfile = {
        name: profile.name,
        email: profile.email,
        nickname: nickname,
        providerType: profile.providerType,
        role: profile.role
    };
    console.log("updatedProfile", updatedProfile)

    fetch('/users', {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(updatedProfile)
    })
        .then(response => response.text().then(text => {
            alert(text);
            if (response.ok) {
                window.location.reload();
            } else if (response.status >= 403 && response.status < 500) {
                window.location.href = "/";
            }
        }))
        .catch(error => {
            console.error('Error:', error);
            alert('서버와의 연결에 실패했습니다.');
        });
}

// 탈퇴
function deleteAccount() {
    fetch('/users', {
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