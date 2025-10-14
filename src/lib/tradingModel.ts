export interface FactorScores {
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

interface SearchResult {
  updatedScores: FactorScores
  mockPosts: string[] // For UI display
}

export class TradingModel {
  private baseline: FactorScores = {} as FactorScores
  private priorToday: Partial<FactorScores> = {}
  private currentHour: number = 1

  initialize(baseline: FactorScores) {
    this.baseline = { ...baseline }
    this.currentHour = 1
    this.priorToday = {}
  }

  calculateHourlySignal(currentScores: FactorScores): number {
    // Step 2: Adjust weights
    const baseWeights = {
      news: 7,
      yestTrend: 2,
      weekTrend: 3,
      todayTrend: 2,
      todaySent: 4,
      yestSent: 3,
      weekSent: 2,
      todayDev: 4,
      yestDev: 3,
    } as Record<keyof FactorScores, number>

    const todayFactors: (keyof FactorScores)[] = ['todayTrend', 'todaySent', 'todayDev']
    let totalWeight = 0
    const adjustedWeights: Record<keyof FactorScores, number> = {} as Record<keyof FactorScores, number>

    Object.entries(baseWeights).forEach(([key, base]) => {
      const k = key as keyof FactorScores
      if (todayFactors.includes(k)) {
        adjustedWeights[k] = Math.min(base + 0.5 * (this.currentHour - 1), base * 2)
      } else {
        adjustedWeights[k] = base
      }
      totalWeight += adjustedWeights[k]
    })

    // Blend prior for today
    todayFactors.forEach(key => {
      const prior = this.priorToday[key] || 0
      ;(currentScores as any)[key] = 0.7 * currentScores[key] + 0.3 * prior
    })

    // Step 3: Weighted sum
    let rawSum = 0
    Object.entries(currentScores).forEach(([key, score]) => {
      const k = key as keyof FactorScores
      rawSum += (score as number) * adjustedWeights[k]
    })
    const baseSignal = (rawSum / totalWeight) * 10

    // Step 4: Modifiers (simplified for demo)
    const probMod = this.calcProbMod(currentScores)
    const riskMod = this.calcRiskMod() // Mock low risk
    const stressMod = this.calcStressMod(currentScores) // Mock neutral
    let signal = baseSignal * probMod * riskMod * stressMod
    signal = Math.max(-10, Math.min(10, Math.round(signal)))

    if (Math.abs(signal) < 1) {
      signal = Math.sign(baseSignal) * 1 || 0
    }

    // Update state
    this.priorToday = {
      todayTrend: currentScores.todayTrend,
      todaySent: currentScores.todaySent,
      todayDev: currentScores.todayDev,
    }
    this.currentHour++

    return signal
  }

  getAdjustedWeight(factor: keyof FactorScores, hour: number): number {
    const baseWeights = {
      news: 7, yestTrend: 2, weekTrend: 3, todayTrend: 2,
      todaySent: 4, yestSent: 3, weekSent: 2, todayDev: 4, yestDev: 3,
    } as Record<keyof FactorScores, number>

    const todayFactors: (keyof FactorScores)[] = ['todayTrend', 'todaySent', 'todayDev']
    if (todayFactors.includes(factor)) {
      return Math.min(baseWeights[factor] + 0.5 * (hour - 1), baseWeights[factor] * 2)
    }
    return baseWeights[factor]
  }

  private calcProbMod(scores: FactorScores): number {
    const positives = Object.values(scores).filter(s => s > 0).length
    const total = Object.keys(scores).length
    return 0.5 + (positives / total) * 1 // 0.5-1.5
  }

  private calcRiskMod(): number {
    // Mock: Always 1.0 (low risk for demo; in real, use ATR/vol)
    return 1.0
  }

  private calcStressMod(scores: FactorScores): number {
    // Mock: Dispersion-based; low stress
    const mean = Object.values(scores).reduce((a, b) => a + b, 0) / Object.keys(scores).length
    const variance = Object.values(scores).reduce((a, b) => a + Math.pow(b - mean, 2), 0) / Object.keys(scores).length
    return 1 - Math.min(variance / 100, 0.3) // 0.7-1.0
  }
}