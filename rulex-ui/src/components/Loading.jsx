import React from "react";

function Loading(props) {
  return (
    <div className="bg-white flex flex-col items-center justify-center min-h-[60vh]">
      <div className="animate-spin rounded-full h-12 w-12 border-t-4 border-b-4 border-red-600 mb-4"></div>
      <div className="text-lg font-semibold text-gray-800">{props.text}</div>
    </div>
  );
}

export default Loading;
