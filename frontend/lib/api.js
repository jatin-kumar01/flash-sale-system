import axios from "axios";

const api = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
});

api.interceptors.request.use(
    (config) => {
        const accessToken = typeof window !== "undefined" ? sessionStorage.getItem("accessToken") : null;

        if (accessToken) {
            config.headers.Authorization = `Bearer ${accessToken}`;
        }

        try {
            const userStr = typeof window !== "undefined" ? sessionStorage.getItem("user") : null;
            if (userStr) {
                const u = JSON.parse(userStr);
                if (u && (u.id || u.userId)) {
                    config.headers["X-User-Id"] = String(u.id || u.userId);
                }
            }
        } catch (e) {
            // Ignore parse errors
        }

        if (config.data instanceof FormData) {
            delete config.headers["Content-Type"];
        } else if (!config.headers["Content-Type"]) {
            config.headers["Content-Type"] = "application/json";
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response?.status === 401) {
            console.warn("Authentication failed or token expired.");

            if (typeof window !== "undefined") {
                sessionStorage.removeItem("accessToken");
                sessionStorage.removeItem("refreshToken");
                sessionStorage.removeItem("user");

                if (window.location.pathname !== "/login") {
                    sessionStorage.setItem(
                        "authMessage",
                        "Your account has been disabled or deleted by the administrator."
                    );
                    window.location.href = "/login";
                }
            }
        }

        return Promise.reject(error);
    }
);

export default api;