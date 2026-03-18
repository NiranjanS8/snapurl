import { useNavigate } from "react-router-dom";
import React, { useState } from "react";
import { motion } from "framer-motion";
import toast from "react-hot-toast";

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
    <div className="min-h-[calc(100vh-64px)] lg:px-14 sm:px-8 px-4">
      {showLimitDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-btnColor">
              Free Limit Reached
            </p>
            <h2 className="mt-3 text-2xl font-bold text-slate-800">
              You have used all 3 free guest shortens
            </h2>
            <p className="mt-3 text-sm text-slate-600">
              Log in to keep creating short links, manage them from your dashboard, and view analytics.
            </p>
            <div className="mt-5 flex gap-3">
              <button
                onClick={() => navigate("/login")}
                className="rounded-md bg-custom-gradient px-5 py-2 font-semibold text-white"
              >
                Login
              </button>
              <button
                onClick={() => setShowLimitDialog(false)}
                className="rounded-md border border-slate-300 px-5 py-2 font-semibold text-slate-700"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
      <div className="flex flex-col items-center pt-16 pb-6 text-center">
        <div className="w-full max-w-4xl">
          <motion.h1
            initial={{ opacity: 0, y: -80 }}
            whileInView={{
              opacity: 1,
              y: 0,
            }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
            className="font-bold font-roboto text-slate-800 md:text-5xl sm:text-4xl text-3xl md:leading-[55px] sm:leading-[45px] leading-10"
          >
            SnapURL - Snap. Share. Go.
          </motion.h1>
          <p className="mx-auto mt-5 max-w-2xl text-slate-700 text-sm sm:text-base">
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
          className="mt-8 w-full max-w-2xl"
        >
          <div className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6 shadow-xl shadow-slate-200/70">
            <h2 className="text-2xl font-bold text-slate-800">
              Create Short Links Instantly
            </h2>
            <p className="mt-3 text-sm text-slate-600">
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
                className={`w-full flex-1 rounded-md border px-3 py-3 text-slate-800 outline-none focus:border-btnColor ${
                  cardError ? "border-red-500" : "border-slate-300"
                }`}
              />
              <button
                onClick={createShortLinkHandler}
                disabled={loading}
                className="rounded-md bg-custom-gradient px-6 py-3 font-semibold text-white disabled:opacity-70 sm:min-w-[160px]"
              >
                {loading ? "Creating..." : "Snap It!"}
              </button>
            </div>
            {cardError && (
              <p className="mt-3 text-left text-sm font-semibold text-red-600">
                {cardError}
              </p>
            )}
            {shortUrl && (
              <div className="mt-4 rounded-xl bg-slate-50 p-4 text-left">
                <p className="break-all text-lg font-bold text-linkColor">
                  {shortUrlLabel}
                </p>
                <div className="mt-3 flex gap-3">
                  <button
                    onClick={() => navigator.clipboard.writeText(shortUrl)}
                    className="rounded-md border border-btnColor px-4 py-2 font-semibold text-btnColor"
                  >
                    Copy
                  </button>
                  <a
                    href={shortUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="rounded-md bg-custom-gradient px-4 py-2 font-semibold text-white"
                  >
                    Open
                  </a>
                </div>
              </div>
            )}
          </div>
        </motion.div>
      </div>
      <div className="sm:pt-12 pt-7">
        <motion.p
          initial={{ opacity: 0, y: 50 }}
          whileInView={{
            opacity: 1,
            y: 0,
          }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="text-slate-800 font-roboto font-bold lg:w-[60%] md:w-[70%] sm:w-[80%] mx-auto text-3xl text-center"
        >
          Built for developers, teams, and everyday users.{" "}
        </motion.p>
        <div className="pt-4 pb-7 grid lg:gap-7 gap-4 xl:grid-cols-4 lg:grid-cols-3 sm:grid-cols-2 grid-cols-1 mt-4">
          <Card
            title="Simple URL Shortening"
            desc="Experience the ease of creating short, memorable URLs in just a few clicks. Our intuitive interface and quick setup process ensure you can start shortening URLs without any hassle."
          />
          <Card
            title="Powerful Analytics"
            desc="Gain insights into your link performance with our comprehensive analytics dashboard. Track clicks, geographical data, and referral sources to optimize your marketing strategies."
          />
          <Card
            title="Enhanced Security"
            desc="Rest assured with our robust security measures. All shortened URLs are protected with advanced encryption, ensuring your data remains safe and secure."
          />
          <Card
            title="Fast and Reliable"
            desc="Enjoy lightning-fast redirects and high uptime with our reliable infrastructure. Your shortened URLs will always be available and responsive, ensuring a seamless experience for your users."
          />
        </div>
      </div>
    </div>
  );
};

export default LandingPage;
