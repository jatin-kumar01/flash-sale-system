// const BASE_URL = 'http://localhost:8080';
//
// // Helper to get saved JWT token
// export function getToken() {
//   return localStorage.getItem('token');
// }
//
// export function setToken(token) {
//   localStorage.setItem('token', token);
// }
//
// export function clearToken() {
//   localStorage.removeItem('token');
// }
//
// // Reusable fetch function
// export async function apiCall(endpoint, method = 'GET', body = null) {
//   const headers = {
//     'Content-Type': 'application/json',
//   };
//
//   const token = getToken();
//   if (token) {
//     headers['Authorization'] = `Bearer ${token}`;
//   }
//
//   const options = {
//     method,
//     headers,
//   };
//
//   if (body) {
//     options.body = JSON.stringify(body);
//   }
//
//   const response = await fetch(`${BASE_URL}${endpoint}`, options);
//   const data = await response.json();
//
//   if (!response.ok) {
//     throw new Error(data.message || 'Request failed');
//   }
//
//   return data;
// }
const BASE_URL = 'http://localhost:8080';

// Helper to get saved JWT token
export function getToken() {
  return localStorage.getItem('token');
}

export function setToken(token) {
  localStorage.setItem('token', token);
}

export function clearToken() {
  localStorage.removeItem('token');
}

// Reusable fetch function
export async function apiCall(endpoint, method = 'GET', body = null) {
  const headers = {
    'Content-Type': 'application/json',
  };

  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const options = {
    method,
    headers,
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, options);
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || 'Request failed');
  }

  return data;
}

// Authentication helpers
export async function registerUser(userData) {
  return await apiCall('/api/auth/register', 'POST', userData);
}

export async function loginUser(credentials) {
  const response = await apiCall('/api/auth/login', 'POST', credentials);
  const token = response.data?.token || response.token;
  if (token) {
    setToken(token);
  }
  return response;
}