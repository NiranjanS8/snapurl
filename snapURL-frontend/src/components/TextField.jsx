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
    return (
      <div className="flex flex-col gap-1">
        <label
          htmlFor={id}
          className={`${className ? className : ""} text-sm font-bold uppercase tracking-[0.12em] text-[#B4A5A5]`}
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
              ? { value: min, message: "Minimum 6 character is required" }
              : null,
  
            pattern:
              type === "email"
                ? {
                    value: /^[a-zA-Z0-9]+@(?:[a-zA-Z0-9]+\.)+com+$/,
                    message: "Invalid email",
                  }
                : type === "url"
                ? {
                    value:
                      /^(https?:\/\/)?(([a-zA-Z0-9\u00a1-\uffff-]+\.)+[a-zA-Z\u00a1-\uffff]{2,})(:\d{2,5})?(\/[^\s]*)?$/,
                    message: "Please enter a valid url",
                  }
                : null,
          })}
        />
  
        {errors[id]?.message && (
          <p className="mt-1 text-sm font-semibold text-red-400">
            {errors[id]?.message}*
          </p>
        )}
      </div>
    );
  };
  
  export default TextField;
