import React, { useState } from 'react'
import { useStoreContext } from '../../contextApi/ContextApi';
import { useForm } from 'react-hook-form';
import { data } from 'autoprefixer';
import TextField from '../TextField';
import { Tooltip } from '@mui/material';
import { RxCross2 } from 'react-icons/rx';
import api from '../../api/api';
import toast from 'react-hot-toast';

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
    },
    mode: "onTouched",
  });

  const createShortUrlHandler = async (data) => {
    setLoading(true);
    try {
        const { data: res } = await api.post("/api/urls/shorten", data, {
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
              Authorization: "Bearer " + token,
            },
          });

          const shortenUrl = `${import.meta.env.VITE_REACT_FRONT_END_URL + "/s/" + `${res.shortUrl}`}`;
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
        toast.error("Create ShortURL Failed");
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
