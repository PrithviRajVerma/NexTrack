import React, { useState } from 'react';
import { BoardCard as BoardCardType, IssueStatus } from '../../types/board';
import { BoardCard } from './BoardCard';
import { Plus } from 'lucide-react';

interface BoardColumnProps {
  status: IssueStatus;
  title: string;
  dotColor: string;
  cards: BoardCardType[];
  onCardClick: (card: BoardCardType) => void;
  onDragStart: (e: React.DragEvent, card: BoardCardType) => void;
  onDrop: (status: IssueStatus) => void;
  onAddIssueClick: (status: IssueStatus) => void;
}

export const BoardColumn: React.FC<BoardColumnProps> = ({
  status,
  title,
  dotColor,
  cards,
  onCardClick,
  onDragStart,
  onDrop,
  onAddIssueClick,
}) => {
  const [isOver, setIsOver] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (!isOver) setIsOver(true);
  };

  const handleDragLeave = () => {
    setIsOver(false);
  };

  const handleDropInternal = (e: React.DragEvent) => {
    e.preventDefault();
    setIsOver(false);
    onDrop(status);
  };

  return (
    <div
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDropInternal}
      className={`w-72 sm:w-80 shrink-0 bg-slate-100/70 dark:bg-slate-900/40 rounded-2xl border transition-all duration-200 flex flex-col max-h-[calc(100vh-185px)] ${
        isOver
          ? 'border-brand-500 ring-2 ring-brand-500/20 bg-brand-50/10 dark:bg-brand-950/20'
          : 'border-slate-200/80 dark:border-slate-800/80'
      }`}
    >
      {/* Column Header */}
      <div className="p-3.5 border-b border-slate-200/80 dark:border-slate-800/80 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className={`w-2.5 h-2.5 rounded-full ${dotColor}`} />
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-800 dark:text-slate-200">
            {title}
          </h2>
          <span className="text-[11px] font-mono px-2 py-0.5 rounded-full bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400 font-bold">
            {cards.length}
          </span>
        </div>

        <button
          onClick={() => onAddIssueClick(status)}
          title={`Add issue to ${title}`}
          className="p-1 rounded-lg text-slate-400 hover:text-brand-600 dark:hover:text-brand-400 hover:bg-slate-200/60 dark:hover:bg-slate-800 transition-colors"
        >
          <Plus className="w-4 h-4" />
        </button>
      </div>

      {/* Cards Scroll Container */}
      <div className="p-2.5 space-y-2.5 overflow-y-auto flex-1 min-h-[140px]">
        {cards.map((card) => (
          <BoardCard
            key={card.id}
            card={card}
            onClick={onCardClick}
            onDragStart={onDragStart}
          />
        ))}

        {cards.length === 0 && (
          <div className="h-28 flex flex-col items-center justify-center border-2 border-dashed border-slate-200 dark:border-slate-800/80 rounded-xl text-slate-400 dark:text-slate-600 text-xs text-center p-3">
            <span>No issues in this column</span>
            <span className="text-[10px] mt-1 text-slate-400/80">Drag a card here</span>
          </div>
        )}
      </div>
    </div>
  );
};
