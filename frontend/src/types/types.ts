export interface WorkItem {
  id: number;
  componentId: string;
  componentType: string;
  status: WorkItemStatus;
  priority: Priority;
  assignedTo?: string;
  notes?: string;
  signalCount: number;
  createdAt: string;
  updatedAt: string;
  closedAt?: string;
}

export enum WorkItemStatus {
  OPEN = 'OPEN',
  INVESTIGATING = 'INVESTIGATING',
  RESOLVED = 'RESOLVED',
  CLOSED = 'CLOSED',
}

export enum Priority {
  P0 = 'P0',
  P1 = 'P1',
  P2 = 'P2',
}

export interface Signal {
  id: string;
  workItemId: number;
  componentId: string;
  componentType: string;
  errorCode: string;
  severity: string;
  message: string;
  timestamp: string;
  metadata?: Record<string, string>;
  debounced: boolean;
}

export interface SignalRequest {
  componentId: string;
  componentType: string;
  errorCode: string;
  severity: string;
  message: string;
  timestamp?: string;
  metadata?: Record<string, string>;
}

export interface RCA {
  id: number;
  workItemId: number;
  incidentStart: string;
  incidentEnd: string;
  rootCauseCategory: string;
  rootCauseDescription: string;
  fixApplied: string;
  preventionSteps: string;
  mttrSeconds: number;
  mttrFormatted: string;
  submittedAt: string;
}

export interface RCARequest {
  incidentStart: string;
  incidentEnd: string;
  rootCauseCategory: string;
  rootCauseDescription: string;
  fixApplied: string;
  preventionSteps: string;
}

export interface DashboardSummary {
  totalActive: number;
  byStatus: {
    OPEN: number;
    INVESTIGATING: number;
    RESOLVED: number;
    CLOSED: number;
  };
  byPriority: {
    P0: number;
    P1: number;
    P2: number;
  };
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  error?: string;
  timestamp: string;
}

export interface StatusUpdateRequest {
  status: string;
  assignedTo?: string;
  notes?: string;
}