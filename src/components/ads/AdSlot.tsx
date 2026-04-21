export default function AdSlot({ type }: { type: 'leaderboard' | 'sidebar' | 'in-content' | 'footer' }) {
  const adsEnabled = process.env.NEXT_PUBLIC_ADS_ENABLED === 'true';
  
  if (!adsEnabled) return null;

  const dimensions = {
    leaderboard: 'w-full max-w-[728px] h-[90px]',
    sidebar: 'w-[300px] h-[250px]',
    'in-content': 'w-full max-w-[300px] sm:max-w-[728px] h-[250px] sm:h-[90px]',
    footer: 'w-full h-[50px] sm:h-[90px]',
  };

  return (
    <div className={`mx-auto bg-zinc-900/50 border border-zinc-800/50 flex flex-col items-center justify-center text-zinc-600 text-[10px] tracking-widest uppercase rounded-lg shadow-inner overflow-hidden relative group my-8 ${dimensions[type]}`}>
      <span className="absolute top-2 left-2 opacity-50">Ad</span>
      <div className="w-full h-full bg-gradient-to-br from-zinc-800/20 to-zinc-900/20" />
    </div>
  );
}
