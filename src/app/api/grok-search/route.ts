import { NextRequest, NextResponse } from 'next/server'

interface RequestBody {
  pair: 'EURUSD' | 'BTCUSD'
}

interface FactorScores {
  news: number
  yestTrend: number
  weekTrend: number
  todayTrend: number
  todaySent: number
  yestSent: number
  weekSent: number
  todayDev: number
  yestDev: number
}

interface ApiResponse {
  updatedScores: Partial<FactorScores> // Only updates todaySent & todayDev
  posts: string[]
}

export async function POST(request: NextRequest) {
  try {
    const body: RequestBody = await request.json()
    const { pair } = body

    // Dummy Grok/X search simulation
    // Real: await fetch('https://api.x.ai/v1/chat/completions', { ... }) or proxy to x_semantic_search
    const isBullish = Math.random() > 0.4 // 60% bullish bias for demo
    const sentimentDelta = isBullish ? (Math.random() * 3 + 1) : -(Math.random() * 3 + 1)
    const devDelta = isBullish ? (Math.random() * 4 + 1) : -(Math.random() * 4 + 1)

    // Mock posts (tailored to pair; in real, parse from X tool response)
    const posts: string[] = [
      isBullish 
        ? `${pair} sentiment turning bullish—strong bids incoming! #Forex #Trading`
        : `${pair} bearish vibes after EU data dump. Watch for downside. #EURUSD`,
      `${pair} hourly: ${isBullish ? 'uptrend intact' : 'reversing lower'} on volume spike.`,
      `Dev note: ${pair} reacting to ${isBullish ? 'dovish ECB hints' : 'hawkish Fed chatter'}. Eyes on 1.08 level.`
    ]

    const updatedScores: Partial<FactorScores> = {
      todaySent: sentimentDelta, // Delta to apply on client
      todayDev: devDelta,
    }

    return NextResponse.json({ updatedScores, posts } as ApiResponse, { status: 200 })
  } catch (error) {
    console.error('API Error:', error)
    return NextResponse.json({ error: 'Failed to fetch search data' }, { status: 500 })
  }
}