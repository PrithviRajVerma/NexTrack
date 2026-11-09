import React from 'react';
import { cn } from '@/lib/utils';
import { Loader2 } from 'lucide-react';

export interface LoaderProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg';
  text?: string;
}

export const Loader: React.FC<LoaderProps> = ({ className, size = 'md', text }) => {
  const sizes = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
  };

  return (
    <div className={cn('flex flex-col items-center justify-center gap-2 text-slate-400', className)}>
      <Loader2 className={cn('animate-spin text-brand-500', sizes[size])} />
      {text && <span className="text-xs font-medium text-slate-500 dark:text-slate-400">{text}</span>}
    </div>
  );
};
