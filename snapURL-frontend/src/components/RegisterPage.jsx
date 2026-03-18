import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import TextField from './TextField';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/api';
import toast from 'react-hot-toast';

const RegisterPage = () => {
    const navigate = useNavigate();
    const [loader, setLoader] = useState(false);

    const {
        register,
        handleSubmit,
        reset,
        formState: {errors}
    } = useForm({
        defaultValues: {
            username: "",
            email: "",
            password: "",
        },
        mode: "onTouched",
    });

    const registerHandler = async (data) => {
        setLoader(true);
        try {
            const { data: response } = await api.post(
                "/api/auth/public/register",
                data
            );
            reset();
            navigate("/login");
            toast.success("Registeration Successful!")
        } catch (error) {
            console.log(error);
            toast.error("Registeration Failed!")
        } finally {
            setLoader(false);
        }
    };

  return (
    <div className='min-h-[calc(100vh-64px)] flex items-center justify-center px-4 py-10'>
        <div className="grid w-full max-w-6xl gap-8 lg:grid-cols-[0.9fr_1.1fr]">
        <form onSubmit={handleSubmit(registerHandler)}
            className="w-full rounded-2xl bg-[#1e1e1e] px-5 py-8 shadow-[0_18px_40px_rgba(0,0,0,0.22)] sm:px-8">
            <p className="text-center text-xs font-black uppercase tracking-[0.24em] text-[#B4A5A5]">
              Get Started
            </p>
            <h1 className="mt-3 text-center text-3xl font-black tracking-tight text-white lg:text-4xl">
                Register Here
            </h1>

            <p className="mt-3 text-center text-sm text-[#B4A5A5]">
              Create your account and start managing links with the new dashboard.
            </p>

            <div className="mt-8 flex flex-col gap-4">
                <TextField
                    label="UserName"
                    required
                    id="username"
                    type="text"
                    message="*Username is required"
                    placeholder="Type your username"
                    register={register}
                    errors={errors}
                />

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
                    min={6}
                    errors={errors}
                />
            </div>

            <button
                disabled={loader}
                type='submit'
                className='my-4 w-full rounded-full bg-[#301B3F] py-3 font-semibold text-white hover:bg-[#3C415C] disabled:opacity-70'>
                {loader ? "Loading..." : "Register"}
            </button>

            <p className='mt-6 text-center text-sm text-[#B4A5A5]'>
                Already have an account? 
                <Link
                    className='font-semibold hover:text-black'
                    to="/login">
                        <span className='text-btnColor'> Login</span>
                </Link>
            </p>
        </form>
          <div className="hidden rounded-2xl bg-[#1e1e1e] px-8 py-10 text-white shadow-[0_18px_40px_rgba(0,0,0,0.22)] lg:block">
            <p className="text-sm font-black uppercase tracking-[0.28em] text-[#B4A5A5]">New workspace</p>
            <h1 className="mt-4 text-5xl font-black tracking-tight">
              Create an account and bring your links into focus.
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-[#B4A5A5]">
              Register once, then shorten links, watch performance trends, and manage everything from a cleaner, more modern analytics experience.
            </p>
          </div>
        </div>
    </div>
  )
}

export default RegisterPage
