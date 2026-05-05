import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { RCARequest } from '../types/types';
import { rcaApi } from '../api/api';

export const RCAForm: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [formData, setFormData] = useState<RCARequest>({
    incidentStart: '',
    incidentEnd: '',
    rootCauseCategory: '',
    rootCauseDescription: '',
    fixApplied: '',
    preventionSteps: '',
  });

  const [mttr, setMttr] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (formData.incidentStart && formData.incidentEnd) {
      const start = new Date(formData.incidentStart).getTime();
      const end = new Date(formData.incidentEnd).getTime();
      const diffSeconds = Math.floor((end - start) / 1000);

      if (diffSeconds < 0) {
        setMttr('❌ End time cannot be before start time');
      } else {
        const hours = Math.floor(diffSeconds / 3600);
        const minutes = Math.floor((diffSeconds % 3600) / 60);
        const secs = diffSeconds % 60;

        let mttrStr = '';
        if (hours > 0) mttrStr += `${hours}h `;
        if (minutes > 0) mttrStr += `${minutes}m `;
        mttrStr += `${secs}s`;

        setMttr(mttrStr);
      }
    }
  }, [formData.incidentStart, formData.incidentEnd]);

  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.incidentStart || !formData.incidentEnd ||
        !formData.rootCauseCategory || !formData.rootCauseDescription ||
        !formData.fixApplied || !formData.preventionSteps) {
      setError('❌ All fields are required!');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      await rcaApi.submitRCA(id!, formData);
      setSuccess(true);

      setTimeout(() => {
        navigate(`/incidents/${id}`);
      }, 2000);
    } catch (err: any) {
      setError(
        err.response?.data?.error ||
          'Failed to submit RCA. Please try again.'
      );
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="max-w-2xl mx-auto p-6 animate-fade-in mt-10">
        <div className="bg-white/80 backdrop-blur-xl border border-emerald-100 p-12 rounded-3xl text-center shadow-[0_8px_30px_rgb(0,0,0,0.04)] relative overflow-hidden">
          <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl -mr-20 -mt-20"></div>
          <div className="w-24 h-24 bg-emerald-100 text-emerald-500 rounded-full flex items-center justify-center mx-auto mb-6 shadow-inner text-4xl">
            ✓
          </div>
          <h2 className="text-3xl font-extrabold mb-3 text-emerald-900 tracking-tight">RCA Submitted Successfully!</h2>
          <p className="text-emerald-700/80 font-medium">Redirecting you back to incident details...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto p-6 animate-fade-in">
      <button
        onClick={() => navigate(`/incidents/${id}`)}
        className="mb-8 group flex items-center text-sm font-semibold text-gray-500 hover:text-indigo-600 transition-colors"
      >
        <span className="transform group-hover:-translate-x-1 transition-transform inline-block mr-2">←</span> Back to Incident
      </button>

      <div className="bg-white/80 backdrop-blur-xl rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 p-8 md:p-12 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/5 rounded-full blur-3xl -mr-20 -mt-20"></div>
        <h1 className="text-3xl font-extrabold mb-8 text-gray-900 tracking-tight flex items-center gap-3 relative z-10">
          <span className="text-3xl">📋</span> Root Cause Analysis
        </h1>

        {error && (
          <div className="mb-8 p-4 bg-red-50 border border-red-200 text-red-700 rounded-xl shadow-sm flex items-center relative z-10">
            <span className="mr-3 text-xl">⚠️</span> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-10 relative z-10">
          {/* Incident Timeline */}
          <fieldset className="bg-gray-50/50 p-6 rounded-2xl border border-gray-100">
            <legend className="text-sm font-bold text-indigo-600 uppercase tracking-wider mb-4 px-2">
              Incident Timeline
            </legend>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-gray-700 font-semibold mb-2 text-sm">
                  Incident Start Time <span className="text-red-500">*</span>
                </label>
                <input
                  type="datetime-local"
                  name="incidentStart"
                  value={formData.incidentStart}
                  onChange={handleChange}
                  required
                  className="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl focus:outline-none focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 transition-all shadow-sm"
                />
              </div>

              <div>
                <label className="block text-gray-700 font-semibold mb-2 text-sm">
                  Incident End Time <span className="text-red-500">*</span>
                </label>
                <input
                  type="datetime-local"
                  name="incidentEnd"
                  value={formData.incidentEnd}
                  onChange={handleChange}
                  required
                  className="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl focus:outline-none focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 transition-all shadow-sm"
                />
              </div>
            </div>

            {mttr && (
              <div className={`mt-6 p-4 rounded-xl shadow-sm border font-medium ${mttr.includes('❌') ? 'bg-red-50 border-red-100 text-red-700' : 'bg-indigo-50 border-indigo-100 text-indigo-700'}`}>
                <strong className="mr-2">Calculated MTTR:</strong> {mttr}
              </div>
            )}
          </fieldset>

          {/* Root Cause */}
          <fieldset className="bg-gray-50/50 p-6 rounded-2xl border border-gray-100">
            <legend className="text-sm font-bold text-indigo-600 uppercase tracking-wider mb-4 px-2">
              Root Cause Details
            </legend>

            <div className="mb-6">
              <label className="block text-gray-700 font-semibold mb-2 text-sm">
                Category <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <select
                  name="rootCauseCategory"
                  value={formData.rootCauseCategory}
                  onChange={handleChange}
                  required
                  className="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl appearance-none focus:outline-none focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 transition-all shadow-sm"
                >
                  <option value="" disabled>-- Select Category --</option>
                  <option value="INFRASTRUCTURE">Infrastructure</option>
                  <option value="CODE_BUG">Code Bug</option>
                  <option value="CONFIGURATION">Configuration</option>
                  <option value="NETWORK">Network</option>
                  <option value="THIRD_PARTY">Third Party Service</option>
                  <option value="HUMAN_ERROR">Human Error</option>
                </select>
                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-gray-500">
                  <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z"/></svg>
                </div>
              </div>
            </div>

            <div>
              <label className="block text-gray-700 font-semibold mb-2 text-sm">
                Description <span className="text-red-500">*</span>
              </label>
              <textarea
                name="rootCauseDescription"
                value={formData.rootCauseDescription}
                onChange={handleChange}
                placeholder="Detailed explanation of what caused the incident..."
                rows={4}
                required
                className="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl focus:outline-none focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 transition-all shadow-sm resize-y"
              />
            </div>
          </fieldset>

          {/* Resolution & Prevention */}
          <fieldset className="bg-gray-50/50 p-6 rounded-2xl border border-gray-100">
            <legend className="text-sm font-bold text-indigo-600 uppercase tracking-wider mb-4 px-2">
              Resolution & Prevention
            </legend>

            <div className="mb-6">
              <label className="block text-gray-700 font-semibold mb-2 text-sm">
                Fix Applied <span className="text-red-500">*</span>
              </label>
              <textarea
                name="fixApplied"
                value={formData.fixApplied}
                onChange={handleChange}
                placeholder="Describe the technical fix or workaround applied..."
                rows={3}
                required
                className="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl focus:outline-none focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 transition-all shadow-sm resize-y"
              />
            </div>

            <div>
              <label className="block text-gray-700 font-semibold mb-2 text-sm">
                Prevention Steps <span className="text-red-500">*</span>
              </label>
              <textarea
                name="preventionSteps"
                value={formData.preventionSteps}
                onChange={handleChange}
                placeholder="Action items to prevent recurrence..."
                rows={3}
                required
                className="w-full px-4 py-3 bg-white border border-gray-200 rounded-xl focus:outline-none focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/10 transition-all shadow-sm resize-y"
              />
            </div>
          </fieldset>

          {/* Form Actions */}
          <div className="flex gap-4 pt-4 border-t border-gray-100 mt-8">
            <button
              type="submit"
              disabled={loading}
              className="flex-1 md:flex-none px-8 py-3 bg-indigo-600 text-white rounded-xl font-bold tracking-wide shadow-md shadow-indigo-200 hover:bg-indigo-700 hover:-translate-y-0.5 transition-all disabled:bg-gray-300 disabled:shadow-none disabled:transform-none disabled:cursor-not-allowed"
            >
              {loading ? 'Submitting...' : 'Submit RCA Report'}
            </button>
            <button
              type="button"
              onClick={() => navigate(`/incidents/${id}`)}
              className="flex-1 md:flex-none px-8 py-3 bg-white text-gray-700 border border-gray-200 rounded-xl font-bold hover:bg-gray-50 transition-colors shadow-sm"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RCAForm;