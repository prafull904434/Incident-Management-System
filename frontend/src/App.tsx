import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './components/Dashboard';
import IncidentDetail from './components/IncidentDetail';
import RCAForm from './components/RCAForm';

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50 font-sans selection:bg-indigo-100 selection:text-indigo-900">
        <nav className="sticky top-0 z-50 backdrop-blur-xl bg-white/70 border-b border-white/20 text-gray-800 px-6 py-4 shadow-sm transition-all">
          <div className="max-w-7xl mx-auto flex items-center justify-between">
            <a href="/" className="text-2xl font-extrabold tracking-tight flex items-center gap-3 group">
              <span className="text-2xl transform group-hover:scale-110 transition-transform duration-200">⚡</span>
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600">
                IMS Core
              </span>
            </a>
            <div className="text-sm font-medium text-gray-500 bg-white/50 px-4 py-2 rounded-full border border-gray-100 shadow-inner">
              System Health: <span className="text-emerald-500 font-bold ml-1">Optimal</span>
            </div>
          </div>
        </nav>

        <main className="py-10">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/incidents/:id" element={<IncidentDetail />} />
            <Route path="/incidents/:id/rca" element={<RCAForm />} />
            <Route
              path="*"
              element={
                <div className="flex flex-col items-center justify-center py-32 text-center">
                  <span className="text-6xl mb-4">🔍</span>
                  <h2 className="text-2xl font-bold text-gray-800 mb-2">404 - Page Not Found</h2>
                  <p className="text-gray-500">The incident you're looking for doesn't exist.</p>
                </div>
              }
            />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;