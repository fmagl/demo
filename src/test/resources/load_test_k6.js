import http from 'k6/http';
import { check } from 'k6';

// Konfiguracja obciążenia - wymuszamy 3000 zapytań na sekundę przez 30 sekund
export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 3000,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 150,
            maxVUs: 500,
        },
    },
};

export default function () {
    // Losujemy numer kuponu od 1 do 100
    const randomCouponId = Math.floor(Math.random() * 100) + 1;
    const couponCode = `TEST${randomCouponId}`;
    
    // Generujemy losowego użytkownika, żeby nie wpaść w limit 1 użycia na osobę
    const randomUserId = `user-${Math.random().toString(36).substring(7)}`;

    const url = `http://localhost:8080/api/v1/coupons/${couponCode}/redeem`;
    const payload = JSON.stringify({
        userId: randomUserId
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Forwarded-For': '8.8.8.8'
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'is status 200 or 409': (r) => r.status === 200 || r.status === 409,
    });
}