import React, { useState, useEffect } from "react";
import { MdCheckCircle, MdInfoOutline, MdDownload, MdFeedback, MdRefresh, MdClose } from "react-icons/md";
import { RiFileExcel2Line } from "react-icons/ri";

const API_BASE_URL = "http://localhost:8080/api";

function AutoCleanResults({ results, onBack, fileName }) {
  const [downloading, setDownloading] = useState(false);
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [feedbackText, setFeedbackText] = useState("");
  const [refining, setRefining] = useState(false);
  const [currentResults, setCurrentResults] = useState(results);
  const [refinementHistory, setRefinementHistory] = useState([]);

  // Update currentResults when results prop changes
  useEffect(() => {
    if (results) {
      setCurrentResults(results);
      // Debug: Log to see if AI inference data is present
      console.log("Results received:", results);
      console.log("AI Inference:", results.aiInference);
      console.log("Column Samples:", results.columnSamples);
    }
  }, [results]);

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

  const handleCustomFeedback = () => {
    setShowFeedbackModal(true);
  };

  const handleAutoRegenerate = async () => {
    if (!currentResults?.aiInference || !currentResults?.columnSamples) {
      alert("No AI inference data available for refinement");
      return;
    }

    setRefining(true);
    try {
      const response = await fetch(`${API_BASE_URL}/refine-inference`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          fileName: fileName,
          previousInference: currentResults.aiInference,
          columnSamples: currentResults.columnSamples,
          feedback: null,
          autoRegenerate: true,
        }),
      });

      if (!response.ok) {
        const errorMessage = response.headers.get("X-Error-Message") || "Refinement failed";
        if (response.status === 429) {
          alert("OpenAI API quota exceeded. The feedback feature requires an active OpenAI subscription. Please check your billing or try again later.");
        } else {
          alert(errorMessage);
        }
        throw new Error(errorMessage);
      }

      const refinedResult = await response.json();
      
      // Store previous result in history
      setRefinementHistory([...refinementHistory, currentResults]);
      
      // Update current results with refined data
      setCurrentResults({
        ...currentResults,
        ...refinedResult,
        columns: refinedResult.columns || currentResults.columns,
      });
      
    } catch (err) {
      console.error("Refinement error:", err);
      alert("Failed to refine inference. Please try again.");
    } finally {
      setRefining(false);
    }
  };

  const handleSubmitFeedback = async () => {
    if (!feedbackText.trim()) {
      alert("Please provide feedback");
      return;
    }

    if (!currentResults?.aiInference || !currentResults?.columnSamples) {
      alert("No AI inference data available for refinement");
      return;
    }

    setRefining(true);
    setShowFeedbackModal(false);
    
    try {
      const response = await fetch(`${API_BASE_URL}/refine-inference`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          fileName: fileName,
          previousInference: currentResults.aiInference,
          columnSamples: currentResults.columnSamples,
          feedback: feedbackText,
          autoRegenerate: false,
        }),
      });

      if (!response.ok) {
        const errorMessage = response.headers.get("X-Error-Message") || "Refinement failed";
        if (response.status === 429) {
          alert("OpenAI API quota exceeded. The feedback feature requires an active OpenAI subscription. Please check your billing or try again later.");
        } else {
          alert(errorMessage);
        }
        throw new Error(errorMessage);
      }

      const refinedResult = await response.json();
      
      // Store previous result in history
      setRefinementHistory([...refinementHistory, currentResults]);
      
      // Update current results with refined data
      setCurrentResults({
        ...currentResults,
        ...refinedResult,
        columns: refinedResult.columns || currentResults.columns,
      });
      
      setFeedbackText("");
      
    } catch (err) {
      console.error("Refinement error:", err);
      alert("Failed to refine inference. Please try again.");
    } finally {
      setRefining(false);
    }
  };

  // Use currentResults if available, otherwise fallback to results prop
  const displayResults = currentResults || results;

  if (!displayResults) {
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
            <div className="text-2xl font-bold text-gray-800">{displayResults.totalRows}</div>
          </div>
          <div className="bg-white rounded-lg p-4 shadow-sm">
            <div className="text-sm text-gray-600">Rows Cleaned</div>
            <div className="text-2xl font-bold text-blue-600">{displayResults.cleanedRows}</div>
          </div>
          <div className="bg-white rounded-lg p-4 shadow-sm">
            <div className="text-sm text-gray-600">Total Corrections</div>
            <div className="text-2xl font-bold text-green-600">{displayResults.totalCorrections}</div>
          </div>
        </div>
        
        {/* Refinement Controls */}
        {displayResults.aiInference ? (
          <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
            <div className="flex items-center justify-between">
              <div className="text-sm text-yellow-800">
                <strong>Not satisfied with the results?</strong> Refine the AI inference to improve accuracy.
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleCustomFeedback}
                  disabled={refining}
                  className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-sm hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed text-sm"
                >
                  <MdFeedback />
                  Custom Feedback
                </button>
                <button
                  onClick={handleAutoRegenerate}
                  disabled={refining}
                  className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-sm hover:bg-purple-700 transition disabled:opacity-50 disabled:cursor-not-allowed text-sm"
                >
                  <MdRefresh />
                  {refining ? "Refining..." : "Auto Regenerate"}
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="mt-4 p-4 bg-gray-50 border border-gray-200 rounded-lg">
            <div className="text-sm text-gray-600">
              <strong>AI inference not available for this run.</strong>
              <div className="text-xs mt-1">Possible reasons:
                <ul className="list-disc list-inside">
                  <li>AI disabled (enable Hugging Face or OpenAI in application.properties)</li>
                  <li>Quota/rate limit reached</li>
                  <li>No AI was used (try Auto Clean mode)</li>
                </ul>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Column Types */}
      {displayResults.columns && displayResults.columns.length > 0 && (
        <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-md mb-6">
          <div className="flex items-center gap-2 text-gray-800 mb-4">
            <MdInfoOutline className="text-xl" />
            <h3 className="text-xl font-semibold">Column Types Detected</h3>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {displayResults.columns.map((col, idx) => (
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
      {displayResults.sampleCorrections && displayResults.sampleCorrections.length > 0 && (
        <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-md mb-6">
          <h3 className="text-xl font-semibold text-gray-800 mb-4">
            Sample Corrections (showing first {displayResults.sampleCorrections.length})
          </h3>
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {displayResults.sampleCorrections.map((correction, idx) => (
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

      {/* Feedback Modal */}
      {showFeedbackModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 shadow-xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-semibold text-gray-800">Provide Custom Feedback</h3>
              <button
                onClick={() => {
                  setShowFeedbackModal(false);
                  setFeedbackText("");
                }}
                className="text-gray-500 hover:text-gray-700"
              >
                <MdClose className="text-2xl" />
              </button>
            </div>
            <p className="text-sm text-gray-600 mb-4">
              Tell us what you'd like to improve about the AI inference. Be specific about which columns or types need adjustment.
            </p>
            <textarea
              value={feedbackText}
              onChange={(e) => setFeedbackText(e.target.value)}
              placeholder="e.g., Column 2 should be detected as DATE instead of NUMERIC, or Column 3 needs better cleaning rules for phone numbers..."
              className="w-full border border-gray-300 rounded-lg p-4 h-32 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <div className="flex gap-3 mt-4 justify-end">
              <button
                onClick={() => {
                  setShowFeedbackModal(false);
                  setFeedbackText("");
                }}
                className="px-4 py-2 bg-gray-300 text-gray-700 rounded-sm hover:bg-gray-400 transition"
              >
                Cancel
              </button>
              <button
                onClick={handleSubmitFeedback}
                disabled={refining || !feedbackText.trim()}
                className="px-4 py-2 bg-blue-600 text-white rounded-sm hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {refining ? "Submitting..." : "Submit Feedback"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AutoCleanResults;

