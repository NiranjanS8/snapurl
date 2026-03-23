import React from "react";
import { Line } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  Legend,
  Tooltip,
  Filler,
  LineElement,
  PointElement,
} from "chart.js";

ChartJS.register(
  Tooltip,
  CategoryScale,
  LinearScale,
  Legend,
  Filler,
  LineElement,
  PointElement
);

const Graph = ({ graphData, compact = false }) => {
  const sortedGraphData = [...(graphData || [])].sort(
    (firstItem, secondItem) => new Date(firstItem.clickDate) - new Date(secondItem.clickDate)
  );
  const labels = sortedGraphData.map((item) => `${item.clickDate}`);
  const clicksPerDay = sortedGraphData.map((item) => item.count ?? item.clickCount);

  const data = {
    labels:
      sortedGraphData.length > 0
        ? labels
        : ["", "", "", "", "", "", "", "", "", "", "", "", "", ""],
    datasets: [
      {
        label: "Total Clicks",
        data:
          sortedGraphData.length > 0
            ? clicksPerDay
            : [1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1],
        borderColor: sortedGraphData.length > 0 ? "rgba(122, 90, 248, 0.95)" : "rgba(180, 165, 165, 0.28)",
        backgroundColor: sortedGraphData.length > 0 ? "rgba(122, 90, 248, 0.12)" : "rgba(180, 165, 165, 0.06)",
        fill: true,
        tension: 0.38,
        borderWidth: compact ? 2.25 : 3,
        pointRadius: sortedGraphData.length > 0 ? (compact ? 2 : 3) : 0,
        pointHoverRadius: compact ? 4 : 5,
        pointBackgroundColor: sortedGraphData.length > 0 ? "#7A5AF8" : "rgba(180, 165, 165, 0.3)",
        pointBorderWidth: 0,
        pointHitRadius: compact ? 12 : 16,
      },
    ],
  };

  const options = {
    maintainAspectRatio: false,
    responsive: true,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        backgroundColor: "#151515",
        titleColor: "#ffffff",
        bodyColor: "#B4A5A5",
        displayColors: false,
        padding: compact ? 10 : 12,
        cornerRadius: compact ? 10 : 12,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        border: {
          display: false,
        },
        grid: {
          color: "rgba(180, 165, 165, 0.06)",
          drawTicks: false,
        },
        ticks: {
          color: "#B4A5A5",
          font: {
            size: compact ? 10 : 12,
            weight: "600",
          },
          padding: compact ? 6 : 8,
          callback: function (value) {
            if (Number.isInteger(value)) {
              return value.toString();
            }
            return "";
          },
        },
        title: {
          display: !compact,
          text: "Clicks",
          font: {
            size: 13,
            weight: "700",
          },
          color: "#ffffff",
        },
      },
      x: {
        border: {
          display: false,
        },
        grid: {
          color: "rgba(180, 165, 165, 0.04)",
          drawTicks: false,
        },
        ticks: {
          color: "#B4A5A5",
          font: {
            size: compact ? 10 : 11,
            weight: "600",
          },
          padding: compact ? 6 : 8,
          maxRotation: 0,
          minRotation: 0,
        },
        title: {
          display: !compact,
          text: "Date",
          font: {
            size: 13,
            weight: "700",
          },
          color: "#ffffff",
        },
      },
    },
  };

  return <Line className={compact ? "h-full w-full" : "w-full"} data={data} options={options} />;
};

export default Graph;
