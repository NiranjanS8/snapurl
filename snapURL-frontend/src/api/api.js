import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_URL || "http://localhost:9090",
});

api.interceptors.request.use((config) => {
    const storedToken = localStorage.getItem("JWT_TOKEN");
    const token = storedToken ? JSON.parse(storedToken) : null;

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error?.response?.status;

        if (status === 401 || status === 403) {
            localStorage.removeItem("JWT_TOKEN");

            if (!window.location.pathname.startsWith("/login")) {
                window.location.href = "/login";
            }
        }

        return Promise.reject(error);
    }
);

export default api;
