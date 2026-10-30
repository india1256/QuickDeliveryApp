import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home, ShieldAlert } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallbackTitle?: string;
  fallbackMessage?: string;
  onReset?: () => void;
  inline?: boolean;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
    errorInfo: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error, errorInfo: null };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an unhandled error:', error, errorInfo);
    this.setState({ error, errorInfo });
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
    if (this.props.onReset) {
      this.props.onReset();
    }
  };

  public render() {
    if (this.state.hasError) {
      const { fallbackTitle, fallbackMessage, inline } = this.props;

      if (inline) {
        return (
          <div className="p-4 rounded-2xl bg-amber-50 border border-amber-300 text-amber-950 space-y-2 shadow-xs">
            <div className="flex items-center gap-2 font-extrabold text-sm text-amber-900">
              <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
              <span>{fallbackTitle || 'Component Encountered a Problem'}</span>
            </div>
            <p className="text-xs text-amber-800">
              {fallbackMessage ||
                this.state.error?.message ||
                'Unable to display this view. You can retry safely.'}
            </p>
            <div className="pt-1 flex items-center gap-2">
              <button
                type="button"
                onClick={this.handleReset}
                className="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs flex items-center gap-1.5 transition shadow-2xs cursor-pointer"
              >
                <RefreshCw className="w-3 h-3" />
                <span>Retry</span>
              </button>
            </div>
          </div>
        );
      }

      return (
        <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
          <div className="max-w-md w-full bg-white rounded-3xl p-6 border border-slate-200 shadow-xl space-y-4 text-center">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-red-100 text-red-600 flex items-center justify-center shadow-xs">
              <ShieldAlert className="w-8 h-8" />
            </div>

            <div className="space-y-1">
              <h2 className="text-lg font-black text-slate-900">
                {fallbackTitle || 'Something went wrong'}
              </h2>
              <p className="text-xs text-slate-600 leading-relaxed">
                {fallbackMessage ||
                  'The application encountered an unexpected runtime error. We recovered gracefully so you can continue without losing your data.'}
              </p>
            </div>

            {this.state.error && (
              <div className="p-3 rounded-xl bg-slate-100 border border-slate-200 text-left font-mono text-[11px] text-slate-800 break-all max-h-32 overflow-y-auto">
                {this.state.error.message}
              </div>
            )}

            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                type="button"
                onClick={this.handleReset}
                className="flex-1 py-2.5 px-4 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold text-xs flex items-center justify-center gap-2 transition shadow-sm cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Try Again</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  window.location.reload();
                }}
                className="py-2.5 px-4 rounded-xl border border-slate-300 text-slate-700 font-bold text-xs hover:bg-slate-100 transition cursor-pointer flex items-center gap-1.5"
              >
                <Home className="w-3.5 h-3.5" />
                <span>Reload App</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
