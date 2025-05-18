const createNotice = async () => {
    const titleErrorElem = document.getElementById('title-error')
    const contentErrorElem = document.getElementById('content-error')
    titleErrorElem.textContent = ''
    contentErrorElem.textContent = ''

    const title = document.getElementById('title').value.trim()
    const content = document.getElementById('content').value.trim()

    let flag = false
    if (!title || title === '') {
        titleErrorElem.textContent = '제목을 입력해주세요.'
        flag = true
    }
    if (!content || content === '') {
        contentErrorElem.textContent = '내용을 입력해주세요.'
        flag = true
    }

    if (flag) {
        return
    }

    const jsonData = {
        title,
        content,
    };
    const response = await fetch('http://localhost:8080/api/notices', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(jsonData)
    })

    if (response.status === 201) {
        location.href = 'http://localhost:8080/notices'
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

const updateNotice = async (id) => {
    const result = confirm("정말 수정하시겠습니까?");
    if (!result) {
        return
    }

    const titleErrorElem = document.getElementById('title-error')
    const contentErrorElem = document.getElementById('content-error')
    titleErrorElem.textContent = ''
    contentErrorElem.textContent = ''

    const title = document.getElementById('title').value.trim()
    const content = document.getElementById('content').value.trim()

    let flag = false
    if (!title || title === '') {
        titleErrorElem.textContent = '제목을 입력해주세요.'
        flag = true
    }
    if (!content || content === '') {
        contentErrorElem.textContent = '내용을 입력해주세요.'
        flag = true
    }

    if (flag) {
        return
    }

    const jsonData = {
        title,
        content,
    }
    const response = await fetch(`http://localhost:8080/api/notices/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(jsonData)
    })

    if (response.status === 204) {
        location.href = `http://localhost:8080/notices/${id}`
    }
    if (response.status === 400) {
        const data = await response.json()
        const { validation } = data
        if (validation && Object.keys(validation).length >= 1) {
            for (const [key, value] of Object.entries(validation)) {
                let field = document.getElementById(key + '-error')
                field.innerText = value
            }
        }
    }
}

const removeNotice = async (id) => {
    const result = confirm('정말 삭제하시겠습니까?')
    if (!result) {
        return
    }

    const response = await fetch(`http://localhost:8080/api/notices/${id}`, {
        method: 'DELETE',
    })
    if (response.status === 204) {
        location.href = 'http://localhost:8080/notices'
    }
}