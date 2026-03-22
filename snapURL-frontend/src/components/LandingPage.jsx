import { useNavigate } from "react-router-dom";
import React, { useState } from "react";
import { motion } from "framer-motion";
import toast from "react-hot-toast";
import { FaChartLine, FaLink, FaLock, FaRocket } from "react-icons/fa";
import { LiaCheckSolid } from "react-icons/lia";

import Card from "./Card";
import { useStoreContext } from "../contextApi/ContextApi";
import api from "../api/api";
import { buildShortLink } from "../utils/publicUrl";

const GUEST_LIMIT = 3;
const GUEST_STORAGE_KEY = "SNAPURL_GUEST_SHORTENS";
const RESERVED_ALIASES = new Set(["api", "admin", "login", "register", "signup", "auth", "public", "dashboard", "error", "s"]);
const STRICT_URL_PATTERN =
  /^(https?:\/\/)?(?=.{4,253}$)(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,24}(?::\d{2,5})?(?:\/[^\s]*)?$/;

const LandingPage = () => {
  const navigate = useNavigate();
  const { token } = useStoreContext();
  const [originalUrl, setOriginalUrl] = useState("");
  const [customAlias, setCustomAlias] = useState("");
  const [useCustomAlias, setUseCustomAlias] = useState(false);
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

    if (!STRICT_URL_PATTERN.test(originalUrl.trim())) {
      setCardError('We\'ll need a valid URL, like "super-long-link.com/shorten-it"');
      return;
    }

    if (useCustomAlias && customAlias.trim() && !/^[A-Za-z0-9_-]{3,32}$/.test(customAlias.trim())) {
      setCardError("Custom alias can only use letters, numbers, hyphens, or underscores and must be 3 to 32 characters long.");
      return;
    }

    if (useCustomAlias && customAlias.trim() && RESERVED_ALIASES.has(customAlias.trim().toLowerCase())) {
      setCardError("That alias is reserved. Please choose another one.");
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
      const payload = {
        originalUrl,
        customAlias: useCustomAlias ? customAlias.trim() || undefined : undefined,
      };
      const { data } = await api.post(endpoint, payload);
      const generatedUrl = buildShortLink(data.shortUrl);

      setShortUrl(generatedUrl);
      setOriginalUrl("");
      setCustomAlias("");
      setUseCustomAlias(false);

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
            <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">
              Free Limit Reached
            </p>
            <h2 className="mt-3 text-2xl font-semibold text-white">
              You have used all 3 free guest shortens
            </h2>
            <p className="mt-3 text-sm text-[#B4A5A5]">
              Log in to keep creating short links, manage them from your dashboard, and view analytics.
            </p>
            <div className="mt-5 flex gap-3">
              <button
                onClick={() => navigate("/login")}
                className="rounded-md bg-[linear-gradient(135deg,#301B3F,#3C415C)] px-5 py-2 font-medium tracking-[0.01em] text-white"
              >
                Login
              </button>
              <button
                onClick={() => setShowLimitDialog(false)}
                className="rounded-md bg-[#151515] px-5 py-2 font-medium tracking-[0.01em] text-[#B4A5A5] shadow-[0_10px_24px_rgba(0,0,0,0.24)]"
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
            className="text-4xl font-bold tracking-[-0.03em] text-white sm:text-5xl md:text-6xl md:leading-[68px] sm:leading-[56px] leading-[46px]"
          >
            SnapURL - Snap. Share. Go.
          </motion.h1>
          <p className="mx-auto mt-6 max-w-2xl text-sm leading-7 text-[#B4A5A5] sm:text-base">
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
            <h2 className="text-lg font-semibold tracking-[-0.02em] text-white sm:text-xl">
              Create Short Links Instantly
            </h2>
            <p className="mt-3 text-sm leading-6 text-[#B4A5A5]">
              {!token && " Sign up to manage links and view analytics."}
            </p>
            <div className="mt-5 flex flex-col gap-3 lg:flex-row lg:items-center">
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
                className={`h-14 w-full rounded-2xl bg-[#151515] px-5 text-[15px] text-white outline-none shadow-[0_10px_24px_rgba(0,0,0,0.18)] transition-all duration-200 focus:ring-2 focus:ring-[#B4A5A5]/55 lg:flex-[1.65] ${
                  cardError ? "ring-2 ring-red-500/70" : ""
                }`}
              />
              <div
                className={`transition-all duration-300 ease-out ${
                  useCustomAlias
                    ? "opacity-100 lg:w-[220px] lg:translate-x-0"
                    : "pointer-events-none opacity-0 lg:w-0 lg:-translate-x-2"
                }`}
              >
                <div className="relative w-full py-1 lg:w-[220px]">
                  <input
                    type="text"
                    value={customAlias}
                    onChange={(event) => {
                      setCustomAlias(event.target.value);
                      if (cardError) {
                        setCardError("");
                      }
                    }}
                    placeholder="Custom alias"
                    className="h-14 w-full rounded-2xl bg-[#17171a] px-4 text-[14px] text-white/88 outline-none shadow-[0_10px_24px_rgba(0,0,0,0.16)] transition-all duration-200 focus:ring-2 focus:ring-[#B4A5A5]/45"
                  />
                </div>
              </div>
              <button
                onClick={createShortLinkHandler}
                disabled={loading}
                className="h-14 rounded-2xl bg-[#301B3F] px-6 text-[17px] font-medium tracking-[0.01em] text-white transition-all duration-200 hover:bg-[#3C415C] disabled:opacity-70 lg:min-w-[158px]"
              >
                {loading ? "Creating..." : "Snap It!"}
              </button>
            </div>
            <div className="mt-3 flex flex-col gap-2 text-left sm:flex-row sm:items-center sm:justify-between">
              <label className="inline-flex w-fit cursor-pointer items-center gap-3 text-sm text-[#B4A5A5]">
                <input
                  type="checkbox"
                  checked={useCustomAlias}
                  onChange={(event) => {
                    setUseCustomAlias(event.target.checked);
                    if (!event.target.checked) {
                      setCustomAlias("");
                    }
                    if (cardError) {
                      setCardError("");
                    }
                  }}
                  className="peer sr-only"
                />
                <span className="flex h-5 w-5 items-center justify-center rounded-[6px] border border-white/10 bg-[#151515] text-transparent shadow-[0_6px_16px_rgba(0,0,0,0.16)] transition-all duration-200 peer-checked:border-[#3C415C] peer-checked:bg-[#301B3F] peer-checked:text-white peer-hover:border-white/15 peer-focus-visible:ring-2 peer-focus-visible:ring-[#B4A5A5]/40">
                  <LiaCheckSolid className="text-sm" />
                </span>
                <span className="font-medium text-white/92">Customize your short link</span>
              </label>
              {useCustomAlias && (
                <p className="text-xs text-[#B4A5A5]">
                  Only letters, numbers, - and _. e.g. my-portfolio
                </p>
              )}
            </div>
            {cardError && (
              <p className="mt-3 text-left text-sm font-medium text-red-400">
                {cardError}
              </p>
            )}
            {shortUrl && (
              <div className="mt-5 rounded-2xl bg-[#151515] p-5 text-center shadow-[0_12px_28px_rgba(0,0,0,0.22)]">
                <p className="break-all text-xl font-bold tracking-[-0.02em] text-white sm:text-2xl">
                  {shortUrlLabel}
                </p>
                <div className="mt-5 flex justify-center gap-3">
                  <button
                    onClick={() => navigator.clipboard.writeText(shortUrl)}
                    className="rounded-xl bg-[#151515] px-4 py-2 font-medium tracking-[0.01em] text-[#B4A5A5] shadow-[0_8px_20px_rgba(0,0,0,0.2)] hover:text-white"
                  >
                    Copy
                  </button>
                  <a
                    href={shortUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="rounded-xl bg-[#301B3F] px-4 py-2 font-medium tracking-[0.01em] text-white hover:bg-[#3C415C]"
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
          className="mx-auto text-center text-3xl font-bold tracking-[-0.03em] text-white lg:w-[60%] md:w-[70%] sm:w-[80%]"
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
