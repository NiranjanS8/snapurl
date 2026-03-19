import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/api'
import toast from 'react-hot-toast'

const ALLOWED_EMAIL_DOMAINS = new Set([
    "gmail.com",
    "googlemail.com",
    "proton.me",
    "protonmail.com",
    "icloud.com",
    "me.com",
    "mac.com",
    "outlook.com",
    "hotmail.com",
    "live.com",
]);

const ForgotPasswordPage = () => {
    const [email, setEmail] = useState("");
    const [code, setCode] = useState("");
    const [password, setPassword] = useState("");
    const [loadingRequest, setLoadingRequest] = useState(false);
    const [loadingReset, setLoadingReset] = useState(false);
    const [requestCompleted, setRequestCompleted] = useState(false);
    const [issuedResetCode, setIssuedResetCode] = useState("");

    const isSupportedEmail = (value) => {
        const normalizedEmail = value?.trim().toLowerCase();
        if (!normalizedEmail || !normalizedEmail.includes("@")) {
            return false;
        }
        return ALLOWED_EMAIL_DOMAINS.has(normalizedEmail.split("@")[1]);
    };

    const requestResetHandler = async (event) => {
        event.preventDefault();

        if (!isSupportedEmail(email)) {
            toast.error("Use a supported provider like Gmail, Proton Mail, iCloud Mail, or Outlook.");
            return;
        }

        setLoadingRequest(true);

        try {
            const { data } = await api.post("/api/auth/public/forgot-password", { email });
            setRequestCompleted(true);
            setIssuedResetCode(data.resetCode || "");
            toast.success(data.message || "If an account exists, a reset code has been prepared.");
        } catch (error) {
            toast.error(error?.response?.data?.message || "Couldn't start the reset flow.");
        } finally {
            setLoadingRequest(false);
        }
    };

    const resetPasswordHandler = async (event) => {
        event.preventDefault();

        if (password.trim().length < 8) {
            toast.error("Password must be at least 8 characters long.");
            return;
        }

        setLoadingReset(true);

        try {
            const activeCode = code || issuedResetCode;
            await api.post("/api/auth/public/reset-password", {
                code: activeCode,
                password,
            });
            toast.success("Password reset successfully.");
            setPassword("");
            setCode("");
            setIssuedResetCode("");
        } catch (error) {
            toast.error(error?.response?.data?.message || "Couldn't reset the password.");
        } finally {
            setLoadingReset(false);
        }
    };

    return (
        <div className='min-h-[calc(100vh-64px)] flex items-center justify-center px-4 py-10'>
            <div className="grid w-full max-w-6xl gap-8 lg:grid-cols-[0.95fr_1.05fr]">
                <div className="hidden rounded-2xl bg-[#1e1e1e] px-8 py-10 shadow-[0_18px_40px_rgba(0,0,0,0.22)] lg:block">
                    <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">Access recovery</p>
                    <h1 className="mt-4 text-5xl font-bold tracking-[-0.03em] text-white">
                        Reset access without leaving your workflow behind.
                    </h1>
                    <p className="mt-5 max-w-xl text-base leading-7 text-[#B4A5A5]">
                        Request a one-time reset code, set a new password, and get back into your link dashboard with the same calm flow as the rest of the product.
                    </p>
                </div>

                <div className="rounded-2xl bg-[#1e1e1e] px-5 py-8 shadow-[0_18px_40px_rgba(0,0,0,0.22)] sm:px-8">
                    <p className="text-center text-xs font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">
                        Account Recovery
                    </p>
                    <h1 className="mt-3 text-center text-3xl font-bold tracking-[-0.03em] text-white lg:text-4xl">
                        Reset your password
                    </h1>
                    <p className="mt-3 text-center text-sm text-[#B4A5A5]">
                        Request a one-time code first, then choose a new password.
                    </p>

                    <div className="mt-8 grid gap-6">
                        <form onSubmit={requestResetHandler} className="rounded-2xl bg-[#181818] p-5 shadow-[0_14px_30px_rgba(0,0,0,0.18)]">
                            <div className="flex items-center justify-between gap-3">
                                <div>
                                    <h2 className="text-base font-semibold text-white">Request reset code</h2>
                                    <p className="mt-1 text-sm text-[#B4A5A5]">Use your account email to generate a one-time code.</p>
                                </div>
                            </div>
                            <div className="mt-4">
                                <input
                                    value={email}
                                    onChange={(event) => setEmail(event.target.value)}
                                    type="email"
                                    placeholder="Email"
                                    className="h-12 w-full rounded-2xl bg-[#151515] px-4 text-sm text-white shadow-[0_10px_24px_rgba(0,0,0,0.14)] outline-none placeholder:text-[#9d8e8e] focus:ring-2 focus:ring-[#3C415C]/60"
                                />
                            </div>
                            <button
                                disabled={loadingRequest}
                                type="submit"
                                className="mt-4 w-full rounded-full bg-[#301B3F] py-3 text-sm font-medium text-white transition-colors hover:bg-[#3C415C] disabled:opacity-70"
                            >
                                {loadingRequest ? "Preparing..." : "Send reset code"}
                            </button>

                            {issuedResetCode && (
                                <div className="mt-4 rounded-2xl bg-[#151515] px-4 py-3 text-sm text-[#B4A5A5]">
                                    <p className="font-medium text-white">Reset code</p>
                                    <p className="mt-2 break-all font-mono text-xs text-[#B4A5A5]">{issuedResetCode}</p>
                                </div>
                            )}

                            {requestCompleted && !issuedResetCode && (
                                <div className="mt-4 rounded-2xl bg-[#151515] px-4 py-3 text-sm text-[#B4A5A5]">
                                    Check your inbox for the one-time reset code.
                                </div>
                            )}
                        </form>

                        <form onSubmit={resetPasswordHandler} className="rounded-2xl bg-[#181818] p-5 shadow-[0_14px_30px_rgba(0,0,0,0.18)]">
                            <div>
                                <h2 className="text-base font-semibold text-white">Set a new password</h2>
                                <p className="mt-1 text-sm text-[#B4A5A5]">Enter the code and choose a password with at least 8 characters.</p>
                            </div>
                            <div className="mt-4 grid gap-3">
                                <input
                                    value={code}
                                    onChange={(event) => setCode(event.target.value)}
                                    type="text"
                                    placeholder="Reset code"
                                    className="h-12 w-full rounded-2xl bg-[#151515] px-4 text-sm text-white shadow-[0_10px_24px_rgba(0,0,0,0.14)] outline-none placeholder:text-[#9d8e8e] focus:ring-2 focus:ring-[#3C415C]/60"
                                />
                                <input
                                    value={password}
                                    onChange={(event) => setPassword(event.target.value)}
                                    type="password"
                                    placeholder="New password"
                                    className="h-12 w-full rounded-2xl bg-[#151515] px-4 text-sm text-white shadow-[0_10px_24px_rgba(0,0,0,0.14)] outline-none placeholder:text-[#9d8e8e] focus:ring-2 focus:ring-[#3C415C]/60"
                                />
                            </div>
                            <button
                                disabled={loadingReset}
                                type="submit"
                                className="mt-4 w-full rounded-full bg-[#301B3F] py-3 text-sm font-medium text-white transition-colors hover:bg-[#3C415C] disabled:opacity-70"
                            >
                                {loadingReset ? "Updating..." : "Update password"}
                            </button>
                        </form>
                    </div>

                    <p className='mt-6 text-center text-sm text-[#B4A5A5]'>
                        Back to
                        <Link className='font-medium' to="/login">
                            <span className='text-btnColor'> Login</span>
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    )
}

export default ForgotPasswordPage
