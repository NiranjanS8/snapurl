import React, { useEffect } from 'react'
import { useParams } from 'react-router-dom'

const ShortenUrlPage = () => {
    const { url } = useParams();

    useEffect(() => {
        if (url) {
            window.location.href = import.meta.env.VITE_BACKEND_URL + `/${url}`;
        }
    }, [url]);
  return (
    <div className="flex min-h-screen items-center justify-center px-6">
      <div className="rounded-2xl bg-[#1e1e1e] px-8 py-8 text-center shadow-[0_18px_40px_rgba(0,0,0,0.22)]">
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">Redirecting</p>
        <p className="mt-3 text-lg font-medium tracking-[-0.01em] text-white">Taking you to your destination...</p>
      </div>
    </div>
  );
}

export default ShortenUrlPage
