import { searchItems } from '@/lib/api/recipes';
import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;
  const query = searchParams.get('q');
  
  if (!query) {
    return NextResponse.json([]);
  }
  
  const results = searchItems(query);
  return NextResponse.json(results);
}
