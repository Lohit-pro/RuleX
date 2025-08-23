import { useState } from "react";
import { RiFileExcel2Line } from "react-icons/ri";
import { MdInbox } from "react-icons/md";
import Results from "../components/Results";
import Loading from "../components/Loading";
import Footer from "../components/Footer";

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
  const [columns, setColumns] = useState([]);
  const [rules, setRules] = useState({});
  const [showResults, setShowResults] = useState(false);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setLoading(true);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch("https://rulex-api.onrender.com/api/headers", {
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
        ...config,
      })),
    };

    console.log("Submitting to backend:", payload);

    // const mockResults = [
    //   'Invalid email format at Row 5, Column "Email": value = "john.doe.com"',
    //   'Non-numeric value at Row 8, Column "Age": value = "twenty-five"',
    //   'Empty value at Row 3, Column "Date of Joining"',
    //   'Invalid date format at Row 6, Column "Start Date": value = "31-13-2022"',
    // ];

    try {
      const response = await fetch("https://rulex-api.onrender.com/api/validate", {
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

    // setResults(mockResults);
    setShowResults(true);
  };

  const handleBackToUpload = () => {
    setFileName("");
    setColumns([]);
    setRules({});
    setShowResults(false);
    setResults([]);
  };

  if (showResults)
    return (
      <div className="flex flex-col gap-32">
        <Results results={results} onBack={handleBackToUpload} />
        <Footer />
      </div>
    );

  if (loading) return <Loading text="Extracting headers..." />;

  return (
    <div className="w-full flex overflow-hidden min-h-screen">
      <div className="w-full max-w-screen-xl mx-auto px-4 py-10 flex flex-col gap-8 overflow-y-auto z-10">
        <div className="flex flex-col items-center gap-4">
          <label
            htmlFor="excel-upload"
            className="px-6 py-3 bg-green-800 shadow-lg text-white border border-green-800 rounded-md cursor-pointer hover:bg-white hover:text-black hover:border-black flex items-center gap-2 transition-colors duration-300"
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

        {columns.length > 0 && (
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
              checked={rules[column]?.case === "upper"}
              onChange={(e) =>
                setRules((prev) => ({
                  ...prev,
                  [column]: { ...prev[column], case: e.target.value },
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
              checked={rules[column]?.case === "lower"}
              onChange={(e) =>
                setRules((prev) => ({
                  ...prev,
                  [column]: { ...prev[column], case: e.target.value },
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
              checked={rules[column]?.case === "both"}
              onChange={(e) =>
                setRules((prev) => ({
                  ...prev,
                  [column]: { ...prev[column], case: e.target.value },
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
              className="cursor-pointer self-start px-6 py-2 mb-2 bg-red-600 text-white text-lg rounded-md hover:bg-white hover:text-black hover:border-black border border-red-600 transition-colors duration-300"
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
