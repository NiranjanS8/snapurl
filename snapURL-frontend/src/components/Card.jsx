import React from "react";
import { motion } from "framer-motion";
const Card = ({ title, desc, icon: Icon, accentClass = "text-[#B4A5A5]", accentBgClass = "bg-transparent" }) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 120 }}
      whileInView={{
        opacity: 1,
        y: 0,
      }}
      viewport={{ once: true }}
      transition={{ duration: 0.5 }}
      className="group rounded-2xl bg-[#1e1e1e] px-5 py-7 shadow-[0_14px_34px_rgba(0,0,0,0.2)] transition-all duration-200 hover:-translate-y-1 hover:shadow-[0_20px_40px_rgba(0,0,0,0.28)]"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1">
          <h1 className="text-xl font-semibold tracking-[-0.02em] text-white">{title}</h1>
        </div>
        {Icon && (
          <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${accentBgClass} ${accentClass} transition-transform duration-200 group-hover:scale-105`}>
            <Icon className="text-lg" />
          </div>
        )}
      </div>
      <p className="mt-3 text-sm leading-6 text-[#B4A5A5]">{desc}</p>
    </motion.div>
  );
};

export default Card;
