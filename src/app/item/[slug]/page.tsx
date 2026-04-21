import { getItemById } from '@/lib/api/recipes';
import { getBazaarData } from '@/lib/api/bazaar';
import { calculateCraft } from '@/lib/calculator';
import { notFound } from 'next/navigation';
import CraftSummary from '@/components/calculator/CraftSummary';
import RecipeTree from '@/components/calculator/RecipeTree';
import AdSlot from '@/components/ads/AdSlot';
import { Metadata } from 'next';

export async function generateMetadata({ params }: { params: Promise<{slug: string}> }): Promise<Metadata> {
  const p = await params;
  const item = getItemById(p.slug.toUpperCase());
  if (!item) return { title: 'Item Not Found' };
  return {
    title: `${item.name} Craft Cost | SkyBlock Crafter`,
    description: `View the live lowest crafting cost for ${item.name} in Hypixel SkyBlock.`
  };
}

export const revalidate = 60;

export default async function ItemPage({ params }: { params: Promise<{slug: string}> }) {
  const p = await params;
  const slug = p.slug.toUpperCase();
  const item = getItemById(slug);
  
  if (!item) {
    notFound();
  }

  // Pre-fetch bazaar data (server side)
  const bazaarData = await getBazaarData();
  
  // Calculate cheapest path
  const craftNode = calculateCraft(slug, 1, bazaarData);

  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-4xl font-bold text-zinc-100">{item.name}</h1>
        <p className="text-zinc-500">
          Last Updated: {bazaarData ? new Date(bazaarData.lastUpdated).toLocaleString() : 'Unknown'}
        </p>
      </div>

      <CraftSummary node={craftNode} />

      <AdSlot type="in-content" />

      <div className="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden shadow-lg mt-4">
        <div className="bg-zinc-950 px-6 py-4 border-b border-zinc-800 flex justify-between items-center">
          <h3 className="font-bold text-zinc-100 items-center flex gap-2">
            Recipe Breakdown
          </h3>
          <span className="text-[10px] sm:text-xs text-zinc-500 uppercase font-medium tracking-wider">Prices per unit</span>
        </div>
        <div className="p-2 sm:p-6 pb-2 sm:pb-6 overflow-x-auto min-h-[300px]">
          <RecipeTree node={craftNode} defaultExpanded={true} />
        </div>
      </div>
      
      <AdSlot type="sidebar" />
    </div>
  );
}
