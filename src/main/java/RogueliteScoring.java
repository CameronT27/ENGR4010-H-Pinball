import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class RogueliteScoring {

    /**
     * Calculates the polynomial-exponential scaling target infinitely.
     * 
     * @param currentLevel The current stage/loop
     * @param baseTarget The final hardcoded target from the standard run
     * @return The scaled score target as a BigDecimal
     */
    public static BigDecimal calculateEndlessTarget(int currentLevel, double baseTarget) {
        // Normal levels do not use the formula
        if (currentLevel <= 8) {
            return BigDecimal.valueOf(baseTarget);
        }

        int c = currentLevel - 8; 
        double k = 0.75;
        double b = 1.6;
        
        // 1. Calculate the inner terms using standard double.
        // Even at level 100, this inner result is only ~5.4e16, safely inside double limits.
        double d = 1.0 + (0.2 * c);
        double innerTerm = Math.pow(k * c, d);
        double formulaBase = b + innerTerm;

        // 2. Switch to BigDecimal for the outer exponent which causes the crash.
        BigDecimal bigBase = BigDecimal.valueOf(formulaBase);
        
        // rawScore = baseTarget * (formulaBase ^ c)
        BigDecimal rawScore = bigBase.pow(c).multiply(BigDecimal.valueOf(baseTarget));

        // 3. Truncate to the 2 most significant digits (e.g., 115,400 -> 110,000)
        // MathContext is vastly cleaner than logarithm modulo math for this.
        MathContext mc = new MathContext(2, RoundingMode.DOWN);
        BigDecimal truncatedScore = rawScore.round(mc);
        
        // Strip trailing decimals generated during calculation
        return truncatedScore.setScale(0, RoundingMode.DOWN);
    }
    
    public static void main(String[] args) {
        System.out.println("Level 12 Target: " + calculateEndlessTarget(12, 50000.0)); 
        
        // Ante 39 is where Balatro's original double math hard-crashes (returns NaN)
        System.out.println("Level 39 Target: " + calculateEndlessTarget(39, 50000.0)); 
        
        // BigDecimal easily handles targets that shatter the floating-point limit
        System.out.println("Level 100 Target: " + calculateEndlessTarget(100, 50000.0)); 
    }
}