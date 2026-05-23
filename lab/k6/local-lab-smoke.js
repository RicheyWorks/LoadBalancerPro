import http from 'k6/http';
import { check } from 'k6';

const DEFAULT_BASE_URL = 'http://127.0.0.1:8080';
const baseUrl = __ENV.LOCAL_LAB_BASE_URL || DEFAULT_BASE_URL;

export const options = {
  vus: 1,
  iterations: 3,
};

export default function () {
  // Local-lab smoke walkthrough only; not load, stress, or benchmark evidence.
  const response = http.get(`${baseUrl}/actuator/health`);

  check(response, {
    'local endpoint returns 200': (r) => r.status === 200,
    'local endpoint returns a body': (r) => Boolean(r.body),
  });
}
