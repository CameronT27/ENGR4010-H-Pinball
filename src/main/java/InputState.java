public final class InputState {
    private boolean leftFlipper;
    private boolean rightFlipper;
    private boolean launchButton;
    private boolean nudgeLeft;
    private boolean nudgeRight;
    private boolean previousLeftFlipper;
    private boolean previousRightFlipper;

    public void setLeftFlipper(boolean pressed) {
        leftFlipper = pressed;
    }

    public void setRightFlipper(boolean pressed) {
        rightFlipper = pressed;
    }

    public boolean leftFlipper() {
        return leftFlipper;
    }

    public boolean leftFlipperPressedThisFrame() {
        return leftFlipper && !previousLeftFlipper;
    }

    public boolean rightFlipper() {
        return rightFlipper;
    }

    public boolean rightFlipperPressedThisFrame() {
        return rightFlipper && !previousRightFlipper;
    }

    public void advanceFrame() {
        previousLeftFlipper = leftFlipper;
        previousRightFlipper = rightFlipper;
    }

    public void setLaunchButton(boolean pressed) {
        launchButton = pressed;
    }

    public boolean launchButton() {
        return launchButton;
    }

    public void setNudgeLeft(boolean pressed) {
        nudgeLeft = pressed;
    }

    public void setNudgeRight(boolean pressed) {
        nudgeRight = pressed;
    }

    public boolean nudgeLeft() {
        return nudgeLeft;
    }

    public boolean nudgeRight() {
        return nudgeRight;
    }
}