import React, { useState } from "react";
import { MdCheckCircle, MdInfoOutline, MdDownload } from "react-icons/md";
import { RiFileExcel2Line } from "react-icons/ri";

const API_BASE_URL = "http://localhost:8080/api";

function AutoCleanResults({ results, onBack, fileName }) {
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const response = await fetch(
        `${API_BASE_URL}/download-cleaned?fileName=${fileName}`
      );
      if (!response.ok) throw new Error("Download failed");
      const blob = await response.blob();
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `Cleaned_${fileName}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      setDownloading(false);
    } catch (err) {
      console.error("Download error:", err);
      alert("Failed to download cleaned file");
      setDownloading(false);
    }
  };

  if (!results) {
    return (
      <div className="w-full max-w-screen-md mx-auto px-4 py-10">
        <div className="text-center text-gray-600">No results available</div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-screen-lg mx-auto px-4 py-10">
      <h2 className="text-3xl font-bold mb-6 text-center text-gray-800">
        Data Cleaning Results
      </h2>

      {/* Summary Card */}
      <div className="bg-gradient-to-r from-green-50 to-blue-50 border border-green-200 rounded-xl p-6 shadow-md mb-6">
        <div className="flex items-center gap-3 text-green-700 mb-4">
          <MdCheckCircle className="text-4xl" />
          <div className="flex-1">
            <div className="text-2xl font-semibold">Cleaning Complete!</div>
            <div className="text-sm text-green-600 mt-1">
              Your Excel file has been automatically cleaned and validated
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
          <div className="bg-white rounded-lg p-4 shadow-sm">
            <div className="text-sm text-gray-600">Total Rows</div>
            <div className="text-2xl font-bold text-gray-800">{results.totalRows}</div>
          </div>
          <div className="bg-white rounded-lg p-4 shadow-sm">
            <div className="text-sm text-gray-600">Rows Cleaned</div>
            <div className="text-2xl font-bold text-blue-600">{results.cleanedRows}</div>
          </div>
          <div className="bg-white rounded-lg p-4 shadow-sm">
            <div className="text-sm text-gray-600">Total Corrections</div>
            <div className="text-2xl font-bold text-green-600">{results.totalCorrections}</div>
          </div>
        </div>
      </div>

      {/* Column Types */}
      {results.columns && results.columns.length > 0 && (
        <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-md mb-6">
          <div className="flex items-center gap-2 text-gray-800 mb-4">
            <MdInfoOutline className="text-xl" />
            <h3 className="text-xl font-semibold">Column Types Detected</h3>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {results.columns.map((col, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200"
              >
                <div>
                  <span className="font-medium text-gray-700">Column {col.index}</span>
                  <span className="ml-2 px-2 py-1 bg-blue-100 text-blue-700 rounded text-sm font-medium">
                    {col.type}
                  </span>
                </div>
                {col.corrections > 0 && (
                  <span className="text-sm text-green-600 font-medium">
                    {col.corrections} fixes
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Sample Corrections */}
      {results.sampleCorrections && results.sampleCorrections.length > 0 && (
        <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-md mb-6">
          <h3 className="text-xl font-semibold text-gray-800 mb-4">
            Sample Corrections (showing first {results.sampleCorrections.length})
          </h3>
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {results.sampleCorrections.map((correction, idx) => (
              <div
                key={idx}
                className="flex items-center gap-4 p-3 bg-gray-50 rounded-lg border border-gray-200"
              >
                <div className="text-sm text-gray-600 min-w-[100px]">
                  Row {correction.row}, Col {correction.col}
                </div>
                <div className="flex-1 flex items-center gap-2">
                  <span className="px-2 py-1 bg-red-100 text-red-700 rounded text-sm line-through">
                    {correction.original || "(empty)"}
                  </span>
                  <span className="text-gray-400">→</span>
                  <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-sm font-medium">
                    {correction.cleaned || "(empty)"}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Actions */}
      <div className="flex flex-col md:flex-row gap-4 justify-center items-center mt-8">
        <button
          onClick={handleDownload}
          disabled={downloading}
          className="flex items-center gap-2 bg-green-600 text-white px-6 py-3 rounded-sm shadow hover:bg-green-700 transition cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <MdDownload className="text-xl" />
          {downloading ? "Downloading..." : "Download Cleaned File"}
        </button>
        <button
          onClick={onBack}
          className="flex items-center gap-2 bg-gray-800 text-white px-6 py-3 rounded-sm shadow hover:bg-transparent hover:text-gray-800 hover:border border transition cursor-pointer"
        >
          <RiFileExcel2Line />
          Upload Another File
        </button>
      </div>
    </div>
  );
}

export default AutoCleanResults;

