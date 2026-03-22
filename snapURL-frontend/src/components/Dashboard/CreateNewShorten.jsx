import React, { useState } from 'react'
import { useStoreContext } from '../../contextApi/ContextApi';
import { useForm } from 'react-hook-form';
import TextField from '../TextField';
import { Tooltip } from '@mui/material';
import { RxCross2 } from 'react-icons/rx';
import api from '../../api/api';
import toast from 'react-hot-toast';
import { buildShortLink } from '../../utils/publicUrl';

const RESERVED_ALIASES = new Set(["api", "admin", "login", "register", "signup", "auth", "public", "dashboard", "error", "s"]);

const CreateNewShorten = ({ setOpen, refetch }) => {
    const { token } = useStoreContext();
    const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      originalUrl: "",
      customAlias: "",
    },
    mode: "onTouched",
  });

  const createShortUrlHandler = async (formData) => {
    setLoading(true);
    try {
        const payload = {
          ...formData,
          customAlias: formData.customAlias?.trim() || undefined,
        };

        const { data: res } = await api.post("/api/urls/shorten", payload, {
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
              Authorization: "Bearer " + token,
            },
          });

          const shortenUrl = buildShortLink(res.shortUrl);
          navigator.clipboard.writeText(shortenUrl).then(() => {
            toast.success("Short URL Copied to Clipboard", {
                position: "bottom-center",
                className: "mb-5",
                duration: 3000,
            });
          });

          await refetch();
          reset();
          setOpen(false);
    } catch (error) {
        toast.error(error?.response?.data?.message || "Create ShortURL Failed");
    } finally {
        setLoading(false);
    }
  };


  return (
    <div className="flex items-center justify-center rounded-2xl bg-[#1e1e1e] shadow-[0_18px_40px_rgba(0,0,0,0.22)]">
    <form
        onSubmit={handleSubmit(createShortUrlHandler)}
        className="relative w-[360px] rounded-[28px] pt-8 pb-5 px-4 sm:w-[450px] sm:px-8"
      >

        <h1 className="mt-3 text-center text-[22px] font-semibold tracking-[-0.02em] text-white sm:mt-0 sm:text-2xl">
                Create New Shorten Url
        </h1>

        <div className="mt-2 mb-3 h-px bg-white/6 sm:mb-5" />

        <div>
          <TextField
            label="Enter URL"
            required
            id="originalUrl"
            placeholder="https://example.com"
            type="url"
            message="Url is required"
            register={register}
            errors={errors}
          />
        </div>

        <div className="mt-4 flex flex-col gap-1">
          <label
            htmlFor="customAlias"
            className="text-sm font-medium uppercase tracking-[0.12em] text-[#B4A5A5]"
          >
            Custom Alias
          </label>
          <input
            id="customAlias"
            type="text"
            placeholder="Optional"
            className={`rounded-2xl bg-[#151515] px-4 py-3 text-white outline-none shadow-[0_10px_24px_rgba(0,0,0,0.18)] ${
              errors.customAlias?.message ? "ring-2 ring-red-500/70" : "focus:ring-2 focus:ring-[#B4A5A5]/55"
            }`}
            {...register("customAlias", {
              validate: (value) => {
                const normalizedValue = value?.trim();
                if (!normalizedValue) {
                  return true;
                }
                if (!/^[A-Za-z0-9_-]{3,32}$/.test(normalizedValue)) {
                  return "Use 3-32 letters, numbers, hyphens, or underscores only";
                }
                if (RESERVED_ALIASES.has(normalizedValue.toLowerCase())) {
                  return "That alias is reserved. Please choose another one.";
                }
                return true;
              },
            })}
          />
          <p className="text-xs text-[#B4A5A5]">
            Optional. Great for branded links like `product-launch`.
          </p>
          {errors.customAlias?.message && (
            <p className="mt-1 text-sm font-medium text-red-400">
              {errors.customAlias.message}*
            </p>
          )}
        </div>

        <button
          className="my-3 w-32 rounded-full bg-[#301B3F] py-2 font-medium tracking-[0.01em] text-white transition-colors hover:bg-[#3C415C]"
          type="text"
        >
          {loading ? "Loading..." : "Create"}
        </button>

        {!loading && (
          <Tooltip title="Close">
            <button
              disabled={loading}
              onClick={() => setOpen(false)}
              className=" absolute right-2 top-2  "
            >
              <RxCross2 className="text-white text-3xl" />
            </button>
          </Tooltip>
        )}

      </form>
    </div>
  )
}

export default CreateNewShorten
