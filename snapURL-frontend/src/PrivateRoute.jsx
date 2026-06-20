import { Navigate } from "react-router-dom";
import { useStoreContext } from "./contextApi/ContextApi";

export default function PrivateRoute({ children, publicPage }) {
    const { token, authReady } = useStoreContext();

    if (!authReady) {
        return (
            <div className="flex min-h-[calc(100vh-128px)] items-center justify-center px-4">
                <div className="h-10 w-10 rounded-full border-2 border-[#B4A5A5]/30 border-t-white animate-spin" />
            </div>
        );
    }

    if (publicPage) {
        return token ? <Navigate to="/dashboard" /> : children;
    }

    return !token ? <Navigate to="/login" /> : children;
}
