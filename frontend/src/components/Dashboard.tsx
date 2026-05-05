import React, { useState, useEffect } from 'react';
import { WorkItem, DashboardSummary } from '../types/types';
import { incidentApi, dashboardApi } from '../api/api';

export const Dashboard: React.FC = () => {
  const [incidents, setIncidents] = useState<WorkItem[]>([]);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchIncidents();
    fetchSummary();
    const interval = setInterval(() => {
      fetchIncidents();
      fetchSummary();
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchIncidents = async () => {
    try {
      setLoading(true);
      const response = await incidentApi.getActiveIncidents();
      if (response.data.success) {
        const sorted = response.data.data.sort(
          (a: WorkItem, b: WorkItem) => {
            const priorityMap = { P0: 0, P1: 1, P2: 2 };
            return priorityMap[a.priority] - priorityMap[b.priority];
          }
        );
        setIncidents(sorted);
      }
    } catch (err) {
      setError('Failed to fetch incidents');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchSummary = async () => {
    try {
      const response = await dashboardApi.getSummary();
      if (response.data.success) {
        setSummary(response.data.data);
      }
    } catch (err) {
      console.error('Failed to fetch summary:', err);
    }
  };

  const getPriorityColor = (priority: string): string => {
    switch (priority) {
      case 'P0':
        return 'bg-red-500';
      case 'P1':
        return 'bg-orange-500';
      case 'P2':
        return 'bg-yellow-500';
      default:
        return 'bg-gray-500';
    }
  };

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'OPEN':
        return 'bg-red-100 text-red-800';
      case 'INVESTIGATING':
        return 'bg-yellow-100 text-yellow-800';
      case 'RESOLVED':
        return 'bg-green-100 text-green-800';
      case 'CLOSED':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading && !incidents.length) {
    return (
      <div className="flex justify-center items-center h-64 text-gray-600">
        Loading incidents...
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto animate-fade-in">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-4xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-gray-800 to-gray-600 tracking-tight">
          Dashboard Overview
        </h1>
        <div className="bg-white/60 backdrop-blur-sm px-4 py-2 rounded-lg border border-gray-200 shadow-sm text-sm font-medium text-gray-500">
          Auto-refreshing every 5s
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-100 border-l-4 border-red-500 text-red-700">
          {error}
        </div>
      )}

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-6 mb-10">
          <div className="bg-white/80 backdrop-blur-xl p-6 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 hover:-translate-y-1 hover:shadow-lg transition-all duration-300">
            <h3 className="text-gray-500 text-xs font-bold uppercase tracking-wider mb-2">Active</h3>
            <p className="text-4xl font-black text-gray-800">{summary.totalActive}</p>
          </div>
          <div className="relative overflow-hidden bg-gradient-to-br from-red-50 to-white p-6 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-red-100 hover:-translate-y-1 hover:shadow-red-200/50 transition-all duration-300 group">
            <div className="absolute top-0 right-0 w-24 h-24 bg-red-500/10 rounded-full blur-xl -mr-8 -mt-8 group-hover:bg-red-500/20 transition-all"></div>
            <h3 className="text-red-800/60 text-xs font-bold uppercase tracking-wider mb-2">P0 Critical</h3>
            <p className="text-4xl font-black text-red-600">{summary.byPriority.P0}</p>
          </div>
          <div className="relative overflow-hidden bg-gradient-to-br from-orange-50 to-white p-6 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-orange-100 hover:-translate-y-1 hover:shadow-orange-200/50 transition-all duration-300 group">
            <div className="absolute top-0 right-0 w-24 h-24 bg-orange-500/10 rounded-full blur-xl -mr-8 -mt-8 group-hover:bg-orange-500/20 transition-all"></div>
            <h3 className="text-orange-800/60 text-xs font-bold uppercase tracking-wider mb-2">P1 High</h3>
            <p className="text-4xl font-black text-orange-500">{summary.byPriority.P1}</p>
          </div>
          <div className="relative overflow-hidden bg-gradient-to-br from-amber-50 to-white p-6 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-amber-100 hover:-translate-y-1 hover:shadow-amber-200/50 transition-all duration-300 group">
            <div className="absolute top-0 right-0 w-24 h-24 bg-amber-500/10 rounded-full blur-xl -mr-8 -mt-8 group-hover:bg-amber-500/20 transition-all"></div>
            <h3 className="text-amber-800/60 text-xs font-bold uppercase tracking-wider mb-2">P2 Medium</h3>
            <p className="text-4xl font-black text-amber-500">{summary.byPriority.P2}</p>
          </div>
          <div className="relative overflow-hidden bg-gradient-to-br from-indigo-50 to-white p-6 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-indigo-100 hover:-translate-y-1 hover:shadow-indigo-200/50 transition-all duration-300 group">
            <div className="absolute top-0 right-0 w-24 h-24 bg-indigo-500/10 rounded-full blur-xl -mr-8 -mt-8 group-hover:bg-indigo-500/20 transition-all"></div>
            <h3 className="text-indigo-800/60 text-xs font-bold uppercase tracking-wider mb-2">Resolved</h3>
            <p className="text-4xl font-black text-indigo-600">{summary.byStatus.CLOSED}</p>
          </div>
        </div>
      )}

      {/* Incidents Table */}
      <div className="bg-white/80 backdrop-blur-xl rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 overflow-hidden">
        <div className="px-8 py-6 border-b border-gray-100 bg-white/50">
          <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse"></div>
            Active Incidents
          </h2>
        </div>

        {incidents.length === 0 ? (
          <div className="p-16 text-center bg-emerald-50/50">
            <div className="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">🎉</span>
            </div>
            <p className="text-xl font-bold text-emerald-800">System is healthy!</p>
            <p className="text-emerald-600 mt-1">No active incidents require your attention.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50/50 border-b border-gray-100">
                <tr>
                  <th className="px-8 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">ID</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Component</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Priority</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Signals</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Assigned</th>
                  <th className="px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Created</th>
                  <th className="px-8 py-4 text-right text-xs font-bold text-gray-500 uppercase tracking-wider">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {incidents.map((incident) => (
                  <tr
                    key={incident.id}
                    className="hover:bg-gray-50/80 transition-colors group"
                  >
                    <td className="px-8 py-5 whitespace-nowrap">
                      <span className="text-sm font-mono font-medium text-gray-500 group-hover:text-indigo-600 transition-colors">#{incident.id}</span>
                    </td>
                    <td className="px-6 py-5 whitespace-nowrap">
                      <span className={`px-3 py-1 rounded-full text-xs font-bold tracking-wide border ${getStatusColor(incident.status)}`}>
                        {incident.status}
                      </span>
                    </td>
                    <td className="px-6 py-5 whitespace-nowrap">
                      <span className="text-sm font-medium text-gray-700">{incident.componentId}</span>
                    </td>
                    <td className="px-6 py-5 whitespace-nowrap">
                      <span className={`px-2.5 py-1 rounded shadow-sm text-white text-xs font-bold tracking-wide ${getPriorityColor(incident.priority)}`}>
                        {incident.priority}
                      </span>
                    </td>
                    <td className="px-6 py-5 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-gray-400"></span>
                        <span className="text-sm font-semibold text-gray-700">{incident.signalCount}</span>
                      </div>
                    </td>
                    <td className="px-6 py-5 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        {incident.assignedTo ? (
                          <>
                            <div className="w-6 h-6 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-500 text-white flex items-center justify-center text-[10px] font-bold">
                              {incident.assignedTo.charAt(0).toUpperCase()}
                            </div>
                            <span className="text-sm font-medium text-gray-700">{incident.assignedTo}</span>
                          </>
                        ) : (
                          <span className="text-sm text-gray-400 italic">Unassigned</span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-5 whitespace-nowrap">
                      <span className="text-sm text-gray-500">
                        {new Date(incident.createdAt).toLocaleDateString()} <span className="text-gray-400">{new Date(incident.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                      </span>
                    </td>
                    <td className="px-8 py-5 whitespace-nowrap text-right">
                      <a
                        href={`/incidents/${incident.id}`}
                        className="inline-flex items-center justify-center px-4 py-2 bg-white border border-gray-200 rounded-lg text-sm font-semibold text-indigo-600 hover:bg-indigo-50 hover:border-indigo-200 hover:text-indigo-700 transition-all shadow-sm group-hover:shadow"
                      >
                        Details
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;