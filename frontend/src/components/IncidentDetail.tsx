import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { WorkItem, Signal, RCA } from '../types/types';
import { incidentApi, signalApi, rcaApi } from '../api/api';

export const IncidentDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [incident, setIncident] = useState<WorkItem | null>(null);
  const [signals, setSignals] = useState<Signal[]>([]);
  const [rca, setRca] = useState<RCA | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      fetchIncident();
      fetchSignals();
      fetchRCA();
    }
  }, [id]);

  const fetchIncident = async () => {
    try {
      const response = await incidentApi.getIncidentById(id!);
      if (response.data.success) {
        setIncident(response.data.data);
      }
    } catch (err) {
      setError('Failed to fetch incident');
      console.error(err);
    }
  };

  const fetchSignals = async () => {
    try {
      setLoading(true);
      const response = await signalApi.getSignalsByWorkItem(id!);
      if (response.data.success) {
        setSignals(response.data.data);
      }
    } catch (err) {
      console.error('Failed to fetch signals:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchRCA = async () => {
    try {
      const response = await rcaApi.getRCA(id!);
      if (response.data.success) {
        setRca(response.data.data);
      }
    } catch (err) {
      console.log('No RCA found yet');
    }
  };

  const handleInvestigate = async () => {
    try {
      await incidentApi.investigate(id!, {
        status: 'INVESTIGATING',
        notes: 'Started investigation',
      });
      fetchIncident();
    } catch (err) {
      alert('Failed to move to INVESTIGATING');
    }
  };

  const handleResolve = async () => {
    try {
      await incidentApi.resolve(id!, {
        status: 'RESOLVED',
        notes: 'Issue resolved',
      });
      fetchIncident();
    } catch (err) {
      alert('Failed to move to RESOLVED');
    }
  };

  const handleClose = async () => {
    if (!rca) {
      alert('❌ Cannot close without RCA!');
      return;
    }
    try {
      await incidentApi.close(id!, {
        status: 'CLOSED',
        notes: 'Incident closed',
      });
      fetchIncident();
    } catch (err) {
      alert('Failed to close incident');
    }
  };

  if (loading) {
    return <div className="flex justify-center items-center h-64">Loading incident...</div>;
  }

  if (!incident) {
    return <div className="text-center text-red-600 text-lg">Incident not found</div>;
  }

  return (
    <div className="max-w-5xl mx-auto p-6 animate-fade-in">
      <button
        onClick={() => navigate('/')}
        className="mb-8 group flex items-center text-sm font-semibold text-gray-500 hover:text-indigo-600 transition-colors"
      >
        <span className="transform group-hover:-translate-x-1 transition-transform inline-block mr-2">←</span> Back to Dashboard
      </button>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-xl shadow-sm flex items-center">
          <span className="mr-3 text-xl">⚠️</span> {error}
        </div>
      )}

      {/* Header */}
      <div className="bg-white/80 backdrop-blur-xl p-8 rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 mb-8 flex flex-col md:flex-row md:justify-between md:items-center relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/5 rounded-full blur-3xl -mr-20 -mt-20"></div>
        <div className="relative z-10 mb-4 md:mb-0">
          <div className="flex items-center gap-3 mb-2">
            <h1 className="text-4xl font-extrabold tracking-tight text-gray-900">Incident #{incident.id}</h1>
          </div>
          <p className="text-gray-500 text-lg flex items-center gap-2">
            <span className="inline-block w-2 h-2 rounded-full bg-indigo-400"></span>
            Component: <span className="font-semibold text-gray-700">{incident.componentId}</span>
          </p>
        </div>
        <div className="flex flex-wrap gap-3 relative z-10">
          <span
            className={`px-5 py-2 rounded-full text-sm font-bold tracking-wide shadow-sm border ${
              incident.status === 'OPEN'
                ? 'bg-red-50 border-red-200 text-red-700'
                : incident.status === 'INVESTIGATING'
                ? 'bg-amber-50 border-amber-200 text-amber-700'
                : incident.status === 'RESOLVED'
                ? 'bg-emerald-50 border-emerald-200 text-emerald-700'
                : 'bg-blue-50 border-blue-200 text-blue-700'
            }`}
          >
            <span className="mr-1.5 opacity-70">●</span> {incident.status}
          </span>
          <span
            className={`px-5 py-2 rounded-full text-sm font-bold tracking-wide shadow-sm text-white ${
              incident.priority === 'P0'
                ? 'bg-gradient-to-r from-red-500 to-red-600'
                : incident.priority === 'P1'
                ? 'bg-gradient-to-r from-orange-400 to-orange-500'
                : 'bg-gradient-to-r from-amber-400 to-amber-500'
            }`}
          >
            {incident.priority}
          </span>
        </div>
      </div>

      {/* Details */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-8">
        <div className="bg-white/60 backdrop-blur-sm p-6 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
          <label className="block text-gray-400 text-xs font-bold mb-2 uppercase tracking-wider">Assigned To</label>
          <div className="flex items-center gap-3">
            {incident.assignedTo ? (
              <>
                <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-500 text-white flex items-center justify-center font-bold shadow-sm">
                  {incident.assignedTo.charAt(0).toUpperCase()}
                </div>
                <p className="text-gray-800 font-semibold">{incident.assignedTo}</p>
              </>
            ) : (
              <p className="text-gray-500 italic">Unassigned</p>
            )}
          </div>
        </div>
        <div className="bg-white/60 backdrop-blur-sm p-6 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
          <label className="block text-gray-400 text-xs font-bold mb-2 uppercase tracking-wider">Signals Received</label>
          <p className="text-gray-800 text-3xl font-black">{incident.signalCount}</p>
        </div>
        <div className="bg-white/60 backdrop-blur-sm p-6 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
          <label className="block text-gray-400 text-xs font-bold mb-2 uppercase tracking-wider">Created</label>
          <p className="text-gray-800 font-medium">{new Date(incident.createdAt).toLocaleDateString()}</p>
          <p className="text-gray-500 text-sm">{new Date(incident.createdAt).toLocaleTimeString()}</p>
        </div>
        <div className="bg-white/60 backdrop-blur-sm p-6 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
          <label className="block text-gray-400 text-xs font-bold mb-2 uppercase tracking-wider">Updated</label>
          <p className="text-gray-800 font-medium">{new Date(incident.updatedAt).toLocaleDateString()}</p>
          <p className="text-gray-500 text-sm">{new Date(incident.updatedAt).toLocaleTimeString()}</p>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex flex-wrap gap-4 mb-10">
        {incident.status === 'OPEN' && (
          <button
            onClick={handleInvestigate}
            className="px-8 py-3 bg-indigo-600 text-white rounded-xl font-bold tracking-wide shadow-md shadow-indigo-200 hover:bg-indigo-700 hover:-translate-y-0.5 transition-all"
          >
            Start Investigation
          </button>
        )}
        {incident.status === 'INVESTIGATING' && (
          <button
            onClick={handleResolve}
            className="px-8 py-3 bg-emerald-600 text-white rounded-xl font-bold tracking-wide shadow-md shadow-emerald-200 hover:bg-emerald-700 hover:-translate-y-0.5 transition-all"
          >
            Mark as Resolved
          </button>
        )}
        {incident.status === 'RESOLVED' && (
          <button
            onClick={handleClose}
            disabled={!rca}
            className={`px-8 py-3 rounded-xl font-bold tracking-wide shadow-md transition-all ${
              rca
                ? 'bg-blue-600 text-white shadow-blue-200 hover:bg-blue-700 hover:-translate-y-0.5'
                : 'bg-gray-200 text-gray-400 shadow-none cursor-not-allowed'
            }`}
          >
            Close Incident {!rca && '(Requires RCA)'}
          </button>
        )}
        {incident.status === 'CLOSED' && (
          <div className="px-8 py-3 bg-gray-100/80 text-gray-600 border border-gray-200 rounded-xl font-bold flex items-center gap-2">
            <span className="text-emerald-500">✓</span> Incident Closed
          </div>
        )}
      </div>

      {/* Signals */}
      <div className="bg-white/80 backdrop-blur-xl rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 mb-10 overflow-hidden">
        <div className="px-8 py-6 border-b border-gray-100 bg-white/50">
          <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
            Raw Signals <span className="bg-gray-200 text-gray-700 py-0.5 px-2.5 rounded-full text-xs">{signals.length}</span>
          </h2>
        </div>
        {signals.length === 0 ? (
          <div className="p-12 text-center text-gray-500 font-medium">No signals received</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50/50 border-b border-gray-100">
                <tr>
                  <th className="px-8 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Time</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Error Code</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Severity</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Message</th>
                  <th className="px-8 py-4 text-center text-xs font-bold text-gray-500 uppercase tracking-wider">Debounced</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {signals.map((signal) => (
                  <tr key={signal.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-8 py-4 whitespace-nowrap text-sm text-gray-600">{new Date(signal.timestamp).toLocaleTimeString()}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-indigo-600 bg-indigo-50/30 px-2 rounded">{signal.errorCode}</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-2.5 py-1 rounded text-white text-xs font-bold tracking-wide shadow-sm ${
                          signal.severity === 'P0'
                            ? 'bg-red-500'
                            : signal.severity === 'P1'
                            ? 'bg-orange-500'
                            : 'bg-amber-500'
                        }`}
                      >
                        {signal.severity}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-700 font-medium">{signal.message}</td>
                    <td className="px-8 py-4 whitespace-nowrap text-center text-sm">
                      {signal.debounced ? <span className="text-emerald-500 font-bold">✓</span> : <span className="text-gray-300">-</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* RCA Display */}
      {rca && (
        <div className="bg-white/80 backdrop-blur-xl p-8 rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 mb-10 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-blue-500/5 rounded-full blur-2xl -mr-10 -mt-10"></div>
          <h2 className="text-2xl font-extrabold mb-6 text-gray-800 flex items-center gap-2">
            <span className="text-2xl">📋</span> Root Cause Analysis
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 relative z-10">
            <div className="space-y-6">
              <div>
                <label className="block text-gray-400 text-xs font-bold mb-2 uppercase tracking-wider">Category</label>
                <div className="inline-block px-3 py-1 bg-gray-100 text-gray-700 rounded-lg text-sm font-semibold">
                  {rca.rootCauseCategory}
                </div>
              </div>
              <div>
                <label className="block text-gray-400 text-xs font-bold mb-2 uppercase tracking-wider">Root Cause</label>
                <p className="text-gray-800 leading-relaxed bg-gray-50 p-4 rounded-xl border border-gray-100">{rca.rootCauseDescription}</p>
              </div>
            </div>
            <div className="space-y-6">
              <div>
                <label className="block text-gray-400 text-xs font-bold mb-2 uppercase tracking-wider">Fix Applied</label>
                <p className="text-gray-800 leading-relaxed bg-gray-50 p-4 rounded-xl border border-gray-100">{rca.fixApplied}</p>
              </div>
              <div>
                <label className="block text-gray-400 text-xs font-bold mb-2 uppercase tracking-wider">Prevention Steps</label>
                <p className="text-gray-800 leading-relaxed bg-emerald-50/50 p-4 rounded-xl border border-emerald-100">{rca.preventionSteps}</p>
              </div>
            </div>
            <div className="md:col-span-2 mt-4">
              <div className="bg-gradient-to-r from-blue-50 to-indigo-50 p-6 rounded-2xl border border-blue-100 flex items-center justify-between">
                <div>
                  <label className="block text-blue-800 text-sm font-bold mb-1">Mean Time To Resolution (MTTR)</label>
                  <p className="text-blue-600/80 text-sm">Time taken from incident creation to resolution.</p>
                </div>
                <p className="text-3xl font-black text-blue-700 tracking-tight">{rca.mttrFormatted}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* RCA CTA */}
      {!rca && incident.status !== 'OPEN' && (
        <div className="text-center p-10 bg-gradient-to-br from-indigo-50 to-purple-50 rounded-3xl border border-indigo-100 shadow-sm relative overflow-hidden">
          <div className="relative z-10">
            <h3 className="text-xl font-bold text-indigo-900 mb-2">Ready to close this incident?</h3>
            <p className="text-indigo-700/70 mb-6">A Root Cause Analysis report is required before closing.</p>
            <a
              href={`/incidents/${id}/rca`}
              className="inline-flex items-center px-8 py-3 bg-indigo-600 text-white rounded-xl font-bold tracking-wide shadow-lg shadow-indigo-200 hover:bg-indigo-700 hover:-translate-y-0.5 transition-all"
            >
              Write RCA Report
            </a>
          </div>
        </div>
      )}
    </div>
  );
};

export default IncidentDetail;