"use client";

import { CraftNode } from '@/lib/types';
import { ChevronDown, ChevronRight, Hammer, Coins, AlertCircle } from 'lucide-react';
import { useState } from 'react';
import clsx from 'clsx';

export function formatCoins(num: number | null) {
  if (num === null) return 'N/A';
  if (num >= 1_000_000_000) return (num / 1_000_000_000).toFixed(2) + 'B';
  if (num >= 1_000_000) return (num / 1_000_000).toFixed(2) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k';
  return num.toLocaleString();
}

function SourceBadge({ source }: { source: string }) {
  switch(source) {
    case 'bazaar': return <span className="bg-blue-500/20 text-blue-400 border border-blue-500/30 px-2 py-0.5 rounded text-[10px] uppercase font-bold flex flex-row items-center gap-1"><Coins className="w-3 h-3" /> Bazaar</span>;
    case 'crafted': return <span className="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded text-[10px] uppercase font-bold flex flex-row items-center gap-1"><Hammer className="w-3 h-3" /> Crafted</span>;
    case 'manual': return <span className="bg-yellow-500/20 text-yellow-400 border border-yellow-500/30 px-2 py-0.5 rounded text-[10px] uppercase font-bold">Manual</span>;
    default: return <span className="bg-red-500/20 text-red-400 border border-red-500/30 px-2 py-0.5 rounded text-[10px] uppercase font-bold flex flex-row items-center gap-1"><AlertCircle className="w-3 h-3"/> Unknown</span>;
  }
}

export default function RecipeTree({ node, defaultExpanded = false }: { node: CraftNode, defaultExpanded?: boolean }) {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const hasChildren = node.children && node.children.length > 0;

  return (
    <div className="w-full text-sm font-medium">
      <div 
        className={clsx(
          "flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-3 py-2 px-3 rounded hover:bg-zinc-800/50 transition-colors border border-transparent",
          hasChildren && "cursor-pointer group hover:border-zinc-700/50 shadow-sm",
          !hasChildren && "pl-3 sm:pl-8"
        )}
        onClick={() => hasChildren && setExpanded(!expanded)}
      >
        <div className="flex items-center gap-2">
          {hasChildren && (
            <button className="text-zinc-500 group-hover:text-zinc-300 transition-colors">
              {expanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
            </button>
          )}
          
          <span className="text-zinc-400 w-8 inline-block select-none">{node.amount}x</span>
          <span className="text-zinc-200 truncate max-w-[150px] sm:max-w-[250px]">{node.name}</span>
          <SourceBadge source={node.source} />
        </div>
        
        <div className="flex flex-1 items-center sm:justify-end gap-3 pl-8 sm:pl-0">
          {node.isCheaperToBuy && (
            <span className="text-[10px] text-yellow-500 border border-yellow-500/30 px-2 py-0.5 rounded-full hidden lg:block">
              Cheaper to Buy!
            </span>
          )}
          <div className="flex sm:flex-col items-center sm:items-end gap-2 sm:gap-0">
            <span className="text-zinc-100 font-bold">{formatCoins(node.totalCost)}</span>
            {node.amount > 1 && (
              <span className="text-[10px] text-zinc-500">({formatCoins(node.unitCost)} ea)</span>
            )}
          </div>
        </div>
      </div>
      
      {expanded && hasChildren && (
        <div className="pl-4 sm:pl-6 ml-2 sm:ml-2 border-l border-zinc-800 flex flex-col gap-1 mt-1">
          {node.children!.map((child, i) => (
            <RecipeTree key={`${child.itemId}-${i}`} node={child} />
          ))}
        </div>
      )}
    </div>
  );
}
