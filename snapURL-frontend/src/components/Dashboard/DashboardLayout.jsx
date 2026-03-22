import React, { useEffect, useRef, useState } from 'react'
import Graph from './Graph'
import { useStoreContext } from '../../contextApi/ContextApi'
import { useFetchMyShortUrls, useFetchTotalClicks } from '../../hooks/useQuery'
import ShortenPopUp from './ShortenPopUp'
import { FaChartLine, FaCheck, FaChevronDown, FaLink } from 'react-icons/fa'
import ShortenUrlList from './ShortenUrlList'
import { Link, useNavigate } from 'react-router-dom'
import Loader from '../Loader'
import dayjs from 'dayjs'
import { buildShortLink } from '../../utils/publicUrl'

const DashboardLayout = () => {
    // const refetch = false;
    const { token } = useStoreContext();
    const navigate = useNavigate();
    const [shortenPopUp, setShortenPopUp] = useState(false);
    const [searchInput, setSearchInput] = useState("");
    const [debouncedSearch, setDebouncedSearch] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [minClicks, setMinClicks] = useState("");
    const [maxClicks, setMaxClicks] = useState("");
    const [statusFilter, setStatusFilter] = useState("all");
    const [statusMenuOpen, setStatusMenuOpen] = useState(false);
    const [sortOption, setSortOption] = useState("latest");
    const [sortMenuOpen, setSortMenuOpen] = useState(false);
    const [cursor, setCursor] = useState(null);
    const [cursorHistory, setCursorHistory] = useState([]);
    const [currentPage, setCurrentPage] = useState(1);
    const linkSectionRef = useRef(null);
    const statusMenuRef = useRef(null);
    const sortMenuRef = useRef(null);

    // console.log(useFetchTotalClicks(token, onError));

    useEffect(() => {
      const timeoutId = window.setTimeout(() => {
        setDebouncedSearch(searchInput.trim());
      }, 350);

      return () => window.clearTimeout(timeoutId);
    }, [searchInput]);

    const sortMap = {
      latest: { sortBy: "createdAt", order: "desc" },
      oldest: { sortBy: "createdAt", order: "asc" },
      clicked: { sortBy: "clicks", order: "desc" },
      leastClicked: { sortBy: "clicks", order: "asc" },
      accessed: { sortBy: "lastAccessed", order: "desc" },
    };
    const sortLabelMap = {
      latest: "Latest",
      oldest: "Oldest",
      clicked: "Most clicked",
      leastClicked: "Least clicked",
      accessed: "Recently accessed",
    };
    const statusLabelMap = {
      all: "All status",
      active: "Active",
      expired: "Expired",
    };

    const activeSort = sortMap[sortOption] || sortMap.latest;
    const queryParams = {
      size: 10,
      sortBy: activeSort.sortBy,
      order: activeSort.order,
      ...(debouncedSearch ? { query: debouncedSearch } : {}),
      ...(cursor ? { cursor } : {}),
      ...(startDate ? { startDate } : {}),
      ...(endDate ? { endDate } : {}),
      ...(minClicks !== "" ? { minClicks } : {}),
      ...(maxClicks !== "" ? { maxClicks } : {}),
      ...(statusFilter !== "all" ? { status: statusFilter } : {}),
    };

    useEffect(() => {
      setCursor(null);
      setCursorHistory([]);
      setCurrentPage(1);
    }, [debouncedSearch, startDate, endDate, minClicks, maxClicks, statusFilter, sortOption]);

    useEffect(() => {
      const handleClickOutside = (event) => {
        if (statusMenuRef.current && !statusMenuRef.current.contains(event.target)) {
          setStatusMenuOpen(false);
        }
        if (sortMenuRef.current && !sortMenuRef.current.contains(event.target)) {
          setSortMenuOpen(false);
        }
      };

      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const {isLoading, data: myShortenUrls, refetch } = useFetchMyShortUrls({
      token,
      onError,
      params: queryParams,
    })
    
    const {isLoading: loader, data: totalClicks, refetch: refetchTotalClicks} = useFetchTotalClicks(token, onError)

    const safeShortenUrls = myShortenUrls?.items || [];
    const nextCursor = myShortenUrls?.nextCursor || null;
    const hasNextPage = Boolean(myShortenUrls?.hasNext);
    const safeTotalClicks = totalClicks || [];
    const totalClickCount = safeTotalClicks.reduce((sum, item) => sum + (item.count ?? item.clickCount ?? 0), 0);
    const totalLinks = safeShortenUrls.length;
    const visibleClickCount = safeShortenUrls.reduce((sum, item) => sum + (item.clickCount ?? 0), 0);
    const avgClicksPerLink = totalLinks ? (visibleClickCount / totalLinks).toFixed(1) : "0.0";
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

    const scrollToLinkSection = () => {
      linkSectionRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    };

    const handleNextPage = () => {
      if (!nextCursor) {
        return;
      }
      setCursorHistory((prev) => [...prev, cursor]);
      setCursor(nextCursor);
      setCurrentPage((prev) => prev + 1);
      scrollToLinkSection();
    };

    const handlePreviousPage = () => {
      if (currentPage === 1) {
        return;
      }

      setCursorHistory((prev) => {
        const nextHistory = [...prev];
        const previousCursor = nextHistory.pop() ?? null;
        setCursor(previousCursor);
        return nextHistory;
      });
      setCurrentPage((prev) => Math.max(prev - 1, 1));
      scrollToLinkSection();
    };

    const resetFilters = () => {
      setSearchInput("");
      setDebouncedSearch("");
      setStartDate("");
      setEndDate("");
      setMinClicks("");
      setMaxClicks("");
      setStatusFilter("all");
      setSortOption("latest");
      setCursor(null);
      setCursorHistory([]);
      setCurrentPage(1);
    };

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
                    <p className="mt-1 text-xs leading-5 text-[#B4A5A5] sm:text-sm">Links in the current result set</p>
                  </div>

                  <div className="flex h-full min-h-[124px] flex-col justify-between rounded-2xl bg-[#151515] px-4 py-4 shadow-[0_12px_30px_rgba(0,0,0,0.18)] sm:px-5 sm:py-4">
                    <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#B4A5A5]">Average Engagement</p>
                    <p className="mt-2 text-3xl font-bold tracking-[-0.02em] text-white sm:text-[32px]">{avgClicksPerLink}</p>
                    <p className="mt-1 text-xs leading-5 text-[#B4A5A5] sm:text-sm">Average clicks for the visible links</p>
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
                                  to={buildShortLink(link.shortUrl)}
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

            <div ref={linkSectionRef} className='pt-1'>
              <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p className="text-sm font-medium uppercase tracking-[0.16em] text-[#B4A5A5]">Link Intelligence</p>
                  <h2 className="mt-1.5 text-2xl font-bold tracking-[-0.03em] text-white sm:text-3xl">
                    Your shortened links
                  </h2>
                </div>
                <p className="text-sm text-[#B4A5A5]">
                  Page {currentPage} - {safeShortenUrls.length} results
                </p>
              </div>
              <div className="mb-5 rounded-2xl bg-[#1e1e1e] px-4 py-3 shadow-[0_12px_30px_rgba(0,0,0,0.16)]">
                <div className="flex flex-col gap-2.5 xl:flex-row xl:items-center xl:justify-start">
                  <div className="min-w-0 xl:w-[300px] xl:flex-none">
                    <input
                      type="text"
                      value={searchInput}
                      onChange={(event) => setSearchInput(event.target.value)}
                      placeholder="Search link"
                      className="h-10 w-full rounded-xl bg-[#151515] px-4 text-sm text-white outline-none shadow-[0_8px_20px_rgba(0,0,0,0.16)] placeholder:text-[#B4A5A5] focus:ring-2 focus:ring-[#B4A5A5]/45"
                    />
                  </div>

                  <div className="flex flex-col gap-2.5 lg:flex-row lg:items-center lg:flex-none">
                    <div className="flex items-center gap-2 lg:w-[340px] lg:flex-none">
                      <input
                        type="date"
                        value={startDate}
                        onChange={(event) => setStartDate(event.target.value)}
                        aria-label="Start date"
                        className="h-10 w-full rounded-xl bg-[#151515] px-3.5 text-sm text-white outline-none shadow-[0_8px_20px_rgba(0,0,0,0.16)] focus:ring-2 focus:ring-[#B4A5A5]/45"
                      />
                      <span className="px-1 text-sm text-[#B4A5A5]">to</span>
                      <input
                        type="date"
                        value={endDate}
                        onChange={(event) => setEndDate(event.target.value)}
                        aria-label="End date"
                        className="h-10 w-full rounded-xl bg-[#151515] px-3.5 text-sm text-white outline-none shadow-[0_8px_20px_rgba(0,0,0,0.16)] focus:ring-2 focus:ring-[#B4A5A5]/45"
                      />
                    </div>

                    <div className="grid gap-2.5 sm:grid-cols-2 lg:ml-4 lg:w-[230px] lg:grid-cols-2 lg:flex-none">
                      <input
                        type="number"
                        min="0"
                        value={minClicks}
                        onChange={(event) => setMinClicks(event.target.value)}
                        placeholder="Min clicks"
                        className="h-10 w-full rounded-xl bg-[#151515] px-3.5 text-sm text-white outline-none shadow-[0_8px_20px_rgba(0,0,0,0.16)] placeholder:text-[#B4A5A5] focus:ring-2 focus:ring-[#B4A5A5]/45"
                      />
                      <input
                        type="number"
                        min="0"
                        value={maxClicks}
                        onChange={(event) => setMaxClicks(event.target.value)}
                        placeholder="Max clicks"
                        className="h-10 w-full rounded-xl bg-[#151515] px-3.5 text-sm text-white outline-none shadow-[0_8px_20px_rgba(0,0,0,0.16)] placeholder:text-[#B4A5A5] focus:ring-2 focus:ring-[#B4A5A5]/45"
                      />
                    </div>

                    <div className="grid gap-2.5 sm:grid-cols-2 lg:ml-3 lg:w-[230px] lg:grid-cols-2 lg:flex-none">
                      <div ref={statusMenuRef} className="relative min-w-0">
                        <button
                          type="button"
                          onClick={() => {
                            setSortMenuOpen(false);
                            setStatusMenuOpen((prev) => !prev);
                          }}
                          className="flex h-10 w-full items-center justify-between rounded-xl bg-[#151515] px-3.5 text-sm text-white outline-none shadow-[0_8px_20px_rgba(0,0,0,0.16)] transition-colors hover:bg-[#1a1a1a] focus:ring-2 focus:ring-[#B4A5A5]/45"
                        >
                          <span>{statusFilter === "all" ? "Status" : statusLabelMap[statusFilter]}</span>
                          <FaChevronDown className={`text-[11px] text-[#B4A5A5] transition-transform ${statusMenuOpen ? "rotate-180" : ""}`} />
                        </button>
                        {statusMenuOpen && (
                          <div className="absolute left-0 right-0 top-[calc(100%+8px)] z-30 overflow-hidden rounded-xl bg-[#181818] p-1 shadow-[0_18px_40px_rgba(0,0,0,0.28)] ring-1 ring-white/6">
                            {Object.entries(statusLabelMap).map(([value, label]) => (
                              <button
                                key={value}
                                type="button"
                                onClick={() => {
                                  setStatusFilter(value);
                                  setStatusMenuOpen(false);
                                }}
                                className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm transition-colors ${
                                  statusFilter === value
                                    ? "bg-[#301B3F]/45 text-white"
                                    : "text-[#B4A5A5] hover:bg-white/5 hover:text-white"
                                }`}
                              >
                                <span>{label}</span>
                                {statusFilter === value && <FaCheck className="text-[11px] text-[#C7B8FF]" />}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>

                      <div ref={sortMenuRef} className="relative min-w-0">
                        <button
                          type="button"
                          onClick={() => {
                            setStatusMenuOpen(false);
                            setSortMenuOpen((prev) => !prev);
                          }}
                          className="flex h-10 w-full items-center justify-between rounded-xl bg-[#151515] px-3.5 text-sm text-white outline-none shadow-[0_8px_20px_rgba(0,0,0,0.16)] transition-colors hover:bg-[#1a1a1a] focus:ring-2 focus:ring-[#B4A5A5]/45"
                        >
                          <span>{sortLabelMap[sortOption]}</span>
                          <FaChevronDown className={`text-[11px] text-[#B4A5A5] transition-transform ${sortMenuOpen ? "rotate-180" : ""}`} />
                        </button>
                        {sortMenuOpen && (
                          <div className="absolute right-0 top-[calc(100%+8px)] z-30 min-w-full overflow-hidden rounded-xl bg-[#181818] p-1 shadow-[0_18px_40px_rgba(0,0,0,0.28)] ring-1 ring-white/6">
                            {Object.entries(sortLabelMap).map(([value, label]) => (
                              <button
                                key={value}
                                type="button"
                                onClick={() => {
                                  setSortOption(value);
                                  setSortMenuOpen(false);
                                }}
                                className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm transition-colors ${
                                  sortOption === value
                                    ? "bg-[#301B3F]/45 text-white"
                                    : "text-[#B4A5A5] hover:bg-white/5 hover:text-white"
                                }`}
                              >
                                <span>{label}</span>
                                {sortOption === value && <FaCheck className="text-[11px] text-[#C7B8FF]" />}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center xl:ml-1 xl:flex-none">
                    <button
                      onClick={resetFilters}
                      className="h-10 rounded-xl border border-white/8 px-3 text-sm font-medium tracking-[0.01em] text-[#B4A5A5] transition-colors hover:border-white/14 hover:text-white"
                    >
                      Reset
                    </button>
                  </div>
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
                  <ShortenUrlList
                    data={safeShortenUrls}
                    onDelete={() => Promise.all([refetch(), refetchTotalClicks()])}
                  />
              )}
              {!isLoading && safeShortenUrls.length > 0 && (
                <div className="mt-5 flex justify-center rounded-2xl bg-[#1e1e1e] px-4 py-4 shadow-[0_12px_30px_rgba(0,0,0,0.18)]">
                  <div className="flex items-center gap-3">
                    <button
                      onClick={handlePreviousPage}
                      disabled={currentPage === 1}
                      className="rounded-full bg-[#151515] px-4 py-2 text-sm font-medium tracking-[0.01em] text-white disabled:cursor-not-allowed disabled:opacity-45"
                    >
                      Previous
                    </button>
                    <button
                      onClick={handleNextPage}
                      disabled={!hasNextPage}
                      className="rounded-full bg-[#301B3F] px-4 py-2 text-sm font-medium tracking-[0.01em] text-white disabled:cursor-not-allowed disabled:opacity-45"
                    >
                      Next
                    </button>
                  </div>
                </div>
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
