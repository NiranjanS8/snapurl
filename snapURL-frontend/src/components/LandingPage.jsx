import { useNavigate } from "react-router-dom";
import React, { useState } from "react";
import { motion } from "framer-motion";
import toast from "react-hot-toast";
import { FaChartLine, FaLink, FaLock, FaRocket } from "react-icons/fa";

import Card from "./Card";
import { useStoreContext } from "../contextApi/ContextApi";
import api from "../api/api";

const GUEST_LIMIT = 3;
const GUEST_STORAGE_KEY = "SNAPURL_GUEST_SHORTENS";

const LandingPage = () => {
  const navigate = useNavigate();
  const { token } = useStoreContext();
  const [originalUrl, setOriginalUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [shortUrl, setShortUrl] = useState("");
  const [cardError, setCardError] = useState("");
  const [showLimitDialog, setShowLimitDialog] = useState(false);

  const guestUsageCount = Number(localStorage.getItem(GUEST_STORAGE_KEY) || "0");
  const shortUrlLabel = shortUrl.replace(/^https?:\/\//, "");

  const createShortLinkHandler = async () => {
    if (!originalUrl.trim()) {
      setCardError('We\'ll need a valid URL, like "super-long-link.com/shorten-it"');
      return;
    }

    if (!token && guestUsageCount >= GUEST_LIMIT) {
      setShowLimitDialog(true);
      return;
    }

    setLoading(true);
    setCardError("");
    try {
      const endpoint = token ? "/api/urls/shorten" : "/api/urls/public/shorten";
      const { data } = await api.post(endpoint, { originalUrl });
      const generatedUrl = `${import.meta.env.VITE_REACT_FRONT_END_URL}/s/${data.shortUrl}`;

      setShortUrl(generatedUrl);
      setOriginalUrl("");

      if (!token) {
        localStorage.setItem(GUEST_STORAGE_KEY, String(guestUsageCount + 1));
      }

      navigator.clipboard.writeText(generatedUrl).then(() => {
        toast.success("Short URL copied to clipboard");
      });
    } catch (error) {
      setCardError(
        error?.response?.data?.message ||
          'We\'ll need a valid URL, like "super-long-link.com/shorten-it"'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)] px-4 lg:px-14 sm:px-8">
      {showLimitDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4">
          <div className="w-full max-w-md rounded-2xl bg-[#1d1b24] p-6 shadow-[0_20px_50px_rgba(0,0,0,0.32)]">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#B4A5A5]">
              Free Limit Reached
            </p>
            <h2 className="mt-3 text-2xl font-bold text-white">
              You have used all 3 free guest shortens
            </h2>
            <p className="mt-3 text-sm text-[#B4A5A5]">
              Log in to keep creating short links, manage them from your dashboard, and view analytics.
            </p>
            <div className="mt-5 flex gap-3">
              <button
                onClick={() => navigate("/login")}
                className="rounded-md bg-[linear-gradient(135deg,#301B3F,#3C415C)] px-5 py-2 font-semibold text-white"
              >
                Login
              </button>
              <button
                onClick={() => setShowLimitDialog(false)}
                className="rounded-md bg-[#151515] px-5 py-2 font-semibold text-[#B4A5A5] shadow-[0_10px_24px_rgba(0,0,0,0.24)]"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
      <section className="pt-16 pb-10">
        <div className="flex flex-col items-center text-center">
        <div className="w-full max-w-4xl">
          <motion.h1
            initial={{ opacity: 0, y: -80 }}
            whileInView={{
              opacity: 1,
              y: 0,
            }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
            className="font-bold font-roboto text-white md:text-6xl sm:text-5xl text-4xl md:leading-[68px] sm:leading-[56px] leading-[46px] tracking-tight"
          >
            SnapURL - Snap. Share. Go.
          </motion.h1>
          <p className="mx-auto mt-6 max-w-2xl text-sm font-medium leading-7 text-[#B4A5A5] sm:text-base">
            SnapURL transforms lengthy URLs into short, convenient links for faster sharing.
          </p>
        </div>
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{
            opacity: 1,
            y: 0,
          }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
            className="mt-10 w-full max-w-3xl"
        >
          <div className="rounded-2xl bg-[#1e1e1e] p-6 shadow-[0_18px_40px_rgba(0,0,0,0.22)] sm:p-7">
            <h2 className="text-lg font-semibold tracking-tight text-white sm:text-xl">
              Create Short Links Instantly
            </h2>
            <p className="mt-3 text-sm font-medium leading-6 text-[#B4A5A5]">
              {!token && " Sign up to manage links and view analytics."}
            </p>
            <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center">
              <input
                type="url"
                value={originalUrl}
                onChange={(event) => {
                  setOriginalUrl(event.target.value);
                  if (cardError) {
                  setCardError("");
                  }
                }}
                placeholder="Paste URL to snap"
                className={`w-full flex-1 rounded-2xl bg-[#151515] px-4 py-4 text-white outline-none shadow-[0_10px_24px_rgba(0,0,0,0.18)] transition-all duration-200 focus:ring-2 focus:ring-[#B4A5A5]/55 ${
                  cardError ? "ring-2 ring-red-500/70" : ""
                }`}
              />
              <button
                onClick={createShortLinkHandler}
                disabled={loading}
                className="rounded-2xl bg-[#301B3F] px-6 py-4 text-[17px] font-semibold text-white transition-all duration-200 hover:bg-[#3C415C] disabled:opacity-70 sm:min-w-[160px]"
              >
                {loading ? "Creating..." : "Snap It!"}
              </button>
            </div>
            {cardError && (
              <p className="mt-3 text-left text-sm font-semibold text-red-400">
                {cardError}
              </p>
            )}
            {shortUrl && (
              <div className="mt-5 rounded-2xl bg-[#151515] p-5 text-center shadow-[0_12px_28px_rgba(0,0,0,0.22)]">
                <p className="break-all text-xl font-extrabold tracking-tight text-white sm:text-2xl">
                  {shortUrlLabel}
                </p>
                <div className="mt-5 flex justify-center gap-3">
                  <button
                    onClick={() => navigator.clipboard.writeText(shortUrl)}
                    className="rounded-xl bg-[#151515] px-4 py-2 font-semibold text-[#B4A5A5] shadow-[0_8px_20px_rgba(0,0,0,0.2)] hover:text-white"
                  >
                    Copy
                  </button>
                  <a
                    href={shortUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="rounded-xl bg-[#301B3F] px-4 py-2 font-semibold text-white hover:bg-[#3C415C]"
                  >
                    Open
                  </a>
                </div>
              </div>
            )}
          </div>
        </motion.div>
      </div>
      </section>
      <section className="pt-10 pb-8">
        <motion.p
          initial={{ opacity: 0, y: 50 }}
          whileInView={{
            opacity: 1,
            y: 0,
          }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="mx-auto text-center font-roboto text-3xl font-black tracking-tight text-white lg:w-[60%] md:w-[70%] sm:w-[80%]"
        >
          Built for developers, teams, and everyday users.{" "}
        </motion.p>
        <p className="mx-auto mt-4 max-w-2xl text-center text-sm font-medium leading-7 text-[#B4A5A5]">
          A cleaner experience for shortening, sharing, and understanding how your links perform.
        </p>
        <div className="mt-6 grid gap-5 pb-7 pt-4 xl:grid-cols-4 lg:grid-cols-3 sm:grid-cols-2 grid-cols-1">
          <Card
            title="Simple URL Shortening"
            desc="Experience the ease of creating short, memorable URLs in just a few clicks. Our intuitive interface and quick setup process ensure you can start shortening URLs without any hassle."
            icon={FaLink}
            accentClass="text-[#B4A5A5]"
            accentBgClass="bg-transparent"
          />
          <Card
            title="Powerful Analytics"
            desc="Gain insights into your link performance with our comprehensive analytics dashboard. Track clicks, geographical data, and referral sources to optimize your marketing strategies."
            icon={FaChartLine}
            accentClass="text-[#B4A5A5]"
            accentBgClass="bg-transparent"
          />
          <Card
            title="Enhanced Security"
            desc="Rest assured with our robust security measures. All shortened URLs are protected with advanced encryption, ensuring your data remains safe and secure."
            icon={FaLock}
            accentClass="text-[#B4A5A5]"
            accentBgClass="bg-transparent"
          />
          <Card
            title="Fast and Reliable"
            desc="Enjoy lightning-fast redirects and high uptime with our reliable infrastructure. Your shortened URLs will always be available and responsive, ensuring a seamless experience for your users."
            icon={FaRocket}
            accentClass="text-[#B4A5A5]"
            accentBgClass="bg-transparent"
          />
        </div>
      </section>
    </div>
  );
};

export default LandingPage;
