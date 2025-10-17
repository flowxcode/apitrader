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
    private double advisoryWeight = -10.0; // weight constant: -10 to +10, negative biases towards SELL
    private int currentMinute = 0; // Track minute for hourly randomness
    
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.userInterface = context.getUserInterface();
        this.rand = new Random();
        this.orderCounter = 0;
        this.currentMinute = 0;
        
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
        if (instrument == Instrument.EURUSD && period == Period.FOUR_HOURS) {
            // Update current minute from bar time
            currentMinute = (int) ((bidBar.getTime() / (60 * 1000)) % 60);
            
            // Exact every 10 min: place order if minute % 10 == 0
            if (currentMinute % 10 != 0) return;
            
            Filter filter = Filter.NO_FILTER;
            double[] smaValues = indicators.sma(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 10, filter, 1, bidBar.getTime(), 0);
            if (smaValues.length == 0) return;
            double sma10 = smaValues[0];
            double currentClose = bidBar.getClose();
            
            double pip = instrument.getPipValue();
            double diffPips = (currentClose - sma10) / pip;
            double trendConstant = -10.0; // pips threshold for full bias
            double biasStrength = Math.min(1.0, Math.abs(diffPips) / trendConstant);
            
            // Trend-based bias
            double trendBias = (diffPips < 0) ? 0.3 * biasStrength : -0.3 * biasStrength;
            
            // Advisory weight bias: -10 to +10 normalized to -0.5 to +0.5 (negative weight increases SELL prob)
            double weightBias = - (advisoryWeight / 20.0);
            
            // Combined probSell: base 0.5 + trend + weight, clamped 0-1
            double probSell = Math.max(0.0, Math.min(1.0, 0.5 + trendBias + weightBias));
            
            IEngine.OrderCommand cmd = (rand.nextDouble() < probSell) ? IEngine.OrderCommand.SELL : IEngine.OrderCommand.BUY;
            
            double amount = 0.01; // Micro lots to avoid margin issues
            String label = "random_order_" + (++orderCounter);
            
            double slDistance = 60 * pip;
            double tpDistance = 100 * pip; // Increased TP by 20% (was 20)
            double price = 0.0; // market order
            long goodTill = 0;
            int slippage = 0;
            String comment = "";
            double fillPrice, sl, tp;
            
            if (cmd == IEngine.OrderCommand.BUY) {
                fillPrice = askBar.getClose();
                sl = fillPrice - slDistance;
                tp = fillPrice + tpDistance;
            } else {
                fillPrice = bidBar.getClose();
                sl = fillPrice + slDistance;
                tp = fillPrice - tpDistance;
            }
            
            IOrder order = engine.submitOrder(label, instrument, cmd, amount, goodTill, price, sl, tp, slippage, comment);
            
            console.getOut().println("Placed random " + cmd + " order: " + label + " (minute: " + currentMinute + ", diffPips: " + diffPips + ", probSell: " + probSell + ", amount" + amount + ", weight: " + advisoryWeight + ") SL: " + sl + " TP: " + tp);
        }
    }
}