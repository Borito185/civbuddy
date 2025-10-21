package calc;

import com.civbuddy.calc.CalculatorClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorClientTest {
    CalculatorClient sut;
    @BeforeEach
    void setup() {
        sut = new CalculatorClient();
    }

    @ParameterizedTest
    @CsvSource({
            "1+2,3",
            "10-7,3",
            "6*7,42",
            "8/2,4",
            "(2+3)*4,20",
            "2+3*4,14",
            "12/(2+1),4"
    })
    void basicExpressions(String expr, double expected) {
        assertEquals(expected, sut.eval(expr), 1e-9);
    }

    @ParameterizedTest(name = "{index} ⇒ {0} = {1}")
    @CsvSource({
            // simple literals
            "1s, 64",
            "2s, 128",
            "0.5s, 32",
            "1ci, 64",
            "1cs, 4096",
            "3k, 3000",

            // arithmetic mixes
            "2cs + 1s, 8256",
            "1k + 2s*10, 2280",
            "5s - 3s, 128",
            "(10k) / (2s), 78.125",
            "1k*2s, 128000",
            "3k - 2cs, -5192",
            "(2k + 3s) / s, 34.25"
    })
    void shortcutsFixed(String expr, double expected) {
        Assertions.assertEquals(expected, sut.eval(expr), 1e-9);
    }

    // s/ci/cs equivalences and identities
    @ParameterizedTest
    @CsvSource({
            "cs, s*s",
            "ci, s"
    })
    void shortcutEquivalences(String a, String b) {
        Assertions.assertEquals(sut.eval(a), sut.eval(b), 1e-9);
    }

    // Scale properties for s and k over a range
    @ParameterizedTest
    @CsvSource({
            "s,64",
            "k,1000"
    })
    void shortcutScaling(String sym, double factor) {
        for (int n : new int[]{-5, -1, 0, 1, 5}) {
            String expr = n + sym;            // e.g., "-5s", "5k"
            double expected = n * factor;
            Assertions.assertEquals(expected, sut.eval(expr), 1e-9,
                    () -> "expr=" + expr);
        }
    }
}
