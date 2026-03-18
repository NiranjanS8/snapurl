import React, { useState } from 'react'
import Graph from './Graph'
import { useStoreContext } from '../../contextApi/ContextApi'
import { useFetchMyShortUrls, useFetchTotalClicks } from '../../hooks/useQuery'
import ShortenPopUp from './ShortenPopUp'
import { FaChartLine, FaLink } from 'react-icons/fa'
import ShortenUrlList from './ShortenUrlList'
import { Link, useNavigate } from 'react-router-dom'
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
    const topLinks = [...safeShortenUrls]
      .sort((a, b) => (b.clickCount ?? 0) - (a.clickCount ?? 0))
      .slice(0, 5);
    const latestActivity = safeTotalClicks[safeTotalClicks.length - 1];
    const latestActivityDate = latestActivity?.clickDate
      ? dayjs(latestActivity.clickDate).format("MMM DD")
      : "No activity yet";

    function onError() {
      navigate("/error");
    }

  return (
    <div className="min-h-[calc(100vh-64px)] px-4 py-5 sm:px-8 sm:py-6 lg:px-14 lg:py-8">
        {loader ? ( 
            <Loader />
        ): ( 
        <div className="mx-auto flex w-full max-w-7xl flex-col gap-5">
            <div className="overflow-hidden rounded-2xl bg-[#1e1e1e] p-5 shadow-[0_20px_48px_rgba(0,0,0,0.24)] sm:p-6 lg:p-7">
              <div className="flex flex-col gap-5 lg:gap-6">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                  <div className="max-w-2xl">
                    <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">
                      Analytics Command Center
                    </p>
                    <h1 className="mt-2 text-2xl font-bold tracking-[-0.03em] text-white sm:text-4xl lg:whitespace-nowrap">
                      See what your links are doing at a glance.
                    </h1>
                  </div>

                  <div className='sm:text-end text-center'>
                    <button
                        className='rounded-full bg-[#301B3F] px-5 py-2.5 text-sm font-medium tracking-[0.01em] text-white hover:bg-[#3C415C]'
                        onClick={() => setShortenPopUp(true)}>
                        Create a New Short URL
                    </button>
                  </div>
                </div>

                <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                  <div className="flex h-full min-h-[124px] flex-col justify-between rounded-2xl bg-[#151515] px-4 py-4 text-white shadow-[0_12px_30px_rgba(0,0,0,0.18)] sm:px-5 sm:py-4">
                    <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#B4A5A5]">Total Clicks</p>
                    <p className="mt-2 text-3xl font-bold tracking-[-0.02em] sm:text-[32px]">{totalClickCount}</p>
                    <p className="mt-1 text-xs leading-5 text-[#B4A5A5] sm:text-sm">Across all tracked short links</p>
                  </div>

                  <div className="flex h-full min-h-[124px] flex-col justify-between rounded-2xl bg-[#151515] px-4 py-4 shadow-[0_12px_30px_rgba(0,0,0,0.18)] sm:px-5 sm:py-4">
                    <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#B4A5A5]">Links Created</p>
                    <p className="mt-2 text-3xl font-bold tracking-[-0.02em] text-white sm:text-[32px]">{totalLinks}</p>
                    <p className="mt-1 text-xs leading-5 text-[#B4A5A5] sm:text-sm">Short links currently in your dashboard</p>
                  </div>

                  <div className="flex h-full min-h-[124px] flex-col justify-between rounded-2xl bg-[#151515] px-4 py-4 shadow-[0_12px_30px_rgba(0,0,0,0.18)] sm:px-5 sm:py-4">
                    <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#B4A5A5]">Average Engagement</p>
                    <p className="mt-2 text-3xl font-bold tracking-[-0.02em] text-white sm:text-[32px]">{avgClicksPerLink}</p>
                    <p className="mt-1 text-xs leading-5 text-[#B4A5A5] sm:text-sm">Average clicks per active link</p>
                  </div>

                  <div className="flex h-full min-h-[124px] flex-col justify-between rounded-2xl bg-[#151515] px-4 py-4 shadow-[0_12px_30px_rgba(0,0,0,0.18)] sm:px-5 sm:py-4">
                    <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#B4A5A5]">Latest Activity</p>
                    <p className="mt-2 text-2xl font-bold tracking-[-0.02em] text-white sm:text-[28px]">{latestActivityDate}</p>
                    <p className="mt-1 text-xs leading-5 text-[#B4A5A5] sm:text-sm">
                      {latestActivity ? `${latestActivity.count ?? latestActivity.clickCount ?? 0} clicks recorded` : "Share a link to start collecting data"}
                    </p>
                  </div>
                </div>

                <div className="grid gap-4 xl:grid-cols-12 xl:items-stretch">
                  <div className="xl:col-span-8">
                  <div className="relative h-full overflow-hidden rounded-2xl bg-[#151515] p-4 shadow-[0_16px_36px_rgba(0,0,0,0.2)] sm:p-5 lg:p-6">
                    <div className="mb-4 flex items-center justify-between gap-4">
                      <div>
                        <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#B4A5A5]">Performance Timeline</p>
                        <h2 className="mt-1.5 text-xl font-semibold tracking-[-0.02em] text-white sm:text-2xl">
                          Click activity over time
                        </h2>
                      </div>
                      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#301B3F] text-white">
                        <FaChartLine />
                      </div>
                    </div>
                    <div className="relative h-[280px] sm:h-[320px] lg:h-[360px]">
                        {safeTotalClicks.length === 0 && (
                             <div className="absolute inset-0 m-auto flex flex-col items-center justify-center px-6 text-center">
                             <h1 className="mb-2 text-[18px] font-semibold tracking-[-0.02em] text-white sm:text-2xl">
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
                  </div>

                  <div className="xl:col-span-4">
                    <div className="flex h-full flex-col rounded-2xl bg-[#151515] px-5 py-5 text-white shadow-[0_12px_30px_rgba(0,0,0,0.18)] sm:px-6 sm:py-6">
                      <div className="flex items-center justify-between gap-3">
                        <div>
                          <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#B4A5A5]">Top Performers</p>
                          <h2 className="mt-1.5 text-xl font-semibold tracking-[-0.02em] text-white sm:text-2xl">
                            Link leaderboard
                          </h2>
                        </div>
                      </div>
                      {topLinks.length === 0 ? (
                        <div className="mt-4 rounded-2xl bg-[#1e1e1e] px-4 py-4 text-sm leading-6 text-[#B4A5A5] shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
                          Create and share a short URL to start ranking performance.
                        </div>
                      ) : (
                        <div className="mt-4 overflow-hidden rounded-2xl bg-[#1e1e1e] shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
                          <div className="grid grid-cols-[44px_minmax(0,1fr)_72px_78px] items-center gap-3 px-4 py-3 text-[11px] font-medium uppercase tracking-[0.12em] text-[#B4A5A5]">
                            <span>Rank</span>
                            <span>Short Link</span>
                            <span className="text-right">Clicks</span>
                            <span className="text-right">Share</span>
                          </div>
                          {topLinks.map((link, index) => {
                            const clicks = link.clickCount ?? 0;
                            const share = totalClickCount > 0 ? ((clicks / totalClickCount) * 100).toFixed(1) : "0.0";

                            return (
                              <div
                                key={link.id ?? link.shortUrl}
                                className="grid grid-cols-[44px_minmax(0,1fr)_72px_78px] items-center gap-3 border-t border-white/6 px-4 py-3 text-sm text-[#B4A5A5]"
                              >
                                <span className={`font-medium ${index === 0 ? "text-white" : "text-[#B4A5A5]"}`}>
                                  {index + 1}
                                </span>
                                <Link
                                  to={`${import.meta.env.VITE_REACT_FRONT_END_URL}/s/${link.shortUrl}`}
                                  target="_blank"
                                  rel="noreferrer"
                                  className={`truncate font-medium tracking-[-0.01em] hover:text-white ${index === 0 ? "text-[#C7B8FF]" : "text-white/92"}`}
                                >
                                  {link.shortUrl}
                                </Link>
                                <span className={`text-right font-medium ${index === 0 ? "text-white" : "text-white/88"}`}>
                                  {clicks}
                                </span>
                                <span className="text-right text-[#B4A5A5]">
                                  {share}%
                                </span>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className='pt-1'>
              <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">Link Intelligence</p>
                  <h2 className="mt-1.5 text-2xl font-bold tracking-[-0.03em] text-white sm:text-3xl">
                    Your shortened links
                  </h2>
                </div>
              </div>
              {!isLoading && safeShortenUrls.length === 0 ? (
                <div className="flex justify-center pt-10">
                  <div className="flex items-center justify-center gap-3 rounded-2xl bg-[#1e1e1e] px-6 py-6 shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                    <h1 className="text-[14px] font-medium tracking-[-0.01em] text-white sm:text-[18px]">
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
