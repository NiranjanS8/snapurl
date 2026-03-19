const TextField = ({
    label,
    id,
    type,
    errors,
    register,
    required,
    message,
    className,
    min,
    value,
    placeholder,
  }) => {
    const strictUrlPattern =
      /^(https?:\/\/)?(?=.{4,253}$)(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,24}(?::\d{2,5})?(?:\/[^\s]*)?$/;
    const emailPattern =
      /^[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\.)+[A-Za-z]{2,24}$/;

    return (
      <div className="flex flex-col gap-1">
        <label
          htmlFor={id}
          className={`${className ? className : ""} text-sm font-medium uppercase tracking-[0.12em] text-[#B4A5A5]`}
        >
          {label}
        </label>
  
        <input
          type={type}
          id={id}
          placeholder={placeholder}
          className={`${
            className ? className : ""
          } rounded-2xl bg-[#151515] px-4 py-3 outline-none text-white shadow-[0_10px_24px_rgba(0,0,0,0.18)] ${
            errors[id]?.message ? "ring-2 ring-red-500/70" : "focus:ring-2 focus:ring-[#B4A5A5]/55"
          }`}
          {...register(id, {
            required: { value: required, message },
            minLength: min
              ? { value: min, message: `Minimum ${min} character is required` }
              : null,
  
            pattern:
              type === "email"
                ? {
                    value: emailPattern,
                    message: "Invalid email",
                  }
                : type === "url"
                ? {
                    value: strictUrlPattern,
                    message: "Please enter a valid url",
                  }
                : null,
          })}
        />
  
        {errors[id]?.message && (
          <p className="mt-1 text-sm font-medium text-red-400">
            {errors[id]?.message}*
          </p>
        )}
      </div>
    );
  };
  
  export default TextField;
