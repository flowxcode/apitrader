'use client'

import { useState, useEffect } from 'react'
import { TradingModel, FactorScores } from '@/lib/tradingModel'

export default function Home() {
  const [model] = useState(() => new TradingModel())
  const [signal, setSignal] = useState(0)
  const [scores, setScores] = useState<FactorScores | null>(null)
  const [hour, setHour] = useState(1)
  const [history, setHistory] = useState<string[]>([])
  const [searchResults, setSearchResults] = useState<string[]>([])
  const [tradingPair, setTradingPair] = useState<'EURUSD' | 'BTCUSD'>('EURUSD')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    // Init with mock baseline on load
    const initScores: FactorScores = {
      news: 6,
      yestTrend: 2,
      weekTrend: -1,
      todayTrend: 4,
      todaySent: 5,
      yestSent: 3,
      weekSent: 1,
      todayDev: 7,
      yestDev: -2,
    }
    model.initialize(initScores)
    const initialSignal = model.calculateHourlySignal(initScores)
    setSignal(initialSignal)
    setScores(initScores)
    setHistory([`Hour ${hour}: Signal ${initialSignal} (Buy/Sell ${Math.abs(initialSignal)} lots)`])
  }, [model])

  const nextHour = async () => {
    setLoading(true)
    try {
      // Step 1: Dummy API request for Grok Search
      const response = await fetch('/api/grok-search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pair: tradingPair }),
      })

      if (!response.ok) throw new Error('API failed')

      const { updatedScores, posts }: { updatedScores: Partial<FactorScores>; posts: string[] } = await response.json()

      // Apply deltas to current scores
      const newScores = { ...scores! }
      if (updatedScores.todaySent !== undefined) {
        newScores.todaySent = Math.max(-10, Math.min(10, scores!.todaySent + updatedScores.todaySent))
      }
      if (updatedScores.todayDev !== undefined) {
        newScores.todayDev = Math.max(-10, Math.min(10, scores!.todayDev + updatedScores.todayDev))
      }
      setScores(newScores)
      setSearchResults(posts)

      // Log search
      setHistory(h => [...h, `Hour ${hour + 1}: Grok Search on ${tradingPair} updated sentiment/dev.`])

      // Step 2: Calculate signal
      const newSignal = model.calculateHourlySignal(newScores)
      setSignal(newSignal)
      setHour(h => h + 1)
      setHistory(h => [...h, `Hour ${hour + 1}: Signal ${newSignal} (Buy/Sell ${Math.abs(newSignal)} lots)`])
      
      // Mock P&L
      console.log(`Mock P&L for Hour ${hour + 1}: $${(newSignal > 0 ? 1 : -1) * Math.abs(newSignal) * 10 + (Math.random() - 0.5) * 20}`)
    } catch (error) {
      console.error('Next Hour Error:', error)
      setHistory(h => [...h, `Hour ${hour + 1}: Error fetching search data.`])
    } finally {
      setLoading(false)
    }
  }

  const getDirection = (sig: number) => sig > 0 ? 'Buy' : sig < 0 ? 'Sell' : 'Hold'
  const getColor = (sig: number) => sig > 0 ? 'text-green-600' : sig < 0 ? 'text-red-600' : 'text-gray-600'

  return (
    <main className="min-h-screen bg-gray-100 p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">Day Trading Dashboard ({tradingPair})</h1>
        
        {/* Pair Toggle */}
        <div className="mb-4">
          <button
            onClick={() => setTradingPair('EURUSD')}
            className={`mr-2 px-3 py-1 rounded ${tradingPair === 'EURUSD' ? 'bg-blue-500 text-white' : 'bg-gray-200'}`}
          >
            EURUSD
          </button>
          <button
            onClick={() => setTradingPair('BTCUSD')}
            className={`px-3 py-1 rounded ${tradingPair === 'BTCUSD' ? 'bg-blue-500 text-white' : 'bg-gray-200'}`}
          >
            BTCUSD
          </button>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Current Signal Card */}
          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-xl font-semibold mb-4">Current Signal (Hour {hour})</h2>
            <div className={`text-4xl font-bold ${getColor(signal)}`}>
              {getDirection(signal)} {Math.abs(signal)} Lots
            </div>
            <p className="mt-2 text-sm text-gray-600">
              {signal > 0 ? 'Bullish momentum building.' : signal < 0 ? 'Bearish pressure.' : 'Neutral; minimal position.'}
            </p>
            <button
              onClick={nextHour}
              disabled={loading}
              className="mt-4 bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:opacity-50"
            >
              {loading ? 'Searching...' : 'Next Hour (w/ API Search)'}
            </button>
          </div>

          {/* Factors Table */}
          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-xl font-semibold mb-4">Factor Scores & Weights</h2>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-2">Factor</th>
                  <th className="text-right py-2">Score (-10 to +10)</th>
                  <th className="text-right py-2">Weight</th>
                </tr>
              </thead>
              <tbody>
                {scores && Object.entries(scores).map(([key, score]) => (
                  <tr key={key} className="border-b last:border-b-0">
                    <td className="py-2 capitalize">{key.replace(/([A-Z])/g, ' $1').trim()}</td>
                    <td className={`text-right ${getColor(score)} font-mono`}>{score.toFixed(1)}</td>
                    <td className="text-right py-2 font-mono">
                      {model.getAdjustedWeight(key as keyof FactorScores, hour)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Grok Search Results */}
        <div className="mt-8 bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-4">Latest API Search Results (Dummy X Posts)</h2>
          {searchResults.length > 0 ? (
            <ul className="space-y-2 text-sm text-gray-700">
              {searchResults.map((post, i) => (
                <li key={i} className="border-l-2 border-blue-500 pl-3">{post}</li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-gray-500">No results yet—click Next Hour to search.</p>
          )}
        </div>

        {/* Trade History */}
        <div className="mt-8 bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-4">Trade History</h2>
          <ul className="space-y-2">
            {history.map((entry, i) => (
              <li key={i} className="text-sm text-gray-700">{entry}</li>
            ))}
          </ul>
        </div>
      </div>
    </main>
  )
}