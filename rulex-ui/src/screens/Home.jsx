import React from "react";
import Dashboard from "../components/Dashboard"
import Footer from "../components/Footer";

function Home() {
  return (
    <div>
      <div className="flex flex-col">
        <Dashboard />
        <Footer />
      </div>
    </div>
  );
}

export default Home;
