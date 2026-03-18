import React from "react";
import { FaGithub, FaLinkedin } from "react-icons/fa";

const Footer = () => {
  return (
    <footer className="relative z-40 mt-14 bg-[#151515] py-10 shadow-[0_-10px_30px_rgba(0,0,0,0.18)]">
      <div className="mx-auto flex max-w-7xl flex-col gap-8 px-6 lg:flex-row lg:items-center lg:justify-between lg:px-14">
        <div className="text-center lg:text-left">
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">SnapURL</p>
          <h2 className="mt-2 text-3xl font-bold tracking-[-0.03em] text-white">Share smarter links</h2>
          <p className="mt-3 max-w-md text-sm leading-6 text-[#B4A5A5]">
            Simplifying URL shortening with a cleaner command center for sharing, tracking, and managing every link.
          </p>
        </div>

        <div className="rounded-2xl bg-[#1e1e1e] px-6 py-5 text-center text-white shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">Built for speed</p>
          <p className="mt-3 text-sm text-[#B4A5A5]">&copy; 2026 SnapURL. All rights reserved.</p>
        </div>

        <div className="mt-1 flex justify-center space-x-4 lg:mt-0">
          <a href="https://www.linkedin.com/in/niranjans8" target="_blank" rel="noreferrer" className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#1e1e1e] text-[#B4A5A5] shadow-[0_8px_20px_rgba(0,0,0,0.18)] hover:text-white">
            <FaLinkedin size={24} />
          </a>
          <a href="https://github.com/NiranjanS8" target="_blank" rel="noreferrer" className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#1e1e1e] text-[#B4A5A5] shadow-[0_8px_20px_rgba(0,0,0,0.18)] hover:text-white">
            <FaGithub size={24} />
          </a>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
