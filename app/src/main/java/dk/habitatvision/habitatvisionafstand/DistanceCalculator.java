package dk.habitatvision.habitatvisionafstand;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Is used for calculating the calibrated real world value
 */
public class DistanceCalculator {

    public static double calculateRealDistanceInMeters(double rawDistance) {
        return 3 * round(rawDistance, 2) + 0.4;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
