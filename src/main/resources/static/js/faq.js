document.addEventListener('DOMContentLoaded', () => {
    const contentListWrap = document.querySelector('.content_list_wrap');

    const toggleAnswer = (clickedTitle) => {
        contentListWrap.querySelectorAll('.content_list').forEach((faq) => {
            const title = faq.querySelector('.title_area');
            const answer = faq.querySelector('.answer_area');
            const icon = faq.querySelector('.content_area i');

            const isExpanded = title.getAttribute('aria-expanded') === 'true';
            const isClicked = title === clickedTitle;
            const newState = isClicked ? !isExpanded : false;

            title.setAttribute('aria-expanded', newState);
            answer.classList.toggle('expanded', newState);
            icon.classList.toggle('fa-angle-up', newState);
            icon.classList.toggle('fa-angle-down', !newState);
        });
    };

    contentListWrap?.addEventListener('click', (event) => {
        const title = event.target.closest('.title_area');
        if (title) {
            toggleAnswer(title);
        }
    });
});

const removeFaq = async (id) => {
    const result = confirm('정말 삭제 하시겠습니까?');
    if (!result) {
        return
    }

    const response = await fetch(`http://localhost:8080/api/faqs/${id}`, {
        method: 'DELETE'
    })

    if (response.status === 204) {
        location.href = 'http://localhost:8080/faqs'
    }
}

const createFaq = async () => {
    const categoryErrorElem = document.getElementById('category-error')
    const questionErrorElem = document.getElementById('question-error')
    const answerErrorElem = document.getElementById('answer-error')
    categoryErrorElem.textContent = ''
    questionErrorElem.textContent = ''
    answerErrorElem.textContent = ''

    const categoryElem = document.getElementById('category');
    const category = categoryElem.options[categoryElem.selectedIndex].value.trim()
    const question = document.getElementById('question').value.trim()
    const answer = document.getElementById('answer').value.trim()

    let flag = false
    if (!category || category === '') {
        categoryErrorElem.textContent = '카테고리를 선택해주세요.'
        flag = true
    }
    if (!question || question === '') {
        questionErrorElem.textContent = '질문 내용을 입력하세요.'
        flag = true
    }
    if (!answer || answer === '') {
        answerErrorElem.textContent = '답변 내용을 입력하세요.'
        flag = true
    }

    if (flag) {
        return
    }

    const jsonData = {
        category,
        question,
        answer,
    };
    const response = await fetch('http://localhost:8080/api/faqs', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(jsonData),
    })

    if (response.status === 201) {
        location.href = 'http://localhost:8080/faqs'
    }
    if (response.status === 400) {
        const data = await response.json()
        const { validation } = data
        if (validation && Object.keys(validation).length >= 1) {
            for (const [key, value] of Object.entries(validation)) {
                const field = document.getElementById(key + '-error')
                field.textContent = value
            }
        }
    }
}

const updateFaq = async (id) => {
    const result = confirm('정말 수정하시겠습니까?');
    if (!result) {
        return
    }

    const categoryErrorElem = document.getElementById('category-error')
    const questionErrorElem = document.getElementById('question-error')
    const answerErrorElem = document.getElementById('answer-error')
    categoryErrorElem.textContent = ''
    questionErrorElem.textContent = ''
    answerErrorElem.textContent = ''

    const categoryElem = document.getElementById('category')
    const category = categoryElem.options[categoryElem.selectedIndex].value.trim()
    const question = document.getElementById('question').value.trim()
    const answer = document.getElementById('answer').value.trim()

    let flag = false
    if (!category || category === '') {
        categoryErrorElem.textContent = '카테고리를 선택해주세요.'
        flag = true
    }
    if (!question || question === '') {
        questionErrorElem.textContent = '질문 내용을 입력하세요.'
        flag = true
    }
    if (!answer || answer === '') {
        answerErrorElem.textContent = '답변 내용을 입력하세요.'
        flag = true
    }

    if (flag) {
        return
    }

    const jsonData = {
        category,
        question,
        answer,
    };
    const response = await fetch(`http://localhost:8080/api/faqs/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(jsonData)
    })

    if (response.status === 204) {
        location.href = 'http://localhost:8080/faqs'
    }
    if (response.status === 400) {
        const data = await response.json()
        const { validation } = data
        if (validation && Object.keys(validation).length >= 1) {
            for (const [key, value] of Object.entries(validation)) {
                const field = document.getElementById(key + '-error');
                field.textContent = value
            }
        }
    }
}