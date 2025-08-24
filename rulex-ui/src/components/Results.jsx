import React, { useState } from "react";
import { MdCheckCircle, MdErrorOutline } from "react-icons/md";

function Results({ results, onBack, fileName }) {
  const [emailButtonTrigger, setEmailButtonTrigger] = useState(false);
  const [showAll, setShowAll] = useState(false);
  const maxToShow = 10;
  const visibleResults = showAll ? results : results.slice(0, maxToShow);

  const handleDownload = () => {
    fetch(`https://rulex-api.onrender.com/api/download?fileName=${fileName}`)
      .then((response) => {
        if (!response.ok) throw new Error("Download failed");
        return response.blob();
      })
      .then((blob) => {
        const url = window.URL.createObjectURL(new Blob([blob]));
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", `Report_${fileName}`);
        document.body.appendChild(link);
        link.click();
        link.remove();
      })
      .catch((err) => console.error("Download error:", err));
  };

  return (
    <div className="w-full max-w-screen-md mx-auto px-4 py-10">
      <h2 className="text-3xl font-bold mb-6 text-center text-gray-800">
        Validation Results
      </h2>

      {results.length === 0 ? (
        <div className="flex flex-col items-center justify-center text-green-600 text-lg">
          <MdCheckCircle className="text-[100px]" />
          <div className="mt-4 text-2xl font-semibold">All Good!</div>
          <div className="text-base text-gray-600">
            No issues found in the uploaded file.
          </div>
        </div>
      ) : (
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <div className="flex items-center gap-3 text-red-700">
            <MdErrorOutline className="text-3xl" />
            <div className="text-xl font-semibold">Validation Issues Found</div>
          </div>

          <div className="mt-3 text-sm text-red-600">
            Please review the following cells with incorrect values:
          </div>

          <ul className="mt-2 list-disc list-inside text-red-800 space-y-1 text-sm max-h-60 overflow-y-auto">
            {visibleResults.map((res, idx) => (
              <li key={idx}>{res}</li>
            ))}
          </ul>

          {/* Toggle button */}
          {results.length > maxToShow && (
            <div className="mt-2">
              <button
                onClick={() => setShowAll((prev) => !prev)}
                className="text-sm text-blue-600 hover:underline"
              >
                {showAll ? "Show Less" : `Show All (${results.length})`}
              </button>
            </div>
          )}

          <div className="mt-5 flex justify-end gap-2">
            <div className="flex flex-col gap-2">
              <button
                onClick={() => {
                  setEmailButtonTrigger((prev) => !prev);
                }}
                className="bg-gray-800 cursor-pointer text-white hover:bg-white hover:border border hover:text-black hover:border-black text-sm font-medium px-4 py-2 rounded transition"
              >
                Send Report to Email
              </button>
              {emailButtonTrigger && <input type="email" className="w-full" />}
            </div>
            <button
              onClick={handleDownload}
              className="bg-red-600 cursor-pointer text-white text-sm font-medium px-4 py-2 rounded hover:bg-white hover:text-red-600 hover:border border transition"
            >
              Download Report
            </button>
          </div>
        </div>
      )}

      <div className="mt-8 text-center">
        <button
          onClick={onBack}
          className="px-5 py-2 bg-gray-800 text-white rounded-md hover:bg-white hover:text-black hover:border-black border transition-colors cursor-pointer shadow-lg"
        >
          Upload Another Excel
        </button>
      </div>
    </div>
  );
}

export default Results;
