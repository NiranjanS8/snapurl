import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_URL || "http://localhost:9090",
});

let refreshPromise = null;

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
    async (error) => {
        const status = error?.response?.status;
        const originalRequest = error.config;

        if ((status === 401 || status === 403) && !originalRequest?._retry) {
            const storedRefreshToken = localStorage.getItem("JWT_REFRESH_TOKEN");
            const refreshToken = storedRefreshToken ? JSON.parse(storedRefreshToken) : null;

            if (refreshToken && !originalRequest.url?.includes("/api/auth/public/refresh")) {
                originalRequest._retry = true;

                try {
                    if (!refreshPromise) {
                        refreshPromise = api.post("/api/auth/public/refresh", {
                            refreshToken,
                        });
                    }

                    const { data } = await refreshPromise;
                    refreshPromise = null;

                    localStorage.setItem("JWT_TOKEN", JSON.stringify(data.accessToken));
                    localStorage.setItem("JWT_REFRESH_TOKEN", JSON.stringify(data.refreshToken));
                    originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;

                    return api(originalRequest);
                } catch (refreshError) {
                    refreshPromise = null;
                }
            }
        }

        if (status === 401 || status === 403) {
            localStorage.removeItem("JWT_TOKEN");
            localStorage.removeItem("JWT_REFRESH_TOKEN");

            if (!window.location.pathname.startsWith("/login")) {
                window.location.href = "/login";
            }
        }

        return Promise.reject(error);
    }
);

export default api;
