import React from "react";
import { RiFileExcel2Line } from "react-icons/ri";
import { Link } from "react-router-dom";
import UploadButton from "./UploadButton";

function Dashboard() {
  return (
    <div className="w-full min-h-[90vh] bg-gradient-to-r from-white via-red-50 to-red-200 flex items-center justify-center px-6">
      <div className="max-w-screen-xl w-full flex flex-col md:flex-row items-center justify-between gap-10 py-10 md:py-20">

        <div className="flex-1 flex flex-col justify-center items-start gap-5 text-center md:text-left">
          <h1 className="text-4xl md:text-6xl font-extrabold text-gray-800 leading-tight">
            Rule<span className="text-red-500">X</span> Engine
          </h1>
          <p className="text-lg md:text-2xl max-w-xl text-gray-700">
            Upload your Excel, set a few rules, and let the app find the issues
            for you — <span className="text-red-500 font-semibold">faster, easier, and zero manual checks.</span>
          </p>
          <UploadButton link='/upload' text='Upload Excel' />
        </div>

        <div className="flex-1 flex justify-center items-center">
          <img
            src="/image.png"
            alt="Excel Illustration"
            className="max-w-md md:max-w-xl w-full"
          />
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
