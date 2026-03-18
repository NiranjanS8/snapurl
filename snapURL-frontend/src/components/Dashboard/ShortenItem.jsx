import dayjs from 'dayjs';
import React, { useEffect, useState } from 'react'
import CopyToClipboard from 'react-copy-to-clipboard';
import { FaExternalLinkAlt, FaRegCalendarAlt } from 'react-icons/fa';
import { IoCopy } from 'react-icons/io5';
import { LiaCheckSolid } from 'react-icons/lia';
import { MdAnalytics, MdOutlineAdsClick } from 'react-icons/md';
import api from '../../api/api';
import { Link, useNavigate } from 'react-router-dom';
import { useStoreContext } from '../../contextApi/ContextApi';
import { Hourglass } from 'react-loader-spinner';
import Graph from './Graph';

const ShortenItem = ({ originalUrl, shortUrl, clickCount, createdAt }) => {
    const { token } = useStoreContext();
    const navigate = useNavigate();
    const [isCopied, setIsCopied] = useState(false);
    const [analyticToggle, setAnalyticToggle] = useState(false);
    const [loader, setLoader] = useState(false);
    const [selectedUrl, setSelectedUrl] = useState("");
    const [analyticsData, setAnalyticsData] = useState([]);

    const subDomain = import.meta.env.VITE_REACT_FRONT_END_URL.replace(
        /^https?:\/\//,
        ""
      );

    const analyticsHandler = (shortUrl) => {
        if (!analyticToggle) {
            setSelectedUrl(shortUrl);
        }
        setAnalyticToggle(!analyticToggle);
    }

    const fetchMyShortUrl = async () => {
        setLoader(true);
        try {
             const startDate = dayjs(createdAt).startOf("day").format("YYYY-MM-DDTHH:mm:ss");
             const endDate = dayjs().endOf("day").format("YYYY-MM-DDTHH:mm:ss");
             const { data } = await api.get(
              `/api/urls/analytics/${selectedUrl}?startDate=${startDate}&endDate=${endDate}`
             );
            setAnalyticsData(data || []);
            setSelectedUrl("");
            console.log(data);
            
        } catch (error) {
            navigate("/error");
            console.log(error);
        } finally {
            setLoader(false);
        }
    }


    useEffect(() => {
        if (selectedUrl) {
            fetchMyShortUrl();
        }
    }, [selectedUrl]);

  return (
    <div className="overflow-hidden rounded-2xl bg-[#1e1e1e] shadow-[0_16px_36px_rgba(0,0,0,0.2)] transition-all duration-200">
    <div className="flex flex-col gap-6 bg-[#1e1e1e] px-6 py-6 sm:px-7">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0 flex-1 space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            <span className="rounded-full bg-[#151515] px-3 py-1 text-xs font-bold uppercase tracking-[0.22em] text-white shadow-[0_8px_20px_rgba(0,0,0,0.16)]">
              Live Link
            </span>
            <span className="rounded-full bg-[#151515] px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-[#B4A5A5] shadow-[0_8px_20px_rgba(0,0,0,0.16)]">
              {clickCount === 1 ? "1 Click" : `${clickCount} Clicks`}
            </span>
          </div>

          <div className="flex items-center gap-2 text-white">
            <Link
              target='_'
              className='break-all text-xl font-black tracking-tight text-white sm:text-2xl'
              to={import.meta.env.VITE_REACT_FRONT_END_URL + "/s/" + `${shortUrl}`}>
                  {subDomain + "/s/" + `${shortUrl}`}
            </Link>
            <FaExternalLinkAlt className="shrink-0 text-[#B4A5A5]" />
          </div>

          <div className="rounded-2xl bg-[#151515] px-4 py-3 shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-[#B4A5A5]">
              Original URL
            </p>
            <h3 className="mt-2 break-all text-[15px] font-medium text-white sm:text-[16px]">
              {originalUrl}
            </h3>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-2xl bg-[#151515] px-4 py-4 text-white shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
              <div className="flex items-center gap-2 text-[#B4A5A5]">
                <MdOutlineAdsClick className="text-[22px]" />
                  <span className="text-xs font-bold uppercase tracking-[0.16em] text-[#B4A5A5]">
                  Engagement
                </span>
              </div>
              <div className="mt-3 text-3xl font-black leading-none">{clickCount}</div>
              <div className="mt-1 text-sm text-[#B4A5A5]">
                {clickCount === 1 ? "Total click" : "Total clicks"}
              </div>
            </div>

            <div className="rounded-2xl bg-[#151515] px-4 py-4 shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
              <div className="flex items-center gap-2 text-[#B4A5A5]">
                <FaRegCalendarAlt />
                <span className="text-xs font-bold uppercase tracking-[0.16em]">
                  Created
                </span>
              </div>
              <div className="mt-3 text-lg font-bold text-white">
                {dayjs(createdAt).format("MMM DD, YYYY")}
              </div>
              <div className="mt-1 text-sm text-[#B4A5A5]">
                {dayjs(createdAt).format("hh:mm A")}
              </div>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 lg:justify-end">
            <CopyToClipboard
                onCopy={() => setIsCopied(true)}
                text={`${import.meta.env.VITE_REACT_FRONT_END_URL + "/s/" + `${shortUrl}`}`}
            >
                <div className="flex cursor-pointer items-center gap-2 rounded-full bg-[#151515] px-5 py-3 font-semibold text-white transition-transform duration-150 hover:bg-[#301B3F]">
                <button className="">{isCopied ? "Copied" : "Copy"}</button>
                {isCopied ? (
                    <LiaCheckSolid className="text-md" />
                ) : (
                    <IoCopy className="text-md" />
                )}
                </div>
            </CopyToClipboard>

            <div
                onClick={() => analyticsHandler(shortUrl)}
                className="flex cursor-pointer items-center gap-2 rounded-full bg-[#301B3F] px-5 py-3 font-semibold text-white transition-transform duration-150 hover:bg-[#3C415C]"
            >
                <button>{analyticToggle ? "Hide Analytics" : "Analytics"}</button>
                <MdAnalytics className="text-md" />
          </div>
          </div>
      </div>
        <div className={`${
            analyticToggle ? "flex" : "hidden"
          } min-h-96 relative w-full overflow-hidden rounded-2xl bg-[#151515] p-4 shadow-[0_12px_28px_rgba(0,0,0,0.18)] sm:p-5`}>
            {loader ? (
                <div className="min-h-[calc(450px-140px)] flex justify-center items-center w-full">
                    <div className="flex flex-col items-center gap-1">
                    <Hourglass
                        visible={true}
                        height="50"
                        width="50"
                        ariaLabel="hourglass-loading"
                        wrapperStyle={{}}
                        wrapperClass=""
                        colors={['#306cce', '#72a1ed']}
                        />
                        <p className='text-[#B4A5A5]'>Please Wait...</p>
                    </div>
                </div>
                ) : ( 
                    <>{analyticsData.length === 0 && (
                        <div className="absolute inset-0 m-auto flex w-full flex-col items-center justify-center px-6 text-center">
                            <h1 className="mb-2 text-[15px] font-bold text-white sm:text-2xl">
                                No Data For This Time Period
                            </h1>
                            <h3 className="w-full max-w-md text-[12px] text-[#B4A5A5] sm:text-lg">
                                Share your short link to view where your engagements are
                                coming from
                            </h3>
                        </div>
                    )}
                        <Graph graphData={analyticsData} />
                    </>
                    )}
        </div>
      </div>
    </div>
  )
}

export default ShortenItem
