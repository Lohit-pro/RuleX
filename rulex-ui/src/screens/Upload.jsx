import React, { useState } from "react";
import { RiFileExcel2Line } from "react-icons/ri";
import { MdInbox } from "react-icons/md"; // Big empty inbox icon

const mockColumns = ["Name", "Email", "DOB", "Salary"];
const ruleOptions = ["numeric", "alphabet", "email", "date", "non-empty", "regex", "range"];

function Upload() {
  const [fileName, setFileName] = useState("");
  const [columns, setColumns] = useState([]);
  const [rules, setRules] = useState({});

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setFileName(file.name);

    // Simulated backend column fetch
    setColumns(mockColumns);

    const defaultRules = {};
    mockColumns.forEach((col) => {
      defaultRules[col] = "";
    });
    setRules(defaultRules);
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
    // TODO: Replace with actual POST call
    /*
    fetch('/api/submit-rules', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    */
  };

  return (
    <div className="w-full max-w-screen-xl mx-auto px-4 py-10 flex flex-col gap-8">
      {/* Upload Button */}
      <div className="flex flex-col items-center gap-4">
        <label
          htmlFor="excel-upload"
          className="px-6 py-3 bg-green-800 text-white border border-green-800 rounded-md cursor-pointer hover:bg-white hover:text-black hover:border-black flex items-center gap-2 transition-colors duration-300"
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
        {fileName && <div className="text-lg font-semibold">Uploaded: {fileName}</div>}
      </div>

      {/* Empty State Placeholder */}
      {!fileName && (
        <div className="flex flex-col items-center justify-center text-gray-400 mt-16">
          <MdInbox className="text-[120px]" />
          <div className="text-xl font-medium">Nothing to show here yet</div>
          <div className="text-sm">Upload an Excel file to begin setting validation rules</div>
        </div>
      )}

      {/* Rules Form */}
      {columns.length > 0 && (
        <div className="flex flex-col gap-6">
          <div className="text-2xl font-bold text-gray-800">Set Validation Rules:</div>

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

          {/* Submit Button */}
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
