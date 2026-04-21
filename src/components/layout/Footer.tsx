import Link from 'next/link';

export default function Footer() {
  return (
    <footer className="border-t border-zinc-800 bg-black mt-16 py-8">
      <div className="container mx-auto px-4 flex flex-col md:flex-row justify-between items-center gap-4 text-sm text-zinc-500">
        <p>© {new Date().getFullYear()} SkyBlock Crafter. Not affiliated with Hypixel Inc.</p>
        <div className="flex gap-4">
          <Link href="/status" className="hover:text-zinc-300 transition-colors">API Status</Link>
          <Link href="/about" className="hover:text-zinc-300 transition-colors">Methodology</Link>
        </div>
      </div>
    </footer>
  );
}
