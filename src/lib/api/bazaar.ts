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

import https from 'https';

export async function getBazaarData(): Promise<BazaarResponse | null> {
  return new Promise((resolve) => {
    https.get('https://api.hypixel.net/skyblock/bazaar', (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          console.error("Parse error", e);
          resolve(null);
        }
      });
    }).on('error', (e) => {
      console.error("Fetch error", e);
      resolve(null);
    });
  });
}

export async function getBazaarPrice(itemId: string): Promise<number | null> {
  const data = await getBazaarData();
  if (!data?.success || !data.products[itemId]) {
    return null;
  }
  
  return data.products[itemId].quick_status.buyPrice;
}
