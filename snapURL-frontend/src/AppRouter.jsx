import { Suspense, lazy } from "react";
import { Route, Routes } from "react-router-dom";
import Navbar from "./components/NavBar";
import { Toaster } from "react-hot-toast";
import Footer from "./components/Footer";
import PrivateRoute from "./PrivateRoute";

const DashboardLayout = lazy(() => import("./components/Dashboard/DashboardLayout"));
const ErrorPage = lazy(() => import("./components/ErrorPage"));
const ForgotPasswordPage = lazy(() => import("./components/ForgotPasswordPage"));
const LandingPage = lazy(() => import("./components/LandingPage"));
const LoginPage = lazy(() => import("./components/LoginPage"));
const RegisterPage = lazy(() => import("./components/RegisterPage"));
const ShortenUrlPage = lazy(() => import("./components/ShortenUrlPage"));

const RouteFallback = () => (
  <div className="flex min-h-[calc(100vh-128px)] items-center justify-center px-4">
    <div className="h-10 w-10 rounded-full border-2 border-[#B4A5A5]/30 border-t-white animate-spin" />
  </div>
);

const AppRouter = () => {
  const hideHeaderFooter = location.pathname.startsWith("/s");

    return (
        <>
        {!hideHeaderFooter && <Navbar /> }
        <Toaster position='bottom-center'/>
        <main className={!hideHeaderFooter ? "relative overflow-hidden" : ""}>
          {!hideHeaderFooter && <div className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(circle_at_top_left,_rgba(60,65,92,0.14),_transparent_22%),radial-gradient(circle_at_top_right,_rgba(48,27,63,0.18),_transparent_20%)]" />}
          <Suspense fallback={<RouteFallback />}>
            <Routes>
              <Route path="/" element={<LandingPage />} />
              <Route path="/s/:url" element={<ShortenUrlPage />} />

              <Route path="/register" element={<PrivateRoute publicPage={true}><RegisterPage /></PrivateRoute>} />
              <Route path="/login" element={<PrivateRoute publicPage={true}><LoginPage /></PrivateRoute>} />
              <Route path="/forgot-password" element={<PrivateRoute publicPage={true}><ForgotPasswordPage /></PrivateRoute>} />
            
              <Route path="/dashboard" element={ <PrivateRoute publicPage={false}><DashboardLayout /></PrivateRoute>} />
              <Route path="/error" element={ <ErrorPage />} />
              <Route path="*" element={ <ErrorPage message="We can't seem to find the page you're looking for"/>} />
            </Routes>
          </Suspense>
        </main>
        {!hideHeaderFooter && <Footer />}
      </>
    );
}


export default AppRouter;

export const SubDomainRouter = () => {
    return (
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          <Route path="/:url" element={<ShortenUrlPage />} />
        </Routes>
      </Suspense>
    )
}
