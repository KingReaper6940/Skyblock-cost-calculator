export interface BazaarProduct {
  product_id: string;
  sell_summary: { amount: number; pricePerUnit: number; orders: number }[];
  buy_summary: { amount: number; pricePerUnit: number; orders: number }[];
  quick_status: {
    productId: string;
    sellPrice: number;
    buyPrice: number;
    sellVolume: number;
    buyVolume: number;
  };
}

export interface BazaarResponse {
  success: boolean;
  lastUpdated: number;
  products: Record<string, BazaarProduct>;
}

export async function getBazaarData(): Promise<BazaarResponse | null> {
  try {
    const res = await fetch('https://api.hypixel.net/skyblock/bazaar', {
      cache: 'no-store',
    });
    
    if (!res.ok) {
      console.error('Failed to fetch Bazaar data', res.status);
      return null;
    }
    
    return await res.json();
  } catch (err) {
    console.error('Error fetching Bazaar data', err);
    return null;
  }
}

export async function getBazaarPrice(itemId: string): Promise<number | null> {
  const data = await getBazaarData();
  if (!data?.success || !data.products[itemId]) {
    return null;
  }
  
  return data.products[itemId].quick_status.buyPrice;
}
