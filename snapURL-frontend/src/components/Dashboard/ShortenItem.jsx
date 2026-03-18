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
    const shortLink = `${subDomain}/s/${shortUrl}`;
    const createdDate = dayjs(createdAt).format("MMM DD, YYYY");

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
    <div className="flex flex-col gap-4 bg-[#1e1e1e] px-4 py-4 sm:px-5">
      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2.5">
            <span className="rounded-full bg-[#151515] px-2.5 py-1 text-[11px] font-medium uppercase tracking-[0.12em] text-white shadow-[0_8px_20px_rgba(0,0,0,0.16)]">
              Live
            </span>
            <Link
              target='_'
              className='min-w-0 break-all text-base font-semibold tracking-[-0.02em] text-white sm:text-lg'
              to={import.meta.env.VITE_REACT_FRONT_END_URL + "/s/" + `${shortUrl}`}>
                  {shortLink}
            </Link>
            <FaExternalLinkAlt className="shrink-0 text-sm text-[#B4A5A5]" />
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2 xl:justify-end">
            <div className="inline-flex items-center gap-2 rounded-full bg-[#151515] px-3 py-2 text-sm text-white shadow-[0_8px_20px_rgba(0,0,0,0.16)]">
              <MdOutlineAdsClick className="text-base text-[#B4A5A5]" />
              <span className="font-semibold tracking-[-0.01em]">{clickCount}</span>
              <span className="text-xs text-[#B4A5A5]">{clickCount === 1 ? "click" : "clicks"}</span>
            </div>
            <div
                onClick={() => analyticsHandler(shortUrl)}
                className="flex cursor-pointer items-center gap-2 rounded-full bg-[#301B3F] px-4 py-2.5 text-sm font-medium tracking-[0.01em] text-white transition-transform duration-150 hover:bg-[#3C415C]"
            >
                <button>{analyticToggle ? "Hide Analytics" : "Analytics"}</button>
                <MdAnalytics className="text-md" />
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-2.5 text-sm text-[#B4A5A5] lg:flex-row lg:items-center lg:justify-between">
        <div className="min-w-0 rounded-xl bg-[#151515] px-3.5 py-2.5 shadow-[0_10px_24px_rgba(0,0,0,0.16)] lg:flex-1">
          <span className="mr-2 text-[11px] font-medium uppercase tracking-[0.12em] text-[#B4A5A5]">Original</span>
          <span className="break-all text-sm text-white/92">{originalUrl}</span>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 lg:justify-end">
          <CopyToClipboard
              onCopy={() => setIsCopied(true)}
              text={`${import.meta.env.VITE_REACT_FRONT_END_URL + "/s/" + `${shortUrl}`}`}
          >
              <div className="flex cursor-pointer items-center gap-2 rounded-xl bg-[#151515] px-3.5 py-2.5 text-sm font-medium tracking-[0.01em] text-white shadow-[0_10px_24px_rgba(0,0,0,0.16)] transition-transform duration-150 hover:bg-[#301B3F]">
                <button>{isCopied ? "Copied" : "Copy"}</button>
                {isCopied ? (
                    <LiaCheckSolid className="text-md" />
                ) : (
                    <IoCopy className="text-md" />
                )}
              </div>
          </CopyToClipboard>
          <div className="inline-flex items-center gap-2 rounded-xl bg-[#151515] px-3 py-2.5 shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
            <FaRegCalendarAlt className="text-sm" />
            <span className="text-sm text-white/92">{createdDate}</span>
          </div>
        </div>
      </div>

        <div className={`${
            analyticToggle ? "flex" : "hidden"
          } relative min-h-[300px] w-full overflow-hidden rounded-2xl bg-[#151515] p-3.5 shadow-[0_12px_28px_rgba(0,0,0,0.18)] sm:p-4`}>
            {loader ? (
                <div className="flex min-h-[220px] w-full items-center justify-center">
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
                            <h1 className="mb-2 text-[15px] font-semibold tracking-[-0.02em] text-white sm:text-xl">
                                No Data For This Time Period
                            </h1>
                            <h3 className="w-full max-w-md text-[12px] text-[#B4A5A5] sm:text-base">
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
