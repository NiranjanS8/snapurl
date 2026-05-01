import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_URL || "http://localhost:9090",
    withCredentials: true,
});

let refreshPromise = null;
let accessToken = null;

export const setApiAccessToken = (token) => {
    accessToken = token;
};

export const clearApiAccessToken = () => {
    accessToken = null;
};

api.interceptors.request.use((config) => {
    if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
});

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const status = error?.response?.status;
        const originalRequest = error.config;
        const isRefreshRequest = originalRequest?.url?.includes("/api/auth/public/refresh");

        if ((status === 401 || status === 403) && !originalRequest?._retry) {
            if (!isRefreshRequest) {
                originalRequest._retry = true;

                try {
                    if (!refreshPromise) {
                        refreshPromise = api.post("/api/auth/public/refresh");
                    }

                    const { data } = await refreshPromise;
                    refreshPromise = null;

                    setApiAccessToken(data.accessToken);
                    originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;

                    return api(originalRequest);
                } catch (refreshError) {
                    refreshPromise = null;
                }
            }
        }

        if ((status === 401 || status === 403) && !isRefreshRequest) {
            clearApiAccessToken();

            if (!window.location.pathname.startsWith("/login")) {
                window.location.href = "/login";
            }
        }

        return Promise.reject(error);
    }
);

export default api;
