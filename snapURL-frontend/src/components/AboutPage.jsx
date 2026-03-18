import React from "react";
import { FaLink, FaShareAlt, FaEdit, FaChartLine } from "react-icons/fa";

const features = [
  {
    title: "Simple URL Shortening",
    desc: "Experience the ease of creating short, memorable URLs in just a few clicks. Our intuitive interface and quick setup process ensure you can start shortening URLs without any hassle.",
    icon: FaLink,
    accent: "text-[#B4A5A5]",
    bg: "bg-transparent",
  },
  {
    title: "Powerful Analytics",
    desc: "Gain insights into your link performance with our comprehensive analytics dashboard. Track clicks, geographical data, and referral sources to optimize your marketing strategies.",
    icon: FaShareAlt,
    accent: "text-[#B4A5A5]",
    bg: "bg-transparent",
  },
  {
    title: "Enhanced Security",
    desc: "Rest assured with our robust security measures. All shortened URLs are protected with advanced encryption, ensuring your data remains safe and secure.",
    icon: FaEdit,
    accent: "text-[#B4A5A5]",
    bg: "bg-transparent",
  },
  {
    title: "Fast and Reliable",
    desc: "Enjoy lightning-fast redirects and high uptime with our reliable infrastructure. Your shortened URLs will always be available and responsive, ensuring a seamless experience for your users.",
    icon: FaChartLine,
    accent: "text-[#B4A5A5]",
    bg: "bg-transparent",
  },
];

const AboutPage = () => {
  return (
    <div className="min-h-[calc(100vh-64px)] px-4 py-5 sm:px-8 sm:py-6 lg:px-14 lg:py-8">
      <div className="mx-auto max-w-7xl">
        <div className="overflow-hidden rounded-2xl bg-[#1e1e1e] px-5 py-6 shadow-[0_18px_40px_rgba(0,0,0,0.22)] sm:px-6 sm:py-7 lg:px-7">
          <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">About SnapURL</p>
          <h1 className="mt-2 text-3xl font-bold tracking-[-0.03em] text-white sm:text-4xl lg:text-[42px]">
            The same link tool, redesigned to feel sharper and easier to trust.
          </h1>
          <p className="mt-3 max-w-3xl text-sm leading-6 text-[#B4A5A5]">
          SnapURL simplifies URL shortening for efficient sharing. Easily
          generate, manage, and track your shortened links. SnapURL simplifies
          URL shortening for efficient sharing. Easily generate, manage, and
          track your shortened links. SnapURL simplifies URL shortening for
          efficient sharing. Easily generate, manage, and track your shortened
          links. SnapURL simplifies URL shortening for efficient sharing.
          Easily generate, manage, and track your shortened links.
          </p>
        </div>

        <div className="mt-5 grid gap-4 md:grid-cols-2">
          {features.map((feature) => {
            const Icon = feature.icon;
            return (
              <div key={feature.title} className="rounded-2xl bg-[#1e1e1e] px-5 py-5 shadow-[0_12px_28px_rgba(0,0,0,0.18)] sm:px-6 sm:py-5">
                <div className={`flex h-9 w-9 items-center justify-center rounded-xl ${feature.bg} ${feature.accent}`}>
                  <Icon className="text-xl" />
                </div>
                <h2 className="mt-4 text-xl font-semibold tracking-[-0.02em] text-white sm:text-2xl">
                  {feature.title}
                </h2>
                <p className="mt-2 text-sm leading-6 text-[#B4A5A5]">{feature.desc}</p>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default AboutPage;
