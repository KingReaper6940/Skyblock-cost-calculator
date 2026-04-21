import { CraftNode } from '@/lib/types';

function formatCoinsLong(num: number | null) {
  if (num === null) return 'N/A';
  return num.toLocaleString();
}

export default function CraftSummary({ node }: { node: CraftNode }) {
  return (
    <div className="bg-zinc-900/60 backdrop-blur border border-zinc-800 rounded-xl p-8 shadow-2xl flex flex-col items-center justify-center text-center gap-4 relative overflow-hidden">
      <div className="absolute top-0 w-full h-1 bg-gradient-to-r from-yellow-600 via-yellow-400 to-amber-600" />
      <h2 className="text-zinc-500 text-xs tracking-[0.2em] uppercase font-bold mt-2 flex items-center gap-2">
        Estimated Craft Cost
      </h2>
      <div className="text-4xl sm:text-5xl font-black text-white px-6 py-3 rounded-lg bg-zinc-950 border border-zinc-800/80 inline-block shadow-inner tracking-tight">
        {formatCoinsLong(node.totalCost)} <span className="text-lg sm:text-xl text-yellow-500 font-bold tracking-normal">coins</span>
      </div>
      
      {node.source === 'unavailable' && (
        <div className="text-red-400 text-sm bg-red-950/20 px-4 py-2 rounded border border-red-900/30">
          Unable to calculate complete cost due to missing item sources.
        </div>
      )}
      
      {node.isCheaperToBuy && (
        <div className="text-amber-400 text-sm bg-amber-950/30 px-6 py-2 rounded-full border border-amber-900/50 mt-2 font-medium">
          ⚠️ Note: It is currently cheaper to buy this item directly from the Bazaar!
        </div>
      )}
    </div>
  );
}
