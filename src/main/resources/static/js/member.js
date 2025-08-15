// 로그인
// 네이버 로그인
function naverLogin() {
    fetch('/api/members/oauth/naver', {
        method: 'GET',
        credentials: 'include'
    })
        .then(response => response.json())
        .then(data => {
            if (data.message) {
                alert(data.message);
                window.location.href = "/";
            } else if (data.error) {
                alert(data.error);
                window.location.href = "/members/login";
            }
        })
        .catch(error => {
            throw new Error("네이버 로그인 실패");
        });
}

// 구글 로그인
function googleLogin() {
    fetch('/api/members/oauth/google', {
        method: 'GET',
        credentials: 'include'
    })
        .then(response => response.json())
        .then(data => {
            if (data.message) {
                alert(data.message);
                window.location.href = "/";
            } else if (data.error) {
                alert(data.error);
                window.location.href = "/members/login";
            }
        })
        .catch(error => {
            throw new Error("구글 로그인 실패");
        });
}

// 이메일 중복
function validEmail() {
    const email = document.getElementById('emailRegister').value;
    const emailCheckButton = document.getElementById('btn_email_check');
    const registerButton = document.getElementById("btn_register");

    if (!email) {
        alert("이메일을 입력해주세요.");
        return;
    }

    // 기존 에러 메시지 초기화
    document.getElementById('error-email').textContent = '';

    fetch(`/api/members/validate?email=${encodeURIComponent(email)}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then(async response => {
            let message = await response.text();
            if (!response.ok) {
                if (response.status != 409) {
                    message = '이메일 중복 검증에 실패했습니다. 다시 시도해주세요.';
                }
                alert(message);
                document.getElementById('error-email').textContent = message;
                // throw new Error(message); // catch로 흘러감 (로그용)
            } else {
                alert(message);

                // 중복체크 이후 중복확인 막고 회원가입 활성화
                emailCheckButton.disabled = true;
                registerButton.disabled = false;
            }
        });
}

// 자체로그인
function login() {
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const providerType = document.getElementById('providerType').value;

    if (!email || !password || !providerType) {
        alert("이메일과 비밀번호는 빈 값일 수 없습니다.");
        return;
    }

    // 기존 에러 메시지 초기화
    document.querySelectorAll('.error-text').forEach(div => div.textContent = '');

    fetch('/api/members/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            email: email,
            password: password,
            providerType: providerType,
        }),
        credentials: 'include'
    })
        .then(async response => {
            const text = await response.text();
            let data;

            try {
                data = JSON.parse(text);
            } catch (e) {
                data = {message: text};
            }

            if (!response.ok) {
                if (data.validation) {
                    for (const [field, message] of Object.entries(data.validation)) {
                        const errorDiv = document.getElementById(`error-${field}`);
                        if (errorDiv) errorDiv.textContent = data.validation?.[field] || '';
                    }
                }

                if (data.message) {
                    alert(data.message);
                }

                throw new Error("로그인 실패");
            } else {
                alert(data.message);
                window.location.href = "/";
            }
        })
        .catch(error => {
            throw new Error("로그인 실패");
        });
}

document.addEventListener("DOMContentLoaded", function() {
    // 비밀번호 엔터 이벤트
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener("keydown", function(event) {
            if (event.key === "Enter") {
                event.preventDefault();
                login();
            }
        });
    }

    // 이메일 입력 시 버튼 활성화 이벤트
    const emailRegisterInput = document.getElementById('emailRegister');
    if (emailRegisterInput) {
        emailRegisterInput.addEventListener('input', function () {
            const emailCheck = document.getElementById('btn_email_check');
            const register = document.getElementById("btn_register");
            if (emailCheck && register) {
                emailCheck.disabled = false;
                register.disabled = true;
            }
        });
    }
});

// 회원가입
function register() {
    const emailError = document.getElementById('error-email').textContent;
    if (emailError !== null && "" !== emailError) {
        alert("이메일 중복 검증은 필수입니다.")
        return;
    }

    const name = document.querySelector('input[name="name"]').value;
    const email = document.querySelector('input[name="email"]').value;
    const password = document.querySelector('input[name="password"]').value;
    const nickname = document.querySelector('input[name="nickname"]').value;
    const providerType = document.getElementById('providerType').value;

    if (!name) {
        alert("이름은 빈 값일 수 없습니다.");
        return;
    }

    if (!nickname) {
        alert("닉네임은 빈 값일 수 없습니다.");
        return;
    }

    if (!email || !password || !providerType) {
        alert("이메일과 비밀번호는 빈 값일 수 없습니다.");
        return;
    }

    // 기존 에러 메시지 초기화
    document.querySelectorAll('.error-text').forEach(div => div.textContent = '');

    fetch('/api/members/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({name, email, password, nickname, providerType})
    })
        .then(async response => {
            const text = await response.text();
            let data;

            try {
                data = JSON.parse(text);
            } catch (e) {
                data = {message: text};
            }

            if (!response.ok) {
                if (data.validation) {
                    for (const [field, message] of Object.entries(data.validation)) {
                        const errorDiv = document.getElementById(`error-${field}`);
                        if (errorDiv) errorDiv.textContent = data.validation?.[field] || '';
                    }
                } else {
                    alert(data.message || "회원가입에 실패했습니다.");
                }
            } else {
                alert(data.message);
                window.location.href = "/members/login";
            }
        })
        .catch(error => {
            // throw new Error("회원가입 실패");
        });
}

// 로그아웃
function logOut() {
    fetch('/api/members/logout', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then(response => response.json())
        .then(data => {
            if (data.message) {
                alert(data.message);
                window.location.href = "/";
            } else if (data.error) {
                alert(data.error);
                window.location.href = "/members/login";
            }
        })
        .catch(error => {
            alert(error.message);
            window.location.replace("/");
            throw new Error("로그아웃 실패");
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
            alert('닉네임 추천에 실패했습니다. 다시 시도해주세요.');
        });
}

// 수정
function updateAccount() {
    const nicknameInput = document.getElementById('nickname');
    const password = document.getElementById('password').value;
    const nickname = document.getElementById('nickname').value;

    if (!password) {
        alert("비밀번호는 빈 값일 수 없습니다.");
        return;
    }

    if (nickname.length > 50 || !nickname) {
        alert("닉네임은 1자 이상 50자 이하로 입력해주세요.");
        nicknameInput.focus();
        return;
    }

    // 기존 에러 메시지 초기화
    document.querySelectorAll('.error-text').forEach(div => div.textContent = '');

    let updatedProfile;

    if (profile.providerType === "PINUP") {
        updatedProfile = {
            name: profile.name,
            email: profile.email,
            password,
            nickname,
            providerType: profile.providerType,
            role: profile.role
        };
    } else {
        updatedProfile = {
            name: profile.name,
            email: profile.email,
            nickname,
            providerType: profile.providerType,
            role: profile.role
        };
    }

    fetch('/api/members', {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(updatedProfile)
    })
        .then(async response => {
            const data = await response.json();

            if (!response.ok) {
                if (data.validation) {
                    for (const [field, message] of Object.entries(data.validation)) {
                        const errorDiv = document.getElementById(`error-${field}`);
                        if (errorDiv) {
                            errorDiv.textContent = message;
                        }
                    }
                }

                if (data.message) {
                    alert(data.message);
                }

                throw new Error("회원 정보 수정 실패");
            }

            alert(data.message || "회원 정보가 수정되었습니다.");
            window.location.reload();
        })
        .catch(error => {
            alert(error.message || "회원 정보 수정 중 알 수 없는 오류가 발생했습니다.");
        });
}

// 탈퇴
function deleteAccount() {
    if (confirm("정말 탈퇴하시겠습니까?\n이 작업은 되돌릴 수 없습니다.\n" +
        "또한, 작성한 게시글과 댓글은 삭제되지 않습니다.")) {
        fetch('/api/members', {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(profile)
        })
            .then(response => response.json())
            .then(data => {
                if (data.message) {
                    alert(data.message);
                    window.location.href = "/";
                } else if (data.error) {
                    alert(data.error);
                }
            })
            .catch(error => {
                alert('서버와의 연결에 실패했습니다.');
            });
    }
}

function redirectToHome() {
    window.location.href = "/";
}