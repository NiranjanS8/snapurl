import React, { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { IoIosMenu } from "react-icons/io";
import { RxCross2 } from "react-icons/rx";
import { useStoreContext } from "../contextApi/ContextApi";

const Navbar = () => {
  const navigate = useNavigate();
  const { token, setToken } = useStoreContext();
  const path = useLocation().pathname;
  const [navbarOpen, setNavbarOpen] = useState(false);

  const onLogOutHandler = () => {
    setToken(null);
    localStorage.removeItem("JWT_TOKEN");
    navigate("/login");
  };

  return (
    <div className="sticky top-0 z-50 bg-[#151515]/96 shadow-[0_10px_30px_rgba(0,0,0,0.28)] backdrop-blur-xl">
      <div className="mx-auto flex h-16 w-full items-center justify-between px-4 lg:px-14 sm:px-8">
        <Link to="/">
          <h1 className="text-2xl font-black tracking-tight text-white">
            SnapURL
          </h1>
        </Link>
        <ul
          className={`absolute left-4 right-4 top-[74px] flex flex-col gap-3 rounded-2xl bg-[#1e1e1e]/98 px-4 py-4 shadow-[0_16px_40px_rgba(0,0,0,0.3)] backdrop-blur-xl sm:static sm:left-auto sm:right-auto sm:top-auto sm:w-fit sm:flex-row sm:items-center sm:gap-2 sm:rounded-none sm:bg-transparent sm:px-0 sm:py-0 sm:shadow-none ${
            navbarOpen ? "h-fit opacity-100" : "pointer-events-none h-0 overflow-hidden opacity-0 sm:pointer-events-auto sm:h-fit sm:overflow-visible sm:opacity-100"
          }`}
        >
          <li>
            <Link
              className={`rounded-full px-4 py-2 text-sm font-semibold ${
                path === "/" ? "bg-[#1e1e1e] text-white" : "text-[#B4A5A5] hover:bg-white/4 hover:text-white"
              }`}
              to="/"
            >
              Home
            </Link>
          </li>
          <li>
            <Link
              className={`rounded-full px-4 py-2 text-sm font-semibold ${
                path === "/about" ? "bg-[#1e1e1e] text-white" : "text-[#B4A5A5] hover:bg-white/4 hover:text-white"
              }`}
              to="/about"
            >
              About
            </Link>
          </li>
          {token && (
            <li>
              <Link
                className={`rounded-full px-4 py-2 text-sm font-semibold ${
                  path === "/dashboard" ? "bg-[#1e1e1e] text-white" : "text-[#B4A5A5] hover:bg-white/4 hover:text-white"
                }`}
                to="/dashboard"
              >
                Dashboard
              </Link>
            </li>
          )}
          {!token && (
            <Link to="/register">
              <li className="rounded-full bg-[#301B3F] px-5 py-2 text-center text-sm font-semibold text-white hover:bg-[#3C415C]">
                SignUp
              </li>
            </Link>
          )}

          {token && (
            <button
             onClick={onLogOutHandler}
             className="rounded-full bg-[#301B3F] px-5 py-2 text-sm font-semibold text-white hover:bg-[#3C415C]">
              LogOut
            </button>
          )}
        </ul>
        <button
          onClick={() => setNavbarOpen(!navbarOpen)}
          className="flex items-center rounded-full bg-[#1e1e1e] px-2 py-1.5 shadow-[0_8px_24px_rgba(0,0,0,0.28)] sm:hidden"
        >
          {navbarOpen ? (
            <RxCross2 className="text-3xl text-white" />
          ) : (
            <IoIosMenu className="text-3xl text-white" />
          )}
        </button>
      </div>
    </div>
  );
};

export default Navbar;
