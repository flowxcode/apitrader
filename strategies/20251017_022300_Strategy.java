package jforex;

import java.util.*;

import com.dukascopy.api.*;

public class Strategy implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IUserInterface userInterface;
    private Random rand;
    private int orderCounter = 0;
    private final double amount = 0.01;
    private final int atrPeriod = 14;
    private final double slMultiplier = 0.5; // Adjusted for approx 1 hour: half of hourly ATR
    private final double tpMultiplier = 1.0; // TP twice SL for risk-reward
    private final double TICK_SIZE = 0.00001; // For EURUSD, 0.1 pip
    
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
        this.rand = new Random();
        this.orderCounter = 0;
        
        Set<Instrument> instruments = new HashSet<>();
        instruments.add(Instrument.EURUSD);
        context.setSubscribedInstruments(instruments, true);
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
    }

    public void onStop() throws JFException {
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }
    
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (instrument == Instrument.EURUSD && period == Period.ONE_HOUR) {
            double hourlyAtr = indicators.atr(instrument, Period.ONE_HOUR, OfferSide.BID, atrPeriod, 0);
            double slDistance = hourlyAtr * slMultiplier;
            double tpDistance = hourlyAtr * tpMultiplier;
            
            IEngine.OrderCommand cmd = rand.nextBoolean() ? IEngine.OrderCommand.BUY : IEngine.OrderCommand.SELL;
            double entryPrice = 0.0; // Market order
            double slPrice = 0.0;
            double tpPrice = 0.0;
            
            if (cmd == IEngine.OrderCommand.BUY) {
                slPrice = bidBar.getClose() - slDistance;
                tpPrice = askBar.getClose() + tpDistance;
            } else {
                slPrice = askBar.getClose() + slDistance;
                tpPrice = bidBar.getClose() - tpDistance;
            }
            
            // Round to tick size (0.00001 for EURUSD)
            slPrice = Math.round(slPrice / TICK_SIZE) * TICK_SIZE;
            tpPrice = Math.round(tpPrice / TICK_SIZE) * TICK_SIZE;
            
            String label = "random_order_" + (++orderCounter);
            double slippage = 2.0; // 2 pips slippage
            long goodTillTime = 0L; // Immediate
            String comment = null;
            engine.submitOrder(label, instrument, cmd, amount, entryPrice, slippage, slPrice, tpPrice, goodTillTime, comment);
            console.getOut().println("Placed random " + cmd + " order: " + label + " SL:" + slPrice + " TP:" + tpPrice + " ATR:" + hourlyAtr);
        }
    }
}