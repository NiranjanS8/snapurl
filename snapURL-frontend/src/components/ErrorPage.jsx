import React from 'react'
import { FaExclamationTriangle } from 'react-icons/fa'
import { useNavigate } from 'react-router-dom'

const ErrorPage = ({ message }) => {
    const navigate = useNavigate();
  return (
    <div className="flex min-h-[calc(100vh-64px)] items-center justify-center px-6 py-10">
      <div className="w-full max-w-2xl rounded-2xl bg-[#1e1e1e] px-8 py-10 text-center shadow-[0_18px_40px_rgba(0,0,0,0.22)]">
        <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-2xl bg-[#301B3F] text-white">
          <FaExclamationTriangle className='text-4xl' />
        </div>
        <p className="mt-6 text-xs font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">Error State</p>
        <h1 className='mt-3 text-4xl font-bold tracking-[-0.03em] text-white'>
            Oops! Something went wrong.
        </h1>
        <p className='mx-auto mt-4 max-w-lg text-center text-base leading-7 text-[#B4A5A5]'>
            {message ? message : "An unexpected error has occured"}
        </p>
        <button onClick={() => {
            navigate("/");
        }}
        className='mt-8 rounded-full bg-[#301B3F] px-6 py-3 font-medium tracking-[0.01em] text-white hover:bg-[#3C415C]'
        >
            Go back to home
        </button>
      </div>
    </div>
  )
}

export default ErrorPage
