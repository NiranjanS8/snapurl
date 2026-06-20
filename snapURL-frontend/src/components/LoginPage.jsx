import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import TextField from './TextField';
import { Link, useNavigate } from 'react-router-dom';
import api, { setApiAccessToken } from '../api/api';
import toast from 'react-hot-toast';
import { useStoreContext } from '../contextApi/ContextApi';

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

const LoginPage = () => {
    const navigate = useNavigate();
    const [loader, setLoader] = useState(false);
    const { setToken } = useStoreContext();

    const {
        register,
        handleSubmit,
        reset,
        formState: {errors}
    } = useForm({
        defaultValues: {
            email: "",
            password: "",
        },
        mode: "onTouched",
    });

    React.useEffect(() => {
        const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
        if (!googleClientId) {
            console.warn("VITE_GOOGLE_CLIENT_ID is not configured in the frontend .env file.");
            return;
        }

        const handleGoogleLoginResponse = async (response) => {
            setLoader(true);
            try {
                const { data: res } = await api.post(
                    "/api/auth/public/google",
                    { idToken: response.credential }
                );
                setApiAccessToken(res.accessToken);
                setToken(res.accessToken);
                toast.success("Login Successful with Google!");
                navigate("/dashboard");
            } catch (error) {
                toast.error(error?.response?.data?.message || "Google Login Failed!");
            } finally {
                setLoader(false);
            }
        };

        if (window.google) {
            window.google.accounts.id.initialize({
                client_id: googleClientId,
                callback: handleGoogleLoginResponse,
            });
            window.google.accounts.id.renderButton(
                document.getElementById("googleSignInDiv"),
                { theme: "filled_black", size: "large", shape: "pill", width: "100%" }
            );
        }
    }, [navigate, setToken]);

    const isSupportedEmail = (email) => {
        const normalizedEmail = email?.trim().toLowerCase();
        if (!normalizedEmail || !normalizedEmail.includes("@")) {
            return false;
        }
        const domain = normalizedEmail.split("@")[1];
        return ALLOWED_EMAIL_DOMAINS.has(domain);
    };

    const loginHandler = async (data) => {
        if (!isSupportedEmail(data.email)) {
            toast.error("Use a supported provider like Gmail, Proton Mail, iCloud Mail, or Outlook.");
            return;
        }

        setLoader(true);
        try {
            const { data: response } = await api.post(
                "/api/auth/public/login",
                {
                    email: data.email.trim().toLowerCase(),
                    password: data.password,
                }
            );
            setApiAccessToken(response.accessToken);
            setToken(response.accessToken);
            toast.success("Login Successful!");
            reset();
            navigate("/dashboard");
        } catch (error) {
            toast.error(error?.response?.data?.message || "Login Failed!")
        } finally {
            setLoader(false);
        }
    };

  return (
    <div className='min-h-[calc(100vh-64px)] flex items-center justify-center px-4 py-10'>
        <div className="grid w-full max-w-6xl gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="hidden rounded-2xl bg-[#1e1e1e] px-8 py-10 shadow-[0_18px_40px_rgba(0,0,0,0.22)] lg:block">
            <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">Welcome back</p>
            <h1 className="mt-4 text-5xl font-bold tracking-[-0.03em] text-white">
              Step back into your link command center.
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-[#B4A5A5]">
              Log in to manage short links, review click activity, and keep everything moving from one clean dashboard.
            </p>
          </div>
        <form onSubmit={handleSubmit(loginHandler)}
            className="w-full rounded-2xl bg-[#1e1e1e] px-5 py-8 shadow-[0_18px_40px_rgba(0,0,0,0.22)] sm:px-8">
            <p className="text-center text-xs font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">
              Account Access
            </p>
            <h1 className="mt-3 text-center text-3xl font-bold tracking-[-0.03em] text-white lg:text-4xl">
                Login Here
            </h1>

            <p className="mt-3 text-center text-sm text-[#B4A5A5]">
              Enter your details to continue to the dashboard.
            </p>

            <div className="mt-8 flex flex-col gap-4">
                <TextField
                    label="Email"
                    required
                    id="email"
                    type="email"
                    message="*Email is required"
                    placeholder="Type your email"
                    register={register}
                    errors={errors}
                />

                <TextField
                    label="Password"
                    required
                    id="password"
                    type="password"
                    message="*Password is required"
                    placeholder="Type your password"
                    register={register}
                    min={8}
                    errors={errors}
                />
            </div>

            <div className="mt-3 text-right">
                <Link
                    className="text-sm text-[#B4A5A5] transition-colors hover:text-white"
                    to="/forgot-password"
                >
                    Forgot password?
                </Link>
            </div>

            <button
                disabled={loader}
                type='submit'
                className='my-4 w-full rounded-full bg-[#301B3F] py-3 font-medium tracking-[0.01em] text-white hover:bg-[#3C415C] disabled:opacity-70'>
                {loader ? "Loading..." : "Login"}
            </button>

            <div className="flex items-center my-4">
                <div className="flex-grow border-t border-[#B4A5A5] opacity-20"></div>
                <span className="mx-4 text-xs uppercase tracking-wider text-[#B4A5A5] opacity-60">or</span>
                <div className="flex-grow border-t border-[#B4A5A5] opacity-20"></div>
            </div>

            <div className="w-full flex justify-center min-h-[44px]">
                <div id="googleSignInDiv" className="w-full"></div>
            </div>

            <p className='mt-6 text-center text-sm text-[#B4A5A5]'>
                Don't have an account? 
                <Link
                    className='font-medium hover:text-black'
                    to="/register">
                        <span className='text-btnColor'> SignUp</span>
                </Link>
            </p>
        </form>
        </div>
    </div>
  )
}

export default LoginPage
