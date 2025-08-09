import React from "react";
import { Link } from "react-router-dom";
import { RiFileExcel2Line } from "react-icons/ri";

function UploadButton(props) {
  return (
    <Link
      to={props.link}
      className="mt-2 px-6 py-3 rounded-md bg-green-800 border border-green-800 text-white hover:bg-white hover:text-black hover:border-black text-lg transition duration-300 flex items-center gap-2 shadow-lg cursor-pointer"
    >
      <span>{props.text}</span>
      <RiFileExcel2Line />
    </Link>
  );
}

export default UploadButton;
