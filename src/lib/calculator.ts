import { Item, Recipe, CraftNode, PriceSource } from './types';
import { getItemById, getRecipeForOutput } from './api/recipes';
import { BazaarResponse } from './api/bazaar';

export function calculateCraft(
  itemId: string,
  targetAmount: number,
  bazaarData: BazaarResponse | null,
  visited: Set<string> = new Set()
): CraftNode {
  const item = getItemById(itemId);
  
  if (!item) {
    return {
      itemId,
      name: itemId,
      amount: targetAmount,
      totalCost: null,
      unitCost: null,
      source: 'unavailable'
    };
  }
  
  let bazaarUnitCost: number | null = null;
  if (item.isBazaar && bazaarData?.products[itemId]) {
    bazaarUnitCost = bazaarData.products[itemId].quick_status.buyPrice;
  }
  
  if (!bazaarUnitCost && item.manualPrice) {
    bazaarUnitCost = item.manualPrice;
  }

  if (visited.has(itemId)) {
    return {
      itemId,
      name: item.name,
      amount: targetAmount,
      totalCost: bazaarUnitCost ? bazaarUnitCost * targetAmount : null,
      unitCost: bazaarUnitCost,
      source: item.isBazaar ? 'bazaar' : (item.manualPrice ? 'manual' : 'unavailable')
    };
  }

  const recipe = getRecipeForOutput(itemId);
  
  if (!recipe) {
    return {
      itemId,
      name: item.name,
      amount: targetAmount,
      totalCost: bazaarUnitCost ? bazaarUnitCost * targetAmount : null,
      unitCost: bazaarUnitCost,
      source: item.isBazaar ? 'bazaar' : (item.manualPrice ? 'manual' : 'unavailable')
    };
  }

  visited.add(itemId);
  let craftedTotalCost = 0;
  let canCraft = true;
  const children: CraftNode[] = [];

  const craftMultipilier = Math.ceil(targetAmount / recipe.outputAmount);

  for (const ing of recipe.ingredients) {
    const ingNode = calculateCraft(ing.id, ing.amount * craftMultipilier, bazaarData, new Set(visited));
    children.push(ingNode);
    if (ingNode.totalCost === null) {
      canCraft = false;
    } else {
      craftedTotalCost += ingNode.totalCost;
    }
  }

  visited.delete(itemId); 

  const bazzarTotalCost = bazaarUnitCost ? bazaarUnitCost * targetAmount : null;
  
  if (canCraft && !bazzarTotalCost) {
    return {
      itemId,
      name: item.name,
      amount: targetAmount,
      totalCost: craftedTotalCost,
      unitCost: craftedTotalCost / targetAmount,
      source: 'crafted',
      children
    };
  }

  if (!canCraft && bazzarTotalCost) {
    return {
      itemId,
      name: item.name,
      amount: targetAmount,
      totalCost: bazzarTotalCost,
      unitCost: bazaarUnitCost,
      source: item.isBazaar ? 'bazaar' : 'manual'
    };
  }

  if (canCraft && bazzarTotalCost) {
    const isCheaperToBuy = bazzarTotalCost < craftedTotalCost;
    
    if (isCheaperToBuy) {
      return {
        itemId,
        name: item.name,
        amount: targetAmount,
        totalCost: bazzarTotalCost,
        unitCost: bazaarUnitCost,
        source: item.isBazaar ? 'bazaar' : 'manual',
        isCheaperToBuy: true,
        children
      };
    } else {
      return {
        itemId,
        name: item.name,
        amount: targetAmount,
        totalCost: craftedTotalCost,
        unitCost: craftedTotalCost / targetAmount,
        source: 'crafted',
        isCheaperToBuy: false,
        children
      };
    }
  }

  return {
    itemId,
    name: item.name,
    amount: targetAmount,
    totalCost: null,
    unitCost: null,
    source: 'unavailable',
    children
  };
}
