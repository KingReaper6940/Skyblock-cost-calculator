"use client";

import Link from 'next/link';
import { Search, Pickaxe } from 'lucide-react';
import { useState, useEffect, useRef } from 'react';
import { Item } from '@/lib/types';

export default function Header() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Item[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (query.length > 1) {
      // Fetch from a simple API route we'll create later, or we could just filter a pre-fetched list.
      fetch(`/api/search?q=${encodeURIComponent(query)}`)
        .then(res => res.json())
        .then(data => {
          setResults(data);
          setIsOpen(true);
        })
        .catch(console.error);
    } else {
      setResults([]);
      setIsOpen(false);
    }
  }, [query]);

  // Close when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <header className="sticky top-0 z-50 w-full border-b border-[var(--border)] bg-[var(--background)]/80 backdrop-blur">
      <div className="container mx-auto px-4 h-16 flex items-center justify-between gap-4">
        <Link href="/" className="flex items-center gap-2 text-xl font-bold tracking-tight text-[var(--primary)] shrink-0">
          <Pickaxe className="w-6 h-6" />
          <span className="hidden sm:inline-block">SkyBlock</span> Crafter
        </Link>
        
        <div className="relative flex-1 max-w-md" ref={searchRef}>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
            <input 
              type="text" 
              placeholder="Search items to craft..." 
              className="w-full bg-zinc-900 border border-zinc-800 rounded-full py-2 pl-10 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-600 transition-all font-medium text-zinc-200 placeholder:text-zinc-500"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onFocus={() => { if (results.length > 0) setIsOpen(true); }}
            />
          </div>
          
          {isOpen && results.length > 0 && (
            <div className="absolute top-12 left-0 w-full bg-zinc-900 border border-zinc-800 rounded-lg shadow-xl overflow-hidden py-2">
              {results.slice(0, 5).map(item => (
                <Link 
                  key={item.id} 
                  href={`/item/${item.id}`}
                  className="block px-4 py-2 hover:bg-zinc-800 transition-colors text-sm text-zinc-300"
                  onClick={() => setIsOpen(false)}
                >
                  {item.name}
                </Link>
              ))}
            </div>
          )}
        </div>

        <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-zinc-400">
          <Link href="/crafts" className="hover:text-zinc-100 transition-colors">Explorer</Link>
          <Link href="/best-crafts" className="hover:text-zinc-100 transition-colors">Best Crafts</Link>
          <Link href="/about" className="hover:text-zinc-100 transition-colors">About</Link>
        </nav>
      </div>
    </header>
  );
}
