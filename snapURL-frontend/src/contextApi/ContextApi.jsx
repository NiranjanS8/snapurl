import { createContext, useContext, useState } from "react";

const ContextApi = createContext();

export const ContextProvider = ({ children }) => {
    const getToken = localStorage.getItem("JWT_TOKEN")
        ? JSON.parse(localStorage.getItem("JWT_TOKEN"))
        : null;
    const getRefreshToken = localStorage.getItem("JWT_REFRESH_TOKEN")
        ? JSON.parse(localStorage.getItem("JWT_REFRESH_TOKEN"))
        : null;

    const [token, setToken] = useState(getToken);
    const [refreshToken, setRefreshToken] = useState(getRefreshToken);

    const sendData = {
        token,
        setToken,
        refreshToken,
        setRefreshToken,
    };

    return <ContextApi.Provider value={sendData}>{children}</ContextApi.Provider>
};


export const useStoreContext = () => {
    const context = useContext(ContextApi);
    return context;
}
