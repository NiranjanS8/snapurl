import React from "react";
import { Bar } from "react-chartjs-2";
import {
  Chart as ChartJS,
  BarElement,
  CategoryScale,
  LinearScale,
  Legend,
  Tooltip,
  Filler,
} from "chart.js";

ChartJS.register(
  BarElement,
  Tooltip,
  CategoryScale,
  LinearScale,
  Legend,
  Filler
);

const Graph = ({ graphData }) => {
  const labels = graphData?.map((item) => `${item.clickDate}`);
  const clicksPerDay = graphData?.map((item) => item.count ?? item.clickCount);

  const data = {
    labels:
      graphData.length > 0
        ? labels
        : ["", "", "", "", "", "", "", "", "", "", "", "", "", ""],
    datasets: [
      {
        label: "Total Clicks",
        data:
          graphData.length > 0
            ? clicksPerDay
            : [1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1],
        backgroundColor: graphData.length > 0 ? "rgba(60, 65, 92, 0.78)" : "rgba(180, 165, 165, 0.1)",
        borderRadius: 12,
        borderSkipped: false,
        hoverBackgroundColor: "rgba(48, 27, 63, 0.82)",
        barThickness: 22,
        categoryPercentage: 0.72,
        barPercentage: 0.9,
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
        padding: 12,
        cornerRadius: 12,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        border: {
          display: false,
        },
        grid: {
          color: "rgba(180, 165, 165, 0.08)",
          drawTicks: false,
        },
        ticks: {
          color: "#B4A5A5",
          font: {
            size: 12,
            weight: "600",
          },
          callback: function (value) {
            if (Number.isInteger(value)) {
              return value.toString();
            }
            return "";
          },
        },
        title: {
          display: true,
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
          display: false,
          drawTicks: false,
        },
        ticks: {
          color: "#B4A5A5",
          font: {
            size: 11,
            weight: "600",
          },
          maxRotation: 0,
          minRotation: 0,
        },
        title: {
          display: true,
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

  return <Bar className="w-full" data={data} options={options} />;
};

export default Graph;
