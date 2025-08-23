import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './screens/Home';
import Upload from './screens/Upload';

function App() {
  return (
    <Router>
      <div className="min-h-screen flex flex-col">
        <Navbar />

        {/* Page content */}
        <div className="flex-grow">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/upload" element={<Upload />} />
          </Routes>
        </div>

      </div>
    </Router>
  );
}

export default App;
