package util;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Simple Either monad for returning business rule errors from application
 * layer methods without throwing exceptions.
 *
 * Usage:
 *   Either<String, Pilot> result = controller.addPilot(...);
 *   if (result.isLeft())  System.out.println(result.getLeft());   // error
 *   if (result.isRight()) doSomethingWith(result.getRight());     // success
 *
 * @param <L> left type — conventionally the error (String or custom error)
 * @param <R> right type — conventionally the success value
 */
public final class Either<L, R> {

    private final L left;
    private final R right;
    private final boolean isRight;

    private Either(final L left, final R right, final boolean isRight) {
        this.left    = left;
        this.right   = right;
        this.isRight = isRight;
    }

    public static <L, R> Either<L, R> left(final L value) {
        return new Either<>(value, null, false);
    }

    public static <L, R> Either<L, R> right(final R value) {
        return new Either<>(null, value, true);
    }

    public boolean isLeft()  { return !isRight; }
    public boolean isRight() { return isRight; }

    public L getLeft() {
        if (isRight) throw new IllegalStateException("Either is Right — no Left value");
        return left;
    }

    public R getRight() {
        if (!isRight) throw new IllegalStateException("Either is Left — no Right value");
        return right;
    }

    public <T> T fold(final Function<L, T> onLeft, final Function<R, T> onRight) {
        return isRight ? onRight.apply(right) : onLeft.apply(left);
    }

    public void ifLeft(final Consumer<L> action)  { if (!isRight) action.accept(left); }
    public void ifRight(final Consumer<R> action) { if (isRight)  action.accept(right); }

    @Override
    public String toString() {
        return isRight ? "Right(" + right + ")" : "Left(" + left + ")";
    }
}