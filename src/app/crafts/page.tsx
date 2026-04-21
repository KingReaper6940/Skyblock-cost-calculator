import { getAllItems } from '@/lib/api/recipes';
import Link from 'next/link';

export default function CraftsExplorer() {
  const items = getAllItems().sort((a,b) => a.name.localeCompare(b.name));

  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl">
       <h1 className="text-3xl font-bold text-zinc-100 mb-6 flex items-center gap-3">
         All Items <span className="text-sm font-medium text-zinc-500 bg-zinc-900 px-3 py-1 rounded-full border border-zinc-800">{items.length} supported</span>
       </h1>
       <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map(item => (
            <Link 
              key={item.id} 
              href={`/item/${item.id}`}
              className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl hover:border-zinc-700 transition-colors group flex justify-between items-center"
            >
              <span className="font-medium text-zinc-300 group-hover:text-yellow-500 transition-colors truncate">{item.name}</span>
              {item.rarity && (
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-1 rounded bg-zinc-800 text-zinc-400 shrink-0 ml-2">
                  {item.rarity}
                </span>
              )}
            </Link>
          ))}
       </div>
    </div>
  );
}
