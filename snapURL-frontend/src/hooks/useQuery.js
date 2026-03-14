import { useQuery } from "react-query"
import api from "../api/api"


export const useFetchMyShortUrls = (token, onError) => {
    return useQuery("my-shortenurls",
         async () => {
            return await api.get(
                "/api/urls/myurls"
        );
    },
          {
            select: (data) => {
                const sortedData = data.data.sort(
                    (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
                );
                return sortedData;
            },
            enabled: Boolean(token),
            onError,
            retry: false,
            staleTime: 5000
          }
        );
};

export const useFetchTotalClicks = (token, onError) => {
    return useQuery("url-totalclick",
         async () => {
            const startDate = new Date(new Date().getFullYear(), 0, 1)
                .toISOString()
                .split("T")[0];
            const endDate = new Date().toISOString().split("T")[0];

            return await api.get(
                `/api/urls/totalClicks?startDate=${startDate}&endDate=${endDate}`
        );
    },
          {
            select: (data) => {
                // data.data =>
                    //  {
                    //     "2024-01-01": 120,
                    //     "2024-01-02": 95,
                    //     "2024-01-03": 110,
                    //   };
                      
                const convertToArray = Object.keys(data.data).map((key) => ({
                    clickDate: key,
                    count: data.data[key], // data.data[2024-01-01]
                }));
                // Object.keys(data.data) => ["2024-01-01", "2024-01-02", "2024-01-03"]

                // FINAL:
                //   [
                //     { clickDate: "2024-01-01", count: 120 },
                //     { clickDate: "2024-01-02", count: 95 },
                //     { clickDate: "2024-01-03", count: 110 },
                //   ]
                return convertToArray;
            },
            enabled: Boolean(token),
            onError,
            retry: false,
            staleTime: 5000
          }
        );
};
