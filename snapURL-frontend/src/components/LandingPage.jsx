import { useNavigate } from "react-router-dom";
import React, { useState } from "react";
import { motion } from "framer-motion";
import toast from "react-hot-toast";

import Card from "./Card";
import { useStoreContext } from "../contextApi/ContextApi";
import api from "../api/api";

let desc =
  "Generate short, memorable links with ease using SnapURL's intuitive interface. Share URLs effortlessly across platforms. Optimize your sharing strategy with SnapURL. Track clicks and manage your links seamlessly to enhance your online presence. Generate short, memorable links with ease using SnapURL's intuitive interface. Share URLs effortlessly across platforms.";

const GUEST_LIMIT = 3;
const GUEST_STORAGE_KEY = "SNAPURL_GUEST_SHORTENS";

const LandingPage = () => {
  const navigate = useNavigate();
  const { token } = useStoreContext();
  const [originalUrl, setOriginalUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [shortUrl, setShortUrl] = useState("");

  const guestUsageCount = Number(localStorage.getItem(GUEST_STORAGE_KEY) || "0");
  const guestUsesLeft = Math.max(GUEST_LIMIT - guestUsageCount, 0);
  const shortUrlLabel = shortUrl.replace(/^https?:\/\//, "");

  const dashBoardNavigateHandler = () => {
    navigate(token ? "/dashboard" : "/login");
  };

  const createShortLinkHandler = async () => {
    if (!originalUrl.trim()) {
      toast.error("Please enter a URL");
      return;
    }

    if (!token && guestUsageCount >= GUEST_LIMIT) {
      toast.error("Free tries are over. Please log in to continue.");
      navigate("/login");
      return;
    }

    setLoading(true);
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
      toast.error("Unable to create short URL");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-64px)]  lg:px-14 sm:px-8 px-4">
      <div className="lg:flex-row flex-col    lg:py-5   pt-16   lg:gap-10 gap-8 flex justify-between items-center">
        <div className=" flex-1">
          <motion.h1
            initial={{ opacity: 0, y: -80 }}
            whileInView={{
              opacity: 1,
              y: 0,
            }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
            className="font-bold font-roboto text-slate-800 md:text-5xl sm:text-4xl text-3xl   md:leading-[55px] sm:leading-[45px] leading-10 lg:w-full md:w-[70%] w-full"
          >
            SnapURL - Snap. Share. Go.
          </motion.h1>
          <p className="text-slate-700 text-sm my-5">
            SnapURL transforms lengthy URLs into short, convenient links for faster sharing. Its easy-to-use platform allows you to create and share compact URLs instantly, making link sharing smoother than ever.
          </p>
          <div className="mt-5 flex items-center gap-3">
            <motion.button
              initial={{ opacity: 0, y: 80 }}
              whileInView={{
                opacity: 1,
                y: 0,
              }}
              viewport={{ once: true }}
              transition={{ duration: 0.8 }}
              onClick={dashBoardNavigateHandler}
              className="bg-custom-gradient  w-40 text-white rounded-md  py-2"
            >
              Manage Links
            </motion.button>
            <motion.button
              initial={{ opacity: 0, y: 80 }}
              whileInView={{
                opacity: 1,
                y: 0,
              }}
              viewport={{ once: true }}
              transition={{ duration: 0.8 }}
              onClick={createShortLinkHandler}
              className="border-btnColor border w-40 text-btnColor rounded-md  py-2 "
            >
              Create Short Link
            </motion.button>
          </div>
        </div>
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{
            opacity: 1,
            y: 0,
          }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="flex-1 w-full"
        >
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xl shadow-slate-200/70">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-btnColor">
              Instant Guest Shortening
            </p>
            <h2 className="mt-3 text-2xl font-bold text-slate-800">
              Try SnapURL before you sign up
            </h2>
            <p className="mt-3 text-sm text-slate-600">
              Shorten links right from the homepage. Guests get {GUEST_LIMIT} free tries, then we’ll ask them to log in.
            </p>
            <div className="mt-5 flex flex-col gap-3">
              <input
                type="url"
                value={originalUrl}
                onChange={(event) => setOriginalUrl(event.target.value)}
                placeholder="Paste a long URL to shorten instantly"
                className="w-full rounded-md border border-slate-300 px-3 py-3 text-slate-800 outline-none focus:border-btnColor"
              />
              <button
                onClick={createShortLinkHandler}
                disabled={loading}
                className="rounded-md bg-custom-gradient px-5 py-3 font-semibold text-white disabled:opacity-70"
              >
                {loading ? "Creating..." : "Shorten Now"}
              </button>
            </div>
            <p className="mt-3 text-sm text-slate-600">
              {token
                ? "You're signed in, so your new links will also appear in the dashboard."
                : `${guestUsesLeft} of ${GUEST_LIMIT} free guest shortens left.`}
            </p>
            {shortUrl && (
              <div className="mt-4 rounded-xl bg-slate-50 p-4">
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
          className="text-slate-800 font-roboto font-bold lg:w-[60%]  md:w-[70%] sm:w-[80%] mx-auto text-3xl text-center"
        >
          Built for developers, teams, and everyday users.{" "}
        </motion.p>
        <div className="pt-4 pb-7 grid lg:gap-7 gap-4 xl:grid-cols-4  lg:grid-cols-3 sm:grid-cols-2 grid-cols-1 mt-4">
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
            desc="Enjoy lightning-fast redirects and high uptime with our reliable infrastructure. Your shortened URLs will always be available and responsive, ensuring a seamless experience for your users.
"
          />
        </div>
      </div>
    </div>
  );
};

export default LandingPage;
