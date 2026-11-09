import React from 'react';
import { BoardCard as BoardCardType, IssuePriority, IssueType } from '../types/board.types';
import { formatDate } from '@/utils/formatDate';
import { 
  AlertCircle, 
  ArrowUp, 
  ArrowDown, 
  Minus, 
  MessageSquare, 
  GitPullRequest, 
  Bookmark, 
  CheckSquare, 
  Bug, 
  Layers,
  Calendar
} from 'lucide-react';

interface BoardCardProps {
  card: BoardCardType;
  onClick: (card: BoardCardType) => void;
  onDragStart: (e: React.DragEvent, card: BoardCardType) => void;
}

export const BoardCard: React.FC<BoardCardProps> = ({ card, onClick, onDragStart }) => {
  const renderPriorityIcon = (priority: IssuePriority) => {
    switch (priority) {
      case 'URGENT':
        return (
          <span className="text-rose-500 font-bold flex items-center" title="Urgent Priority">
            <AlertCircle className="w-3.5 h-3.5" />
          </span>
        );
      case 'HIGH':
        return (
          <span className="text-orange-500 font-bold flex items-center" title="High Priority">
            <ArrowUp className="w-3.5 h-3.5" />
          </span>
        );
      case 'MEDIUM':
        return (
          <span className="text-amber-500 flex items-center" title="Medium Priority">
            <Minus className="w-3.5 h-3.5" />
          </span>
        );
      case 'LOW':
        return (
          <span className="text-slate-400 flex items-center" title="Low Priority">
            <ArrowDown className="w-3.5 h-3.5" />
          </span>
        );
    }
  };

  const renderTypeIcon = (type: IssueType) => {
    switch (type) {
      case 'BUG':
        return <Bug className="w-3.5 h-3.5 text-rose-500" />;
      case 'STORY':
        return <Bookmark className="w-3.5 h-3.5 text-emerald-500" />;
      case 'EPIC':
        return <Layers className="w-3.5 h-3.5 text-purple-500" />;
      case 'TASK':
      default:
        return <CheckSquare className="w-3.5 h-3.5 text-blue-500" />;
    }
  };

  const getInitials = (name?: string | null) => {
    if (!name) return '?';
    return name
      .split(' ')
      .map(part => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <div
      draggable
      onDragStart={(e) => onDragStart(e, card)}
      onClick={() => onClick(card)}
      className="bg-white dark:bg-slate-900 p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md hover:border-brand-500/50 dark:hover:border-brand-500/50 cursor-grab active:cursor-grabbing transition-all duration-150 group select-none relative"
    >
      {/* Top Header: Type, Key, Priority */}
      <div className="flex items-center justify-between text-xs mb-2">
        <div className="flex items-center gap-1.5">
          {renderTypeIcon(card.issueType)}
          <span className="font-mono font-semibold text-slate-500 dark:text-slate-400 group-hover:text-brand-600 dark:group-hover:text-brand-400 transition-colors">
            {card.formattedKey}
          </span>
          {card.storyPoints !== null && card.storyPoints !== undefined && (
            <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-medium">
              {card.storyPoints}pt
            </span>
          )}
        </div>
        <div className="flex items-center gap-1">
          {renderPriorityIcon(card.priority)}
        </div>
      </div>

      {/* Title */}
      <h3 className="text-xs font-semibold text-slate-800 dark:text-slate-200 leading-snug mb-3 group-hover:text-brand-600 dark:group-hover:text-white transition-colors line-clamp-2">
        {card.title}
      </h3>

      {/* Footer Info: Comments, PRs, Due Date, Assignee Avatar */}
      <div className="flex items-center justify-between pt-2 border-t border-slate-100 dark:border-slate-800/80 text-[11px] text-slate-500 dark:text-slate-400">
        <div className="flex items-center gap-2.5">
          {card.commentCount > 0 && (
            <span className="flex items-center gap-1 hover:text-slate-700 dark:hover:text-slate-300">
              <MessageSquare className="w-3 h-3" />
              <span>{card.commentCount}</span>
            </span>
          )}
          {card.prCount > 0 && (
            <span className="flex items-center gap-1 text-purple-600 dark:text-purple-400">
              <GitPullRequest className="w-3 h-3" />
              <span>{card.prCount}</span>
            </span>
          )}
          {card.dueDate && (
            <span className="flex items-center gap-1 text-[10px]">
              <Calendar className="w-3 h-3" />
              <span>{formatDate(card.dueDate)}</span>
            </span>
          )}
        </div>

        {/* Assignee Avatar */}
        <div 
          className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold text-white shadow-sm ${
            card.assigneeName ? 'bg-gradient-to-tr from-brand-600 to-indigo-500' : 'bg-slate-300 dark:bg-slate-700 text-slate-600 dark:text-slate-400 border border-dashed border-slate-400 dark:border-slate-600'
          }`}
          title={card.assigneeName || 'Unassigned'}
        >
          {getInitials(card.assigneeName)}
        </div>
      </div>
    </div>
  );
};
