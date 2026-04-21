import AdSlot from '@/components/ads/AdSlot';
import { Pickaxe, TrendingUp } from 'lucide-react';
import Link from 'next/link';

export default function Home() {
  return (
    <div className="container mx-auto px-4 py-8 flex flex-col gap-12">
      <section className="text-center py-16 flex flex-col items-center gap-6">
        <div className="bg-yellow-500/10 text-yellow-500 border border-yellow-500/20 px-4 py-1 rounded-full text-sm font-medium">
          Live Bazaar Data Integration
        </div>
        <h1 className="text-5xl sm:text-7xl font-black tracking-tight text-white max-w-4xl">
          Craft <span className="text-transparent bg-clip-text bg-gradient-to-r from-yellow-400 to-yellow-600">Smarter.</span> Not Harder.
        </h1>
        <p className="text-zinc-400 text-lg sm:text-xl max-w-2xl">
          Calculate the exact cost of any Hypixel SkyBlock item. We recursively analyze every sub-recipe against live Bazaar prices to find the absolute cheapest path.
        </p>
        
        <div className="flex gap-4 mt-4">
          <Link href="/crafts" className="bg-yellow-600 hover:bg-yellow-500 text-yellow-950 font-bold px-8 py-3 rounded-full transition-all">
            Explore All Recipes
          </Link>
          <Link href="/item/TERMINATOR" className="bg-zinc-800 hover:bg-zinc-700 text-white font-bold px-8 py-3 rounded-full transition-all flex items-center gap-2">
            <TrendingUp className="w-5 h-5" /> View Terminator
          </Link>
        </div>
      </section>

      <AdSlot type="leaderboard" />

      <section className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto w-full">
        <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-xl flex flex-col gap-3">
          <Pickaxe className="w-8 h-8 text-yellow-500" />
          <h3 className="text-xl font-bold text-zinc-100">Recursive Engine</h3>
          <p className="text-zinc-400 text-sm">We don't just calculate the top level. We check every single sub-component to see if it's cheaper to buy or craft.</p>
        </div>
         <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-xl flex flex-col gap-3">
          <TrendingUp className="w-8 h-8 text-yellow-500" />
          <h3 className="text-xl font-bold text-zinc-100">Live Prices</h3>
          <p className="text-zinc-400 text-sm">Prices are pulled directly from the official Hypixel Bazaar API and refreshed frequently to ensure accuracy.</p>
        </div>
         <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-xl flex flex-col gap-3">
          <div className="w-8 h-8 rounded bg-yellow-500 text-zinc-900 font-bold flex items-center justify-center">100+</div>
          <h3 className="text-xl font-bold text-zinc-100">Growing Database</h3>
          <p className="text-zinc-400 text-sm">We periodically ingest recipes from the community wiki to expand our knowledge base to cover all craftable items.</p>
        </div>
      </section>
    </div>
  );
}
