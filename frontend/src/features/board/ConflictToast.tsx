import React from 'react';
import { ToastMessage } from '../../types/board';
import { AlertTriangle, CheckCircle2, Info, XCircle, X } from 'lucide-react';

interface ToastContainerProps {
  toasts: ToastMessage[];
  onDismiss: (id: string) => void;
}

export const ToastContainer: React.FC<ToastContainerProps> = ({ toasts, onDismiss }) => {
  if (toasts.length === 0) return null;

  const renderIcon = (type: ToastMessage['type']) => {
    switch (type) {
      case 'success':
        return <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />;
      case 'error':
        return <XCircle className="w-4 h-4 text-rose-500 shrink-0" />;
      case 'warning':
        return <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0" />;
      case 'info':
      default:
        return <Info className="w-4 h-4 text-blue-500 shrink-0" />;
    }
  };

  const getBorderColor = (type: ToastMessage['type']) => {
    switch (type) {
      case 'success':
        return 'border-emerald-500/40 bg-emerald-500/10 text-emerald-950 dark:text-emerald-100';
      case 'error':
        return 'border-rose-500/40 bg-rose-500/10 text-rose-950 dark:text-rose-100';
      case 'warning':
        return 'border-amber-500/40 bg-amber-500/10 text-amber-950 dark:text-amber-100';
      case 'info':
      default:
        return 'border-blue-500/40 bg-blue-500/10 text-blue-950 dark:text-blue-100';
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-2.5 max-w-md w-full pointer-events-none">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`pointer-events-auto p-3.5 rounded-xl border backdrop-blur-md shadow-xl flex items-start gap-3 transition-all duration-200 animate-in slide-in-from-bottom-3 ${getBorderColor(
            toast.type
          )}`}
        >
          {renderIcon(toast.type)}
          <div className="flex-1 min-w-0">
            <h4 className="text-xs font-bold leading-tight">{toast.title}</h4>
            <p className="text-xs opacity-90 mt-1 leading-normal break-words">{toast.message}</p>
          </div>
          <button
            onClick={() => onDismiss(toast.id)}
            className="p-1 rounded-md opacity-60 hover:opacity-100 transition-opacity"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      ))}
    </div>
  );
};
