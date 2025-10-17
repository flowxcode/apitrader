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
            IEngine.OrderCommand cmd = rand.nextBoolean() ? IEngine.OrderCommand.BUY : IEngine.OrderCommand.SELL;
            double amount = 100.0;
            String label = "random_order_" + (++orderCounter);
            engine.submitOrder(label, instrument, cmd, amount);
            console.getOut().println("Placed random " + cmd + " order: " + label);
        }
    }
}