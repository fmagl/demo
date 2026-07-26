import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    constant_request_rate: {
      executor: 'constant-arrival-rate',
      rate: 3000,          
      timeUnit: '1s',      
      duration: '30s',     
      preAllocatedVUs: 50, 
      maxVUs: 500,         
    },
  },
};

export default function () {
  const payload = JSON.stringify({
    userId: `user-${Math.floor(Math.random() * 1000000)}`
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': '8.8.8.8', // Symulujemy poprawne IP dla kuponu z 'PL'
    },
  };


  const res = http.post('http://localhost:8080/api/v1/coupons/SUMMER10/redeem', payload, params);


  check(res, {
    'is status 200 or 409': (r) => r.status === 200 || r.status === 409,
  });
}
