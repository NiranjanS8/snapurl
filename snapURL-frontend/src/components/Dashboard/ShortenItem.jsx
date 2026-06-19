import dayjs from 'dayjs';
import { useEffect, useRef, useState } from 'react'
import PropTypes from 'prop-types';
import CopyToClipboard from 'react-copy-to-clipboard';
import { FaExternalLinkAlt, FaRegCalendarAlt } from 'react-icons/fa';
import { IoCopy } from 'react-icons/io5';
import { LiaCheckSolid } from 'react-icons/lia';
import { MdAnalytics, MdOutlineAdsClick, MdQrCode2 } from 'react-icons/md';
import { QRCodeCanvas } from 'qrcode.react';
import api from '../../api/api';
import { Link, useNavigate } from 'react-router-dom';
import { useStoreContext } from '../../contextApi/ContextApi';
import { Hourglass } from 'react-loader-spinner';
import Graph from './Graph';
import toast from 'react-hot-toast';
import DeleteConfirmModal from './DeleteConfirmModal';
import { buildShortLink, getFrontendOrigin } from '../../utils/publicUrl';

const ShortenItem = ({ id, originalUrl, shortUrl, clickCount, createdAt, onDelete }) => {
    const { token } = useStoreContext();
    const navigate = useNavigate();
    const [isCopied, setIsCopied] = useState(false);
    const [analyticToggle, setAnalyticToggle] = useState(false);
    const [loader, setLoader] = useState(false);
    const [deleteLoading, setDeleteLoading] = useState(false);
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [qrModalOpen, setQrModalOpen] = useState(false);
    const [selectedUrl, setSelectedUrl] = useState("");
    const [analyticsData, setAnalyticsData] = useState([]);
    const qrCodeRef = useRef(null);

    const subDomain = getFrontendOrigin().replace(
        /^https?:\/\//,
        ""
      );
    const shortLink = `${subDomain}/s/${shortUrl}`;
    const createdDate = dayjs(createdAt).format("MMM DD, YYYY");

    const downloadQrCode = () => {
        const canvas = qrCodeRef.current;
        if (!canvas) {
            return;
        }

        const downloadLink = document.createElement("a");
        downloadLink.download = `snapurl-${shortUrl}-qr.png`;
        downloadLink.href = canvas.toDataURL("image/png");
        downloadLink.click();
    };

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
        // The fetch is intentionally triggered only when a short URL is selected.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedUrl]);

    const deleteHandler = async () => {
        if (deleteLoading) {
            return;
        }

        setDeleteLoading(true);
        try {
            await api.delete(`/api/urls/${id}`, {
                headers: {
                    Authorization: "Bearer " + token,
                },
            });
            toast.success("Short link deleted");
            setDeleteModalOpen(false);
            if (onDelete) {
                await onDelete();
            }
        } catch (error) {
            toast.error(error?.response?.data?.message || "Delete failed");
        } finally {
            setDeleteLoading(false);
        }
    };

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
              to={buildShortLink(shortUrl)}>
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
            <button
                type="button"
                onClick={() => setDeleteModalOpen(true)}
                disabled={deleteLoading}
                className="flex items-center justify-center rounded-full bg-[#151515] px-3 py-2 text-sm text-white shadow-[0_8px_20px_rgba(0,0,0,0.16)] transition-transform duration-150 hover:bg-[#301B3F] disabled:cursor-not-allowed disabled:opacity-45"
                aria-label="Delete short link"
            >
                {deleteLoading ? "..." : "\uD83D\uDDD1"}
            </button>
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
              text={buildShortLink(shortUrl)}
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
          <button
              type="button"
              onClick={() => setQrModalOpen(true)}
              className="flex items-center gap-2 rounded-xl bg-[#151515] px-3.5 py-2.5 text-sm font-medium tracking-[0.01em] text-white shadow-[0_10px_24px_rgba(0,0,0,0.16)] transition-colors duration-150 hover:bg-[#301B3F]"
              aria-label={`Show QR code for ${shortLink}`}
          >
            <span>QR</span>
            <MdQrCode2 className="text-lg" />
          </button>
          <div className="inline-flex items-center gap-2 rounded-xl bg-[#151515] px-3 py-2.5 shadow-[0_10px_24px_rgba(0,0,0,0.16)]">
            <FaRegCalendarAlt className="text-sm" />
            <span className="text-sm text-white/92">{createdDate}</span>
          </div>
        </div>
      </div>
      <DeleteConfirmModal
        open={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={deleteHandler}
        loading={deleteLoading}
      />
      {qrModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 px-4 py-8 backdrop-blur-sm"
          role="dialog"
          aria-modal="true"
          aria-labelledby={`qr-title-${id}`}
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setQrModalOpen(false);
            }
          }}
        >
          <div className="relative w-full max-w-sm overflow-hidden rounded-[28px] border border-white/10 bg-[#1e1e1e] p-5 shadow-[0_28px_80px_rgba(0,0,0,0.55)] sm:p-6">
            <div className="absolute -right-12 -top-12 h-36 w-36 rounded-full bg-[#301B3F]/70 blur-3xl" />
            <div className="relative">
              <div className="mb-5 flex items-start justify-between gap-4">
                <div>
                  <p className="mb-1 text-[10px] font-semibold uppercase tracking-[0.2em] text-[#B4A5A5]">Scan to visit</p>
                  <h2 id={`qr-title-${id}`} className="text-xl font-semibold tracking-[-0.03em] text-white">Your link, camera-ready.</h2>
                </div>
                <button
                  type="button"
                  onClick={() => setQrModalOpen(false)}
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#151515] text-xl text-[#B4A5A5] transition-colors hover:text-white"
                  aria-label="Close QR code"
                >
                  &times;
                </button>
              </div>

              <div className="mx-auto flex aspect-square w-full max-w-[270px] items-center justify-center rounded-[24px] bg-[#f7f3ea] p-5 shadow-[inset_0_0_0_1px_rgba(0,0,0,0.08),0_18px_44px_rgba(0,0,0,0.28)]">
                <QRCodeCanvas
                  ref={qrCodeRef}
                  value={buildShortLink(shortUrl)}
                  size={230}
                  level="H"
                  marginSize={1}
                  bgColor="#f7f3ea"
                  fgColor="#151515"
                  title={`QR code for ${shortLink}`}
                  className="h-auto max-h-full w-full max-w-full"
                />
              </div>

              <p className="mt-4 truncate rounded-xl bg-[#151515] px-3 py-2.5 text-center text-xs text-[#B4A5A5]">
                {buildShortLink(shortUrl)}
              </p>
              <button
                type="button"
                onClick={downloadQrCode}
                className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-[#301B3F] px-4 py-3 text-sm font-semibold text-white transition-colors hover:bg-[#3C415C]"
              >
                <MdQrCode2 className="text-lg" />
                Download PNG
              </button>
            </div>
          </div>
        </div>
      )}

        <div className={`${
            analyticToggle ? "flex" : "hidden"
          } relative min-h-[190px] w-full overflow-hidden rounded-2xl bg-[#151515] p-2.5 shadow-[0_12px_28px_rgba(0,0,0,0.18)] sm:p-3`}>
            {loader ? (
                <div className="flex min-h-[150px] w-full items-center justify-center">
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
                        <div className="mx-auto h-[150px] w-full max-w-[640px] sm:h-[165px]">
                          <Graph graphData={analyticsData} compact />
                        </div>
                    </>
                    )}
        </div>
      </div>
    </div>
  )
}

ShortenItem.propTypes = {
    id: PropTypes.number.isRequired,
    originalUrl: PropTypes.string.isRequired,
    shortUrl: PropTypes.string.isRequired,
    clickCount: PropTypes.number.isRequired,
    createdAt: PropTypes.string.isRequired,
    onDelete: PropTypes.func,
};

export default ShortenItem
