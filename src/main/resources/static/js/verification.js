let timerInterval; // 타이머 인터벌 저장용
const TIMER_DURATION = 5 * 60; // 5분 (초)

// 메일 전송
function sendMail() {
    const email = (document.getElementById('email') || document.getElementById('emailRegister')).value;

    if (!email) {
        alert("이메일은 빈 값일 수 없습니다.");
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        alert("이메일 형식이 올바르지 않습니다.");
        document.getElementById("error-email").textContent = "이메일 형식이 올바르지 않습니다.";
        return;
    }

    // 기존 에러 메시지 초기화
    document.querySelectorAll('.error-text').forEach(div => div.textContent = '');

    const mailBtn = document.getElementById('btn_mail');
    mailBtn.disabled = true;

    fetch('/api/verification/send', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            email,
            purpose: document.getElementById('emailRegister') ? 'REGISTER' : 'RESET_PASSWORD'  })
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

                mailBtn.disabled = false;   // 실패 시에만 다시 버튼 활성화
                throw new Error("메일 전송 실패");
            } else {
                alert(data.message);

                const emailInput = document.getElementById('email');
                const codeInput = document.getElementById('verification-code');
                const verifyBtn = document.getElementById('btn_verify');

                // UI 초기화 및 활성화
                if (emailInput) emailInput.disabled = true;
                mailBtn.disabled = true;
                codeInput.value = '';
                codeInput.disabled = false;
                verifyBtn.disabled = false;

                // 타이머 표시
                if (document.getElementById('email')) {
                    document.getElementById('code-row').style.display = '';
                }
                document.getElementById('code-error-row').style.display = '';
                document.getElementById('timer-row').style.display = '';

                // 타이머 동안 mailBtn은 항상 비활성화
                startTimer(TIMER_DURATION, codeInput, verifyBtn, mailBtn);
            }
        })
        .catch(error => {
            console.error(error);
            // 실패일 때만 실행되는 catch
            mailBtn.disabled = false;
        });
}

// 타이머 시작
function startTimer(duration, codeInput, verifyBtn, mailBtn) {
    if (!codeInput || !verifyBtn || !mailBtn) return;
    const timerText = document.getElementById('timer-text');
    if (!timerText) return;

    let remaining = duration;
    clearInterval(timerInterval);

    timerInterval = setInterval(() => {
        if (remaining < 0) {
            clearInterval(timerInterval);
            timerText.textContent = '인증 코드 유효시간이 만료되었습니다.\n다시 시도해주세요.';
            codeInput.disabled = true;
            verifyBtn.disabled = true;
            mailBtn.disabled = false;
            return;
        }

        const minutes = Math.floor(remaining / 60);
        const seconds = remaining % 60;
        timerText.textContent = `인증 코드 남은 시간: ${minutes}:${seconds.toString().padStart(2,'0')}`;

        remaining--;
    }, 1000);
}

// 회원가입 - 인증코드 검증
function verifyRegister() {
    const email = document.getElementById('emailRegister').value;
    const code = document.getElementById('verification-code').value;

    if (!email) {
        alert("이메일은 빈 값일 수 없습니다.");
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        alert("이메일 형식이 올바르지 않습니다.");
        document.getElementById("error-email").textContent = "이메일 형식이 올바르지 않습니다.";
        return;
    }

    if (!code) {
        alert("인증 코드는 빈 값일 수 없습니다.");
        return;
    }

    // 기존 에러 메시지 초기화
    document.querySelectorAll('.error-text').forEach(div => div.textContent = '');

    fetch('/api/verification/verifyCode', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({email, code, purpose: "REGISTER"})
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
                    alert(data.message || "본인인증에 실패했습니다.");
                }
            } else {
                alert(data.message + "\n추가 정보를 입력해주세요.");

                // 인증 성공 시 타이머 종료 & 입력창/버튼 비활성화
                clearInterval(timerInterval);

                document.getElementById('verification-code').disabled = true;
                document.getElementById('btn_verify').disabled = true;
                document.getElementById('timer-text').textContent = '';

                document.getElementById('password').disabled = false;
                document.getElementById('confirmPassword').disabled = false;
                document.getElementById('nickname').disabled = false;
                document.getElementById("btn_nickname").disabled = false;

                document.getElementById("btn_register").disabled = false;
            }
        })
        .catch(error => {
            // throw new Error("본인 인증 실패");
        });
}

// 비밀번호찾기 - 인증코드 검증
function verify() {
    const email = document.getElementById('email').value;
    const code = document.getElementById('verification-code').value;

    if (!email) {
        alert("이메일은 빈 값일 수 없습니다.");
        return;
    }

    if (!code) {
        alert("인증 코드는 빈 값일 수 없습니다.");
        return;
    }

    // 기존 에러 메시지 초기화
    document.querySelectorAll('.error-text').forEach(div => div.textContent = '');

    fetch('/api/verification/verifyCode', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({email, code, purpose: "RESET_PASSWORD"})
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
                    alert(data.message || "본인인증에 실패했습니다.");
                    if (response.status === 409) window.location.href = "/members/login";
                }
            } else {
                alert(data.message + "\n비밀번호 변경 화면으로 이동합니다.");

                // 인증 성공 시 타이머 종료 & 입력창/버튼 비활성화
                clearInterval(timerInterval);
                document.getElementById('verification-code').disabled = true;
                document.getElementById('btn_verify').disabled = true;
                document.getElementById('timer-text').textContent = '';

                window.location.href = "/members/password";
            }
        })
        .catch(error => {
            // throw new Error("본인 인증 실패");
        });
}

document.addEventListener("DOMContentLoaded", function() {
    const emailInput = document.getElementById('email');
    const mailBtn = document.getElementById('btn_mail');
    const errorEmailDiv = document.getElementById('error-email');

    if (emailInput && mailBtn && errorEmailDiv) {

        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        // 입력 시 실시간 검증
        emailInput.addEventListener('input', function () {
            const email = emailInput.value.trim();

            // 버튼 활성화/비활성화
            mailBtn.disabled = !emailPattern.test(email);

            // 에러 메시지 표시
            if (!emailPattern.test(email) && email !== "") {
                errorEmailDiv.textContent = "이메일 형식이 올바르지 않습니다.";
                errorEmailDiv.style.display = "block";
            } else {
                errorEmailDiv.textContent = "";
                errorEmailDiv.style.display = "none";
            }
        });

        // 엔터 이벤트
        emailInput.addEventListener("keydown", function(event) {
            if (event.key === "Enter") {
                event.preventDefault();
                const email = emailInput.value.trim();

                if (emailPattern.test(email)) {
                    errorEmailDiv.textContent = "";
                    errorEmailDiv.style.display = "none";
                    sendMail(); // 이메일 형식 맞으면 전송
                } else {
                    errorEmailDiv.textContent = "이메일 형식이 올바르지 않습니다.";
                    errorEmailDiv.style.display = "block";
                }
            }
        });
    }

    // 비밀번호와 확인란 실시간 비교
    const passwordInput = document.getElementById('newPassword');
    const confirmInput = document.getElementById('confirmPassword');
    if (passwordInput && confirmInput) {
        passwordInput.addEventListener("input", checkPasswordMatch);
        confirmInput.addEventListener("input", checkPasswordMatch);
    }
});

// 비밀번호 검증
function checkPasswordMatch() {
    const passwordInput = document.getElementById('newPassword');
    const confirmInput = document.getElementById('confirmPassword');
    const errorDiv = document.getElementById('error-password');

    if (!passwordInput || !confirmInput || !errorDiv) return; // 요소 없으면 종료

    const password = passwordInput.value;
    const confirm = confirmInput.value;

    if (!password) {
        errorDiv.innerText = "";
        return;
    }

    if (password !== confirm) {
        errorDiv.innerText = "비밀번호가 일치하지 않습니다.";
    } else {
        errorDiv.innerText = "";
    }
}