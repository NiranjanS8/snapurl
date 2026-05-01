import { createContext, useContext, useEffect, useState } from "react";
import api, { clearApiAccessToken, setApiAccessToken } from "../api/api";

const ContextApi = createContext();

export const ContextProvider = ({ children }) => {
    const [token, setTokenState] = useState(null);
    const [authReady, setAuthReady] = useState(false);

    const setToken = (nextToken) => {
        setTokenState(nextToken);
        if (nextToken) {
            setApiAccessToken(nextToken);
        } else {
            clearApiAccessToken();
        }
    };

    useEffect(() => {
        let mounted = true;
        api.post("/api/auth/public/refresh")
            .then(({ data }) => {
                if (mounted) {
                    setToken(data.accessToken);
                }
            })
            .catch(() => {
                if (mounted) {
                    setToken(null);
                }
            })
            .finally(() => {
                if (mounted) {
                    setAuthReady(true);
                }
            });

        return () => {
            mounted = false;
        };
    }, []);

    const sendData = {
        token,
        setToken,
        authReady,
    };

    return <ContextApi.Provider value={sendData}>{children}</ContextApi.Provider>
};


export const useStoreContext = () => {
    const context = useContext(ContextApi);
    return context;
}
