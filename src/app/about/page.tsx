export default function AboutPage() {
  return (
    <div className="container mx-auto px-4 py-12 max-w-3xl flex-1 flex flex-col gap-8 text-zinc-300">
      <h1 className="text-4xl font-bold text-white">About & Methodology</h1>
      
      <section className="flex flex-col gap-4">
        <h2 className="text-2xl font-bold text-zinc-100">How Prices are Calculated</h2>
        <p className="leading-relaxed">
          SkyBlock Crafter uses a recursive algorithm to determine the absolute cheapest method to obtain an item. For every component in a recipe, we compare the cost of buying the item directly from the <strong>Hypixel Bazaar</strong> (using the current lowest instant-buy price) versus crafting that sub-component from its raw materials.
        </p>
        <p className="leading-relaxed">
          If an item is not available on the Bazaar and is not craftable (e.g., RNG drops like Judgement Core), the engine falls back to an internal manual price estimate updated periodically.
        </p>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-2xl font-bold text-zinc-100">Data Sources</h2>
        <ul className="list-disc pl-5 space-y-2 text-zinc-400">
          <li><strong>Recipes:</strong> Ingested primarily from community wikis. However, due to aggressive scraping protections (HTTP 403), we currently use a static snapshot representing major complex trees (Terminator, Warden Helmet, etc.).</li>
          <li><strong>Prices:</strong> Tracked live via the official <code>api.hypixel.net/skyblock/bazaar</code> endpoint, cached conservatively using standard Next.js revalidate hooks.</li>
        </ul>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-2xl font-bold text-zinc-100">Tech Stack</h2>
        <p className="leading-relaxed text-zinc-400">
          Built using React 19, Next.js 15 App Router, Tailwind CSS v4, and hosted on Vercel. Designed to be fast, reliable, strictly typed, and dynamically cached.
        </p>
      </section>
    </div>
  );
}
