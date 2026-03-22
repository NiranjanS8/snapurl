const trimTrailingSlash = (value) => value?.replace(/\/+$/, "") || "";

export const getFrontendOrigin = () => {
  const configuredOrigin = trimTrailingSlash(import.meta.env.VITE_REACT_FRONT_END_URL);

  if (configuredOrigin) {
    return configuredOrigin;
  }

  if (typeof window !== "undefined" && window.location?.origin) {
    return trimTrailingSlash(window.location.origin);
  }

  return "";
};

export const buildShortLink = (shortCode) => {
  const frontendOrigin = getFrontendOrigin();
  return `${frontendOrigin}/s/${shortCode}`;
};
