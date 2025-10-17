package jforex;

import java.util.*;

import com.dukascopy.api.*;

public class StrategyCalc implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private List<Double> dailyRanges = new ArrayList<>(); // Store daily pip ranges (non-zero only)
    private long previousDay = 0; // Track previous bar's day
    private double dailyHigh = Double.MIN_VALUE;
    private double dailyLow = Double.MAX_VALUE;
    
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
        
        Set<Instrument> instruments = new HashSet<>();
        instruments.add(Instrument.EURUSD);
        context.setSubscribedInstruments(instruments, true);
        
        console.getOut().println("Daily Range Calculator started for EURUSD");
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
        // Process the last day if not already done
        if (dailyHigh != Double.MIN_VALUE && dailyLow != Double.MAX_VALUE) {
            double pip = Instrument.EURUSD.getPipValue();
            double rangePips = (dailyHigh - dailyLow) / pip;
            long lastDay = previousDay;
            console.getOut().println("Day " + new Date(lastDay * 24 * 60 * 60 * 1000) + ": range " + String.format("%.1f", rangePips) + " pips");
            
            // Add to list only if non-zero (exclude Sundays/0 movement)
            if (rangePips > 0) {
                dailyRanges.add(rangePips);
            }
        }
        
        // Summary (excluding zeros)
        if (!dailyRanges.isEmpty()) {
            double minRange = dailyRanges.stream().min(Double::compare).get();
            double maxRange = dailyRanges.stream().max(Double::compare).get();
            double avgRange = dailyRanges.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
            console.getOut().println("Overall Summary (excluding 0-range days):");
            console.getOut().println("Min daily range: " + String.format("%.1f", minRange) + " pips");
            console.getOut().println("Max daily range: " + String.format("%.1f", maxRange) + " pips");
            console.getOut().println("Avg daily range: " + String.format("%.1f", avgRange) + " pips");
        } else {
            console.getOut().println("No non-zero data processed.");
        }
        
        console.getOut().println("Daily Range Calculator stopped");
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }
    
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (instrument == Instrument.EURUSD && period == Period.DAILY) {
            long currentDay = bidBar.getTime() / (24 * 60 * 60 * 1000); // Day timestamp (midnight)
            
            // New day detected
            if (currentDay != previousDay && previousDay != 0) {
                // Process previous day
                double pip = instrument.getPipValue();
                double rangePips = (dailyHigh - dailyLow) / pip;
                console.getOut().println("Day " + new Date(previousDay * 24 * 60 * 60 * 1000) + ": range " + String.format("%.1f", rangePips) + " pips");
                
                // Add to list only if non-zero (exclude Sundays/0 movement)
                if (rangePips > 0) {
                    dailyRanges.add(rangePips);
                }
                
                // Reset for new day
                dailyHigh = Double.MIN_VALUE;
                dailyLow = Double.MAX_VALUE;
            }
            
            // Update current day's high/low using bid bar (or ask, but bid for consistency)
            dailyHigh = Math.max(dailyHigh, bidBar.getHigh());
            dailyLow = Math.min(dailyLow, bidBar.getLow());
            
            previousDay = currentDay;
        }
    }
}