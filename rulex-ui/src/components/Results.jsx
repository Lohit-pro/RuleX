import React from 'react';
import { MdCheckCircle } from 'react-icons/md'; // Success icon

function Results({ results, onBack }) {
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
        <ul className="list-disc list-inside space-y-2 text-red-600">
          {results.map((res, idx) => (
            <li key={idx}>{res}</li>
          ))}
        </ul>
      )}

      <div className="mt-8 text-center">
        <button
          onClick={onBack}
          className="px-5 py-2 bg-gray-800 text-white rounded-md hover:bg-white hover:text-black hover:border-black border transition-colors cursor-pointer shadow-lg"
        >
          Upload Another File
        </button>
      </div>
    </div>
  );
}

export default Results;
