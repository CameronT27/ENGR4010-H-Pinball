public final class InputState {
    private boolean leftFlipper;
    private boolean rightFlipper;

    public void setLeftFlipper(boolean pressed) {
        leftFlipper = pressed;
    }

    public void setRightFlipper(boolean pressed) {
        rightFlipper = pressed;
    }

    public boolean leftFlipper() {
        return leftFlipper;
    }

    public boolean rightFlipper() {
        return rightFlipper;
    }
}