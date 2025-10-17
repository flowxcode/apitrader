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
            Filter filter = Filter.NO_FILTER;
            double[] smaValues = indicators.sma(instrument, period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 10, filter, 1, bidBar.getTime(), 0);
            if (smaValues.length == 0) return;
            double sma10 = smaValues[0];
            double currentClose = bidBar.getClose();
            
            double pip = instrument.getPipValue();
            double diffPips = (currentClose - sma10) / pip;
            double trendConstant = 10.0; // pips threshold for full bias (from -10 trend constant)
            double biasStrength = Math.min(1.0, Math.abs(diffPips) / trendConstant);
            double probSell;
            if (diffPips < 0) { // bearish
                probSell = 0.5 + 0.3 * biasStrength;
            } else {
                probSell = 0.5 - 0.3 * biasStrength;
            }
            IEngine.OrderCommand cmd = (rand.nextDouble() < probSell) ? IEngine.OrderCommand.SELL : IEngine.OrderCommand.BUY;
            
            double amount = 100.0;
            String label = "random_order_" + (++orderCounter);
            
            double slDistance = 10 * pip;
            double tpDistance = 20 * pip;
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
            
            console.getOut().println("Placed random " + cmd + " order: " + label + " (diffPips: " + diffPips + ", probSell: " + probSell + ") SL: " + sl + " TP: " + tp);
        }
    }
}