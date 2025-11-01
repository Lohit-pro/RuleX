import React, { useState } from "react";
import { MdCheckCircle, MdErrorOutline } from "react-icons/md";
import { RiFileExcel2Line } from "react-icons/ri";

const API_BASE_URL = "http://localhost:8080/api";

function Results({ results, onBack, fileName }) {
  const [emailButtonTrigger, setEmailButtonTrigger] = useState(false);
  const [toEmail, setToEmail] = useState("");
  const [showAll, setShowAll] = useState(false);
  const maxToShow = 10;
  const visibleResults = showAll ? results : results.slice(0, maxToShow);

  const handleDownload = () => {
    fetch(`${API_BASE_URL}/download?fileName=${fileName}`)
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

const handleEmailSubmit = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/email`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        toEmail: toEmail,
        fileName: fileName,
      }),
    });

    if (!response.ok) {
      throw new Error("Failed to send email");
    }

    alert("Email sent successfully!");
  } catch (err) {
    console.error(err);
    alert("Error sending email");
  }
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
      <div className="bg-red-50 border border-red-200 rounded-xl p-6 shadow-md">
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
              className="text-sm text-blue-600 hover:text-blue-800 font-medium transition"
            >
              {showAll ? "Show Less" : `Show All (${results.length})`}
            </button>
          </div>
        )}

        <div className="mt-5 flex justify-end gap-4">
          {/* Email button + input */}
          <div className="flex flex-col gap-2 w-1/2">
            <button
              onClick={() => setEmailButtonTrigger((prev) => !prev)}
              className="bg-gray-800 cursor-pointer text-white text-sm font-medium px-4 py-2 rounded-sm shadow hover:bg-transparent hover:text-gray-800 hover:border border transition"
            >
              Send Report to Email
            </button>

            {emailButtonTrigger && (
              <div className="flex gap-2">
                <input
                  type="email"
                  placeholder="Enter email address"
                  className="flex-1 border border-gray-300 rounded-sm px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gray-400"
                  onChange={(e) => setToEmail(e.target.value)}
                />
                <button
                  onClick={handleEmailSubmit}
                  className="bg-gray-800 text-white text-sm px-4 py-2 rounded-sm shadow hover:bg-transparent hover:text-gray-800 hover:border border cursor-pointer transition"
                >
                  Send
                </button>
              </div>
            )}
          </div>

          {/* Download button */}
          <button
            onClick={handleDownload}
            className="bg-red-600 cursor-pointer text-white text-sm font-medium px-5 py-2 rounded-sm shadow hover:bg-red-700 transition"
          >
            Download Report
          </button>
        </div>
      </div>
    )}

    <div className="mt-8 text-center">
      <button
        onClick={onBack}
        className="px-6 py-2 bg-gray-800 text-white rounded-sm shadow hover:bg-transparent hover:text-gray-800 hover:border border cursor-pointer transition"
      >
        <div className="flex items-center gap-1">
          Upload Another File
          <RiFileExcel2Line />
        </div>
      </button>
    </div>
  </div>
);

}

export default Results;
