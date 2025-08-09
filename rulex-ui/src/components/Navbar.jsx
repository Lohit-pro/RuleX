import React from 'react';
import { FaHome, FaUpload } from 'react-icons/fa';
import { Link } from 'react-router-dom';

function Navbar() {
  return (
    <div className="w-full bg-gradient-to-r from-red-600 to-red-500 text-white shadow-md py-5">
      <div className="w-full max-w-screen-xl mx-auto px-6 flex items-center justify-between">
        
        <div className="text-2xl md:text-4xl font-bold tracking-wide">
          <span className="text-white">Société</span> <span className="text-gray-200">Générale</span>
        </div>

        <div className="flex space-x-8 text-base md:text-lg font-medium">
          <Link
            to="/"
            className="flex items-center gap-1 hover:text-gray-200 transition-all duration-300"
          >
            <FaHome />
            Home
          </Link>
          <Link
            to="/upload"
            className="flex items-center gap-1 hover:text-gray-200 transition-all duration-300"
          >
            <FaUpload />
            Upload
          </Link>
        </div>
      </div>
    </div>
  );
}

export default Navbar;
