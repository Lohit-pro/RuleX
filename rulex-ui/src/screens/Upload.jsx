import { useState } from "react";
import { RiFileExcel2Line } from "react-icons/ri";
import { MdInbox } from "react-icons/md";
import * as XLSX from "xlsx";
import Results from "../components/Results";

const ruleOptions = [
  "numeric",
  "alphabet",
  "email",
  "date",
  "non-empty",
  "regex",
  "range",
];

function Upload() {
  const [fileName, setFileName] = useState("");
  const [columns, setColumns] = useState([]);
  const [rules, setRules] = useState({});
  const [showResults, setShowResults] = useState(false);
  const [results, setResults] = useState([]);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const data = new Uint8Array(event.target.result);
      const workbook = XLSX.read(data, { type: "array" });
      const sheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[sheetName];
      const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });

      const firstRow = jsonData[0];
      setColumns(firstRow);

      const defaultRules = {};
      firstRow.forEach((col) => {
        defaultRules[col] = "";
      });
      setRules(defaultRules);
      setFileName(file.name);
    };

    reader.readAsArrayBuffer(file);
  };

  const handleRuleChange = (column, selectedRule) => {
    setRules((prev) => ({
      ...prev,
      [column]: selectedRule,
    }));
  };

  const handleSubmit = () => {
    const payload = {
      fileName,
      columnRules: Object.entries(rules).map(([column, rule]) => ({
        column,
        rule,
      })),
    };

    console.log("Submitting to backend:", payload);

    const mockResults = [
      'Invalid email format at Row 5, Column "Email": value = "john.doe.com"',
      'Non-numeric value at Row 8, Column "Age": value = "twenty-five"',
      'Empty value at Row 3, Column "Date of Joining"',
      'Invalid date format at Row 6, Column "Start Date": value = "31-13-2022"',
      'Regex mismatch at Row 4, Column "Employee ID": value = "#EMP123"',
    ];

    setResults(mockResults);
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
    return <Results results={results} onBack={handleBackToUpload} />;

  return (
    <div className="w-full max-w-screen-xl mx-auto px-4 py-10 flex flex-col gap-8">
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
              className="flex flex-col md:flex-row items-start md:items-center gap-4 bg-gray-50 p-4 rounded-md shadow"
            >
              <div className="font-medium w-40">{column}</div>
              <select
                value={rules[column]}
                onChange={(e) => handleRuleChange(column, e.target.value)}
                className="border border-gray-300 rounded-md px-3 py-2 w-full md:w-64 focus:outline-none focus:ring-2 focus:ring-red-400"
              >
                <option value="">-- Select Rule --</option>
                {ruleOptions.map((rule) => (
                  <option key={rule} value={rule}>
                    {rule}
                  </option>
                ))}
              </select>
            </div>
          ))}

          <button
            onClick={handleSubmit}
            className="cursor-pointer self-start px-6 py-2 bg-red-600 text-white text-lg rounded-md hover:bg-white hover:text-black hover:border-black border border-red-600 transition-colors duration-300"
          >
            Submit Rules
          </button>
        </div>
      )}
    </div>
  );
}

export default Upload;
