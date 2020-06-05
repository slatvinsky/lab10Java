package xyz.dragonnest.saesentsessis;

import java.util.function.DoubleUnaryOperator;

public class ThreadProcessor implements Runnable {
    private IntegralProcessor calculator;
    private Controller controller;

    public ThreadProcessor(Controller controller, double a, double b, int n, DoubleUnaryOperator f) {
        calculator = new IntegralProcessor(a, b, n, f);
        this.controller = controller;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        double res = calculator.calc();
        controller.sendResult(res, start);
    }
}
