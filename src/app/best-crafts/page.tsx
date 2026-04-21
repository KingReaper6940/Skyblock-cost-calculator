import { getAllItems } from '@/lib/api/recipes';
import { getBazaarData } from '@/lib/api/bazaar';
import { calculateCraft } from '@/lib/calculator';
import Link from 'next/link';

export const revalidate = 60; // Revalidate page every 60 seconds

export default async function BestCraftsPage() {
  const bazaarData = await getBazaarData();
  const items = getAllItems().filter(i => i.isBazaar); 
  
  const opportunities = items.map(item => {
    const node = calculateCraft(item.id, 1, bazaarData);
    if (!node.totalCost || !node.unitCost || node.source === 'unavailable' || node.isCheaperToBuy) {
      return null;
    }
    
    // Total cost here is the crafted cost.
    // If we can also buy it directly, let's see which is cheaper.
    const buyPrice = bazaarData?.products[item.id]?.quick_status.buyPrice || 0;
    if (buyPrice > 0 && node.totalCost < buyPrice) {
       return { item, node, savings: buyPrice - node.totalCost, buyPrice };
    }
    return null;
  }).filter(Boolean) as { item: any, node: any, savings: number, buyPrice: number }[];

  opportunities.sort((a,b) => b.savings - a.savings); // Highest savings first

  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl flex-1">
       <div className="mb-8">
         <h1 className="text-3xl font-bold text-zinc-100 mb-2">Best Crafts Dashboard</h1>
         <p className="text-zinc-400">Discover items where manual crafting is currently significantly cheaper than buying direct.</p>
       </div>
       
       <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {opportunities.length === 0 ? (
            <div className="col-span-full py-12 text-center text-zinc-500 bg-zinc-900 border border-zinc-800 rounded-xl shadow-inner">
              No profitable crafts found at current market prices. Check back later!
            </div>
          ) : (
             opportunities.map(opp => (
                <Link 
                  key={opp.item.id} 
                  href={`/item/${opp.item.id}`}
                  className="bg-zinc-900 border border-emerald-900/50 p-5 rounded-xl hover:border-emerald-700/50 transition-colors group flex flex-col gap-4 relative overflow-hidden"
                >
                  <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/5 rounded-bl-full" />
                  <span className="font-bold text-white text-lg z-10">{opp.item.name}</span>
                  <div className="flex flex-col gap-2 text-sm z-10">
                    <div className="flex justify-between items-center py-1 border-b border-zinc-800/50"><span className="text-zinc-500">Buy Direct:</span><span className="text-zinc-300">{opp.buyPrice.toLocaleString()}</span></div>
                    <div className="flex justify-between items-center py-1 border-b border-zinc-800/50"><span className="text-zinc-500">Crafting Cost:</span><span className="text-emerald-400 font-medium">{opp.node.totalCost.toLocaleString()}</span></div>
                    <div className="flex justify-between items-center font-bold pt-2 mt-1"><span className="text-zinc-400">Profit/Savings:</span><span className="text-emerald-500 bg-emerald-500/10 px-2 py-0.5 rounded">+{opp.savings.toLocaleString()}</span></div>
                  </div>
                </Link>
             ))
          )}
       </div>
    </div>
  );
}
