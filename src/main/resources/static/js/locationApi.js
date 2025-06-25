export async function registerLocation(locationData) {
    const response = await fetch('/api/locations', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(locationData),
    });

    if (!response.ok) {
        throw new Error('주소 등록 실패');
    }

    return await response.json();
}

export async function updateLocation(locationId, locationData) {
    const response = await fetch(`/api/locations/${locationId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(locationData),
    });

    if (!response.ok) {
        throw new Error('주소 수정 실패');
    }

    return await response.json();
}