import { useState } from "react";
import { RiFileExcel2Line } from "react-icons/ri";
import { MdInbox, MdAutoFixHigh } from "react-icons/md";
import Results from "../components/Results";
import AutoCleanResults from "../components/AutoCleanResults";
import Loading from "../components/Loading";
import Footer from "../components/Footer";

const API_BASE_URL = "http://localhost:8080/api";

const ruleOptions = [
  "Numeric",
  "Alphabet",
  "AlphaNumeric",
  "Date",
  "Email",
  "Non-empty",
  "Check In API",
];

export default function Upload() {
  const [fileName, setFileName] = useState("");
  const [uploadedFile, setUploadedFile] = useState(null); // Store the file object
  const [columns, setColumns] = useState([]);
  const [rules, setRules] = useState({});
  const [showResults, setShowResults] = useState(false);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [mode, setMode] = useState("validation"); // "validation" or "autoclean"
  const [autoCleanResults, setAutoCleanResults] = useState(null);
  const [isAutoCleaning, setIsAutoCleaning] = useState(false);

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    // Store the file for later use in auto-clean
    setUploadedFile(file);
    setLoading(true);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch(`${API_BASE_URL}/headers`, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Failed to fetch");
      }

      const headers = await response.json();
      setColumns(Object.keys(headers));

      const defaultRules = {};
      Object.entries(headers).forEach(([column, rule]) => {
        defaultRules[column] = { rule };
      });

      console.log(defaultRules);

      setRules(defaultRules);
      setLoading(false);
      setFileName(file.name);
    } catch (error) {
      console.error("Error uploading file or fetching headers:", error);
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    const payload = {
      fileName,
      rules: Object.entries(rules).map(([column, config]) => ({
        column,
        rule: {
          type: config.rule,
          min: config.min || null,
          max: config.max || null,
          caseType: config.caseType ? config.caseType : "both",
          apiUrl: config.apiUrl || null,
          nonEmpty: config.nonEmpty || false,
        },
      })),
    };

    try {
      const response = await fetch(`${API_BASE_URL}/validate`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        console.error("Validation failed with status", response.status);
        setResults([]);
        return;
      }

      const resultData = await response.json();
      setResults(resultData);
    } catch (error) {
      console.error("Error Validating the file :", error);
    }

    setShowResults(true);
  };

  const handleAutoClean = async () => {
    if (!uploadedFile) {
      alert("Please upload a file first");
      return;
    }
    
    setIsAutoCleaning(true);
    const formData = new FormData();
    
    // Use the stored file reference
    formData.append("file", uploadedFile);

    try {
      const response = await fetch(`${API_BASE_URL}/autoclean?aiOnly=true`, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Auto-clean failed");
      }

      const cleanResult = await response.json();
      setAutoCleanResults(cleanResult);
      setMode("autoclean");
      setShowResults(true);
    } catch (error) {
      console.error("Error auto-cleaning file:", error);
      alert("Failed to auto-clean file. Please try again.");
    } finally {
      setIsAutoCleaning(false);
    }
  };

  const handleBackToUpload = () => {
    setFileName("");
    setUploadedFile(null);
    setColumns([]);
    setRules({});
    setShowResults(false);
    setResults([]);
    setAutoCleanResults(null);
    setMode("validation");
    // Reset file input
    const fileInput = document.getElementById("excel-upload");
    if (fileInput) fileInput.value = "";
  };

  if (showResults && mode === "autoclean")
    return (
      <div className="flex flex-col gap-2">
        <AutoCleanResults results={autoCleanResults} onBack={handleBackToUpload} fileName={fileName} />
        <Footer />
      </div>
    );

  if (showResults && mode === "validation")
    return (
      <div className="flex flex-col gap-2">
        <Results results={results} onBack={handleBackToUpload} fileName={fileName} />
        <Footer />
      </div>
    );

  if (loading) return <Loading text="Extracting headers..." />;
  if (isAutoCleaning) return <Loading text="Auto-cleaning your Excel file..." />;

  return (
    <div className="w-full flex overflow-hidden min-h-screen">
      <div className="w-full max-w-screen-xl mx-auto px-4 py-10 flex flex-col gap-8 overflow-y-auto z-10">
        <div className="flex flex-col items-center gap-4">
          {/* Mode Selector */}
          <div className="flex gap-4 mb-4">
            <button
              onClick={() => setMode("validation")}
              className={`px-4 py-2 rounded-sm transition-colors ${
                mode === "validation"
                  ? "bg-red-600 text-white border-2 border-red-600"
                  : "bg-white text-gray-700 border-2 border-gray-300 hover:border-gray-400"
              }`}
            >
              Validation Mode
            </button>
            <button
              onClick={() => setMode("autoclean")}
              className={`px-4 py-2 rounded-sm transition-colors flex items-center gap-2 ${
                mode === "autoclean"
                  ? "bg-green-600 text-white border-2 border-green-600"
                  : "bg-white text-gray-700 border-2 border-gray-300 hover:border-gray-400"
              }`}
            >
              <MdAutoFixHigh />
              Auto Clean Mode
            </button>
          </div>

          <label
            htmlFor="excel-upload"
            className="px-6 py-3 bg-green-800 shadow-lg text-white border border-green-800 rounded-sm cursor-pointer hover:bg-white hover:text-black hover:border-black flex items-center gap-2 transition-colors duration-300"
          >
            <span>Upload Excel File</span>
            <RiFileExcel2Line />
          </label>
          <input
            type="file"
            id="excel-upload"
            accept=".xlsx, .xls"
            onChange={handleFileChange}
            className="hidden"
          />
          {fileName && (
            <div className="text-lg font-semibold">Uploaded: {fileName}</div>
          )}
        </div>

        {!fileName && (
          <div className="flex flex-col items-center justify-center text-gray-400 mt-16">
            <MdInbox className="text-[120px]" />
            <div className="text-xl font-medium">Nothing to show here yet</div>
            <div className="text-sm">
              Upload an Excel file to begin setting validation rules
            </div>
          </div>
        )}

        {columns.length > 0 && mode === "autoclean" && (
          <div className="flex flex-col gap-6 items-center">
            <div className="text-2xl font-bold text-gray-800 mb-4">
              Auto Clean Mode
            </div>
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 max-w-2xl">
              <div className="text-lg font-semibold text-blue-800 mb-2">
                🤖 Intelligent Data Cleaning
              </div>
              <div className="text-gray-700 space-y-2">
                <p>• Automatically detects column types (Email, Date, Name, Phone, etc.)</p>
                <p>• Cleans and normalizes data using smart algorithms</p>
                <p>• Fixes formatting issues and invalid entries</p>
                <p>• Optionally uses AI for enhanced type inference</p>
              </div>
              <div className="mt-4 text-sm text-gray-600">
                Just click "Auto Clean Excel" below to process your file!
              </div>
            </div>
            <button
              onClick={handleAutoClean}
              className="cursor-pointer px-6 py-3 mb-2 bg-green-600 text-white text-lg rounded-sm hover:bg-white hover:text-black hover:border-black border border-green-600 transition-colors duration-300 flex items-center gap-2"
            >
              <MdAutoFixHigh />
              Auto Clean Excel
            </button>
          </div>
        )}

        {columns.length > 0 && mode === "validation" && (
          <div className="flex flex-col gap-6">
            <div className="text-2xl font-bold text-gray-800">
              Set Validation Rules:
            </div>

            {columns.map((column, index) => (
              <div
                key={index}
                className="flex flex-col gap-2 p-4 rounded-md shadow-sm border border-gray-400 max-w-2xl"
              >
                <div className="font-medium text-lg">{column}</div>

                <select
                  value={rules[column]?.rule || ""}
                  onChange={(e) =>
                    setRules((prev) => ({
                      ...prev,
                      [column]: { ...prev[column], rule: e.target.value },
                    }))
                  }
                  className="border border-gray-300 bg-white rounded-md px-3 py-2 w-full md:w-64 focus:outline-none focus:ring-2 focus:ring-red-400"
                >
                  <option value="">-- Select Rule --</option>
                  {ruleOptions.map((rule) => (
                    <option key={rule} value={rule}>
                      {rule}
                    </option>
                  ))}
                </select>

                {rules[column]?.rule === "Numeric" && (
                  <div className="flex gap-4">
                    <input
                      type="number"
                      placeholder="Min"
                      className="border px-2 py-1 rounded w-24 bg-white"
                      onChange={(e) =>
                        setRules((prev) => ({
                          ...prev,
                          [column]: { ...prev[column], min: e.target.value },
                        }))
                      }
                    />
                    <input
                      type="number"
                      placeholder="Max"
                      className="border px-2 py-1 rounded w-24 bg-white"
                      onChange={(e) =>
                        setRules((prev) => ({
                          ...prev,
                          [column]: { ...prev[column], max: e.target.value },
                        }))
                      }
                    />
                  </div>
                )}

                {rules[column]?.rule === "Alphabet" && (
                  <div className="flex gap-4">
                    <label>
                      <input
                        type="radio"
                        name={`alphabet-${index}`}
                        value="upper"
                        checked={rules[column]?.caseType === "upper"}
                        onChange={(e) =>
                          setRules((prev) => ({
                            ...prev,
                            [column]: {
                              ...prev[column],
                              caseType: e.target.value,
                            },
                          }))
                        }
                      />{" "}
                      UPPERCASE
                    </label>
                    <label>
                      <input
                        type="radio"
                        name={`alphabet-${index}`}
                        value="lower"
                        checked={rules[column]?.caseType === "lower"}
                        onChange={(e) =>
                          setRules((prev) => ({
                            ...prev,
                            [column]: {
                              ...prev[column],
                              caseType: e.target.value,
                            },
                          }))
                        }
                      />{" "}
                      lowercase
                    </label>
                    <label>
                      <input
                        type="radio"
                        name={`alphabet-${index}`}
                        value="both"
                        checked={rules[column]?.caseType === "both"}
                        onChange={(e) =>
                          setRules((prev) => ({
                            ...prev,
                            [column]: {
                              ...prev[column],
                              caseType: e.target.value,
                            },
                          }))
                        }
                      />{" "}
                      Both
                    </label>
                  </div>
                )}

                {rules[column]?.rule === "Check In API" && (
                  <div className="flex gap-4">
                    <input
                      type="text"
                      className="border border-gray-300 px-2 py-1 rounded w-96 focus:outline-none focus:ring-2 focus:ring-red-400 bg-white"
                      placeholder="Enter API link"
                      value={rules[column]?.apiValue || ""}
                      onChange={(e) =>
                        setRules((prev) => ({
                          ...prev,
                          [column]: {
                            ...prev[column],
                            apiValue: e.target.value,
                          },
                        }))
                      }
                    />
                  </div>
                )}
              </div>
            ))}

            <button
              onClick={handleSubmit}
              className="cursor-pointer self-start px-6 py-2 mb-2 bg-red-600 text-white text-lg rounded-sm hover:bg-white hover:text-black hover:border-black border border-red-600 transition-colors duration-300"
            >
              Submit Rules
            </button>
          </div>
        )}
      </div>

      {fileName && (
        <div className="fixed right-0 top-0 h-screen flex items-center justify-center pt-32 z-0">
          <img src="./upload-bg.jpg" className="w-[33rem] mr-28" />
        </div>
      )}
    </div>
  );
}
