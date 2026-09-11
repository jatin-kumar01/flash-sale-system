// import axios from "axios";
//
// console.log(
//     "API Base URL:",
//     process.env.NEXT_PUBLIC_API_BASE_URL
// );
//
// const api = axios.create({
//     baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
//     headers: {
//         "Content-Type": "application/json",
//     },
// });
//
// export default api;

import axios from "axios";

const api = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

// Request interceptor
api.interceptors.request.use(
    (config) => {
        const accessToken = localStorage.getItem("accessToken");

        if (accessToken) {
            config.headers.Authorization = `Bearer ${accessToken}`;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response?.status === 401) {
            console.warn("Authentication failed or token expired.");

            localStorage.removeItem("accessToken");

            window.location.href = "/login";
        }

        return Promise.reject(error);
    }
);

export default api;