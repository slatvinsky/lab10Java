package xyz.dragonnest.saesentsessis;

import java.util.function.DoubleUnaryOperator;

public class IntegralProcessor {

    private double a;
    private double b;
    private int n;
    private DoubleUnaryOperator func;

    public IntegralProcessor(double a, double b, int n, DoubleUnaryOperator f) {
        this.a = a;
        this.b = b;
        this.n = n;
        this.func = f;
    }

    public double calc() {
        System.out.println(a+" "+b+" "+n+" "+" "+func);
        double h = (this.b-this.a)/this.n, result = 0;
        for (int i = 0; i < this.n-1; i++) {
            double x = this.a+i*h;
            double b = (x+h/2);
            result += this.func.applyAsDouble(b)*h;
        }
        return result;
    }
}
