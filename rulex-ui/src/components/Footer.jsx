import React from "react";
import { FaGithub, FaLinkedin, FaEnvelope } from "react-icons/fa";

function Footer() {
  return (
    <footer className="bg-black text-white py-10">
      <div className="max-w-screen-xl mx-auto px-4 text-center flex flex-col items-center gap-8">
        <h1 className="text-3xl font-bold tracking-wide text-white">
          Rule<span className="text-red-500">X</span> Engine
        </h1>

        <div className="flex flex-wrap justify-center gap-6 text-lg">
          <a
            href="https://teams.microsoft.com/l/user/19:<user-id>@thread.tacv2"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:underline text-blue-300"
          >
            Lohit M K
          </a>

          <a
            href="https://teams.microsoft.com/l/user/19:<user-id>@thread.tacv2"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:underline text-blue-300"
          >
            Siddesha N M
          </a>

          <a
            href="https://teams.microsoft.com/l/user/19:<user-id>@thread.tacv2"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:underline text-blue-300"
          >
            Sheetal
          </a>
        </div>

        <div className="flex gap-6 text-xl">
          <a href="https://github.com/Lohit-pro/RuleX" target="_blank" className="text-gray-500 text-sm flex items-center gap-2 hover:text-red-500 transition">
          Contribute to RuleX Engine
            <FaGithub />
          </a>
        </div>

        <div className="text-sm text-gray-400 mt-4">
          &copy; {new Date().getFullYear()} Team RuleX. All rights reserved.
        </div>
      </div>
    </footer>
  );
}

export default Footer;
