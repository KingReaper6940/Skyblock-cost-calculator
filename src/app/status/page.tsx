import { getBazaarData } from '@/lib/api/bazaar';

export const revalidate = 60;

export default async function StatusPage() {
  const bazaarData = await getBazaarData();

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl flex-1">
       <h1 className="text-3xl font-bold text-zinc-100 mb-6">System Status</h1>
       
       <div className="grid grid-cols-1 gap-4">
         <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-xl">
           <h3 className="font-bold text-zinc-300 text-lg">Hypixel Bazaar API</h3>
           <div className="mt-2 flex items-center justify-between py-2 border-b border-zinc-800/50">
             <span className="text-zinc-500 text-sm">Status</span>
             {bazaarData ? (
               <span className="text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider">Operational</span>
             ) : (
               <span className="text-red-400 bg-red-500/10 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider">Unreachable</span>
             )}
           </div>
           {bazaarData && (
             <div className="mt-2 flex items-center justify-between py-2 border-b border-zinc-800/50">
               <span className="text-zinc-500 text-sm">Last Synced</span>
               <span className="text-zinc-300 text-sm font-medium">{new Date(bazaarData.lastUpdated).toLocaleString()}</span>
             </div>
           )}
           <div className="mt-2 flex items-center justify-between py-2">
             <span className="text-zinc-500 text-sm">Cache Policy</span>
             <span className="text-zinc-400 text-sm">60s Stale-While-Revalidate</span>
           </div>
         </div>

         <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-xl">
           <h3 className="font-bold text-zinc-300 text-lg">Recipe Database</h3>
           <div className="mt-2 flex items-center justify-between py-2 border-b border-zinc-800/50">
             <span className="text-zinc-500 text-sm">Active Source</span>
             <span className="text-zinc-300 text-sm font-medium">Local Fallback Snapshot</span>
           </div>
           <div className="mt-2 flex items-center justify-between py-2 border-b border-zinc-800/50">
             <span className="text-zinc-500 text-sm">Scraper Pipeline</span>
             <span className="text-amber-400 bg-amber-500/10 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider">Cloudflare 403 Blocked</span>
           </div>
           <div className="mt-2 flex items-center justify-between py-2">
             <span className="text-zinc-500 text-sm">Items Tracked</span>
             <span className="text-zinc-300 text-sm font-medium">18 Core Trees</span>
           </div>
         </div>
       </div>
    </div>
  );
}
