import axios, { AxiosInstance } from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export const signalApi = {
  ingestSignal: (signal: any) =>
    axiosInstance.post('/signals/ingest', signal),

  ingestBatch: (signals: any[]) =>
    axiosInstance.post('/signals/ingest/batch', signals),

  getSignalsByWorkItem: (workItemId: string) =>
    axiosInstance.get(`/signals/${workItemId}`),
};

export const incidentApi = {
  getAllIncidents: (page = 0, size = 10) =>
    axiosInstance.get('/incidents', { params: { page, size } }),

  getIncidentById: (id: string) =>
    axiosInstance.get(`/incidents/${id}`),

  getActiveIncidents: () =>
    axiosInstance.get('/incidents/active'),

  getIncidentsByPriority: (priority: string) =>
    axiosInstance.get(`/incidents/severity/${priority}`),

  investigate: (id: string, data: any) =>
    axiosInstance.post(`/incidents/${id}/investigate`, data),

  resolve: (id: string, data: any) =>
    axiosInstance.post(`/incidents/${id}/resolve`, data),

  close: (id: string, data: any) =>
    axiosInstance.post(`/incidents/${id}/close`, data),

  updateStatus: (id: string, data: any) =>
    axiosInstance.patch(`/incidents/${id}/status`, data),

  deleteIncident: (id: string) =>
    axiosInstance.delete(`/incidents/${id}`),
};

export const rcaApi = {
  submitRCA: (workItemId: string, rca: any) =>
    axiosInstance.post(`/incidents/${workItemId}/rca`, rca),

  getRCA: (workItemId: string) =>
    axiosInstance.get(`/incidents/${workItemId}/rca`),

  updateRCA: (workItemId: string, rca: any) =>
    axiosInstance.put(`/incidents/${workItemId}/rca`, rca),

  getMTTR: (workItemId: string) =>
    axiosInstance.get(`/incidents/${workItemId}/mttr`),
};

export const dashboardApi = {
  getSummary: () =>
    axiosInstance.get('/dashboard/summary'),

  getRealtimeState: () =>
    axiosInstance.get('/dashboard/realtime'),

  getMetrics: () =>
    axiosInstance.get('/dashboard/metrics'),

  getHeatmap: () =>
    axiosInstance.get('/dashboard/heatmap'),
};

export default axiosInstance;