import React, { useState } from 'react'
import Graph from './Graph'
import { useStoreContext } from '../../contextApi/ContextApi'
import { useFetchMyShortUrls, useFetchTotalClicks } from '../../hooks/useQuery'
import ShortenPopUp from './ShortenPopUp'
import { FaChartLine, FaLink } from 'react-icons/fa'
import ShortenUrlList from './ShortenUrlList'
import { useNavigate } from 'react-router-dom'
import Loader from '../Loader'
import dayjs from 'dayjs'

const DashboardLayout = () => {
    // const refetch = false;
    const { token } = useStoreContext();
    const navigate = useNavigate();
    const [shortenPopUp, setShortenPopUp] = useState(false);

    // console.log(useFetchTotalClicks(token, onError));

    const {isLoading, data: myShortenUrls, refetch } = useFetchMyShortUrls(token, onError)
    
    const {isLoading: loader, data: totalClicks, refetch: refetchTotalClicks} = useFetchTotalClicks(token, onError)

    const safeShortenUrls = myShortenUrls || [];
    const safeTotalClicks = totalClicks || [];
    const totalClickCount = safeTotalClicks.reduce((sum, item) => sum + (item.count ?? item.clickCount ?? 0), 0);
    const totalLinks = safeShortenUrls.length;
    const avgClicksPerLink = totalLinks ? (totalClickCount / totalLinks).toFixed(1) : "0.0";
    const topLink = safeShortenUrls.reduce((best, current) => {
      if (!best || current.clickCount > best.clickCount) {
        return current;
      }
      return best;
    }, null);
    const latestActivity = safeTotalClicks[safeTotalClicks.length - 1];
    const latestActivityDate = latestActivity?.clickDate
      ? dayjs(latestActivity.clickDate).format("MMM DD")
      : "No activity yet";

    function onError() {
      navigate("/error");
    }

  return (
    <div className="lg:px-14 sm:px-8 px-4 min-h-[calc(100vh-64px)]">
        {loader ? ( 
            <Loader />
        ): ( 
        <div className="mx-auto w-full py-12 lg:w-[92%]">
            <div className="overflow-hidden rounded-2xl bg-[#1e1e1e] p-6 shadow-[0_20px_48px_rgba(0,0,0,0.24)] sm:p-8">
              <div className="flex flex-col gap-8">
                <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
                  <div className="max-w-2xl">
                    <p className="text-sm font-black uppercase tracking-[0.28em] text-[#B4A5A5]">
                      Analytics Command Center
                    </p>
                    <h1 className="mt-3 text-3xl font-black tracking-tight text-white sm:text-5xl">
                      See what your links are doing at a glance.
                    </h1>
                    <p className="mt-4 max-w-xl text-sm leading-6 text-[#B4A5A5] sm:text-base">
                      A bold live view of link performance, click momentum, and top-performing short URLs across your dashboard.
                    </p>
                  </div>

                  <div className='sm:text-end text-center'>
                    <button
                        className='rounded-full bg-[#301B3F] px-5 py-3 font-semibold text-white hover:bg-[#3C415C]'
                        onClick={() => setShortenPopUp(true)}>
                        Create a New Short URL
                    </button>
                  </div>
                </div>

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                  <div className="rounded-2xl bg-[#151515] px-5 py-5 text-white shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                    <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#B4A5A5]">Total Clicks</p>
                    <p className="mt-4 text-4xl font-black tracking-tight">{totalClickCount}</p>
                    <p className="mt-2 text-sm text-[#B4A5A5]">Across all tracked short links</p>
                  </div>

                  <div className="rounded-2xl bg-[#151515] px-5 py-5 shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                    <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#B4A5A5]">Links Created</p>
                    <p className="mt-4 text-4xl font-black tracking-tight text-white">{totalLinks}</p>
                    <p className="mt-2 text-sm text-[#B4A5A5]">Short links currently in your dashboard</p>
                  </div>

                  <div className="rounded-2xl bg-[#151515] px-5 py-5 shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                    <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#B4A5A5]">Average Engagement</p>
                    <p className="mt-4 text-4xl font-black tracking-tight text-white">{avgClicksPerLink}</p>
                    <p className="mt-2 text-sm text-[#B4A5A5]">Average clicks per active link</p>
                  </div>

                  <div className="rounded-2xl bg-[#151515] px-5 py-5 shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                    <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#B4A5A5]">Latest Activity</p>
                    <p className="mt-4 text-3xl font-black tracking-tight text-white">{latestActivityDate}</p>
                    <p className="mt-2 text-sm text-[#B4A5A5]">
                      {latestActivity ? `${latestActivity.count ?? latestActivity.clickCount ?? 0} clicks recorded` : "Share a link to start collecting data"}
                    </p>
                  </div>
                </div>

                <div className="grid gap-5 xl:grid-cols-[minmax(0,1.8fr)_minmax(320px,0.9fr)]">
                  <div className="relative overflow-hidden rounded-2xl bg-[#151515] p-5 shadow-[0_16px_36px_rgba(0,0,0,0.2)] sm:p-6">
                    <div className="mb-5 flex items-center justify-between gap-4">
                      <div>
                        <p className="text-xs font-black uppercase tracking-[0.22em] text-[#B4A5A5]">Performance Timeline</p>
                        <h2 className="mt-2 text-2xl font-black tracking-tight text-white">
                          Click activity over time
                        </h2>
                      </div>
                      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#301B3F] text-white">
                        <FaChartLine />
                      </div>
                    </div>
                    <div className="h-96 relative">
                        {safeTotalClicks.length === 0 && (
                             <div className="absolute inset-0 m-auto flex flex-col items-center justify-center px-6 text-center">
                             <h1 className="mb-2 text-[18px] font-bold text-white sm:text-2xl">
                               No Data For This Time Period
                             </h1>
                             <h3 className="w-full max-w-md text-sm text-[#B4A5A5] sm:text-lg">
                               Share your short link to view where your engagements are
                               coming from
                             </h3>
                           </div>
                        )}
                        <Graph graphData={safeTotalClicks} />
                    </div>
                  </div>

                  <div className="grid gap-5">
                    <div className="rounded-2xl bg-[#151515] px-6 py-6 text-white shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                      <p className="text-xs font-black uppercase tracking-[0.22em] text-[#B4A5A5]">Top Performer</p>
                      <h2 className="mt-3 text-2xl font-black tracking-tight">
                        {topLink ? topLink.shortUrl : "No top link yet"}
                      </h2>
                      <p className="mt-3 break-all text-sm text-[#B4A5A5]">
                        {topLink ? topLink.originalUrl : "Create and share a short URL to start ranking performance."}
                      </p>
                      <div className="mt-5 inline-flex rounded-full bg-white/5 px-4 py-2 text-sm font-semibold text-white shadow-[0_8px_20px_rgba(0,0,0,0.16)]">
                        {topLink ? `${topLink.clickCount} clicks` : "Waiting for engagement"}
                      </div>
                    </div>

                    <div className="rounded-2xl bg-[#151515] px-6 py-6 shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                      <p className="text-xs font-black uppercase tracking-[0.22em] text-[#B4A5A5]">Dashboard Notes</p>
                      <div className="mt-5 grid gap-4">
                        <div className="rounded-2xl bg-[#1e1e1e] px-4 py-4 shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
                          <p className="text-sm font-bold text-white">Momentum</p>
                          <p className="mt-1 text-sm text-[#B4A5A5]">
                            {totalClickCount > 0 ? "Your links are collecting traffic. Keep promoting the strongest performers." : "No click activity yet. Share a live link to start filling this panel."}
                          </p>
                        </div>
                        <div className="rounded-2xl bg-[#1e1e1e] px-4 py-4 shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
                          <p className="text-sm font-bold text-white">Coverage</p>
                          <p className="mt-1 text-sm text-[#B4A5A5]">
                            {totalLinks > 0 ? `${totalLinks} links are available for tracking and comparison.` : "Create your first short URL to unlock link-level analytics cards below."}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className='pt-8'>
              <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p className="text-sm font-black uppercase tracking-[0.22em] text-[#B4A5A5]">Link Intelligence</p>
                  <h2 className="mt-2 text-2xl font-black tracking-tight text-white sm:text-3xl">
                    Your shortened links
                  </h2>
                </div>
              </div>
              {!isLoading && safeShortenUrls.length === 0 ? (
                <div className="flex justify-center pt-10">
                  <div className="flex items-center justify-center gap-3 rounded-2xl bg-[#1e1e1e] px-6 py-6 shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                    <h1 className="font-montserrat text-[14px] font-semibold text-white sm:text-[18px]">
                      You haven't created any short link yet
                    </h1>
                    <FaLink className="text-[#B4A5A5] sm:text-xl text-sm" />
                  </div>
              </div>
              ) : (
                  <ShortenUrlList data={safeShortenUrls} />
              )}
            </div>
        </div>
        )}

        <ShortenPopUp
          refetch={() => Promise.all([refetch(), refetchTotalClicks()])}
          open={shortenPopUp}
          setOpen={setShortenPopUp}
        />
    </div>
  )
}

export default DashboardLayout
