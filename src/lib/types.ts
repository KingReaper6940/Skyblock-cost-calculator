export type PriceSource = 'bazaar' | 'crafted' | 'manual' | 'unavailable';

export interface Item {
  id: string;          // e.g. "TERMINATOR"
  name: string;        // e.g. "Terminator"
  rarity?: string;     // e.g. "MYTHIC"
  wikiUrl?: string;
  isBazaar?: boolean;
  manualPrice?: number;
}

export interface Ingredient {
  id: string;          // Item ID
  amount: number;
}

export interface Recipe {
  id: string;          // output item ID
  outputAmount: number;
  ingredients: Ingredient[];
}

export interface CraftNode {
  itemId: string;
  name: string;
  amount: number;
  totalCost: number | null;
  unitCost: number | null;
  source: PriceSource;
  children?: CraftNode[];
  isCheaperToBuy?: boolean;
}
