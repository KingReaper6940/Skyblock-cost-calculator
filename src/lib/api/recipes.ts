import { Item, Recipe } from '../types';
import recipesData from '../data/recipes.json';

const data = recipesData as { items: Item[]; recipes: Recipe[] };

export function getAllItems(): Item[] {
  return data.items;
}

export function getItemById(id: string): Item | undefined {
  return data.items.find(item => item.id === id);
}

export function searchItems(query: string): Item[] {
  const lowerQuery = query.toLowerCase();
  return data.items.filter(item => item.name.toLowerCase().includes(lowerQuery));
}

export function getRecipeForOutput(itemId: string): Recipe | undefined {
  return data.recipes.find(r => r.id === itemId);
}
