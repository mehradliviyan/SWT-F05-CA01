
package autopilot.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SteeringController Unit Tests")
class SteeringControllerTest {

    private SteeringController steeringController;


    // ---------------------------------------------------------
    // Test constants
    // ---------------------------------------------------------

    private static final double INITIAL_ANGLE_DEGREES = 0.0;

    private static final double PHYSICAL_LIMIT_DEGREES = 450.0;
    private static final double HIGH_SPEED_LIMIT_DEGREES = 90.0;
    private static final double HIGH_SPEED_THRESHOLD_KMH = 120.0;
    private static final double MAX_CHANGE_PER_TICK_DEGREES = 60.0;

    private static final double REPRESENTATIVE_LOW_SPEED_KMH = 50.0;
    private static final double REPRESENTATIVE_HIGH_SPEED_KMH = 150.0;

    // ---------------------------------------------------------
    // Test lifecycle
    // ---------------------------------------------------------

    @BeforeAll
    static void beforeAll() {
        System.out.println(
                "========== Starting SteeringController Tests =========="
        );
    }

    @BeforeEach
    void setUp() {
        System.out.println(
                "---- Setting up a new SteeringController test ----"
        );

        steeringController = new SteeringController();
    }


    // ---------------------------------------------------------
    // Initial state
    // ---------------------------------------------------------

    @Test
    @DisplayName("Initial current angle should be zero")
    void initialState_shouldHaveZeroCurrentAngle() {
        assertEquals(
                INITIAL_ANGLE_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Initial target angle should be zero")
    void initialState_shouldHaveZeroTargetAngle() {
        assertEquals(
                INITIAL_ANGLE_DEGREES,
                steeringController.getTargetAngleDegrees()
        );
    }

    // ---------------------------------------------------------
    // Requested angle validation
    // ---------------------------------------------------------

    @ParameterizedTest(name = "Invalid requested angle: {0}")
    @ValueSource(doubles = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Should reject non-finite requested angles")
    void update_whenRequestedAngleIsNonFinite_shouldThrowIllegalArgumentException(
            double requestedAngleDegrees
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> steeringController.update(
                        requestedAngleDegrees,
                        REPRESENTATIVE_LOW_SPEED_KMH
                )
        );

        assertEquals(
                "requested angle must be finite",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "Invalid requested angle: {0}")
    @ValueSource(doubles = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Invalid requested angles should preserve the current angle")
    void update_whenRequestedAngleIsNonFinite_shouldPreserveCurrentAngle(
            double requestedAngleDegrees
    ) {
        steeringController.update(
                150.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> steeringController.update(
                        requestedAngleDegrees,
                        REPRESENTATIVE_LOW_SPEED_KMH
                )
        );

        assertEquals(
                MAX_CHANGE_PER_TICK_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @ParameterizedTest(name = "Invalid requested angle: {0}")
    @ValueSource(doubles = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Invalid requested angles should preserve the target angle")
    void update_whenRequestedAngleIsNonFinite_shouldPreserveTargetAngle(
            double requestedAngleDegrees
    ) {
        steeringController.update(
                150.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> steeringController.update(
                        requestedAngleDegrees,
                        REPRESENTATIVE_LOW_SPEED_KMH
                )
        );

        assertEquals(
                150.0,
                steeringController.getTargetAngleDegrees()
        );
    }

    // ---------------------------------------------------------
    // Speed validation
    // ---------------------------------------------------------

    @ParameterizedTest(name = "Invalid speed: {0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Should reject invalid speeds")
    void update_whenSpeedIsInvalid_shouldThrowIllegalArgumentException(
            double speedKmh
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> steeringController.update(30.0, speedKmh)
        );

        assertEquals(
                "speed must be a finite non-negative value",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "Invalid speed: {0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Invalid speeds should preserve the current angle")
    void update_whenSpeedIsInvalid_shouldPreserveCurrentAngle(
            double speedKmh
    ) {
        steeringController.update(
                150.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> steeringController.update(30.0, speedKmh)
        );

        assertEquals(
                MAX_CHANGE_PER_TICK_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @ParameterizedTest(name = "Invalid speed: {0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Invalid speeds should preserve the target angle")
    void update_whenSpeedIsInvalid_shouldPreserveTargetAngle(
            double speedKmh
    ) {
        steeringController.update(
                150.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> steeringController.update(30.0, speedKmh)
        );

        assertEquals(
                150.0,
                steeringController.getTargetAngleDegrees()
        );
    }

    // ---------------------------------------------------------
    // Validation order
    // ---------------------------------------------------------

    @Test
    @DisplayName("Requested angle should be validated before speed")
    void update_whenAngleAndSpeedAreInvalid_shouldValidateAngleFirst() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> steeringController.update(
                        Double.NaN,
                        -1.0
                )
        );

        assertEquals(
                "requested angle must be finite",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // Physical steering limits
    // ---------------------------------------------------------


    @Test
    @DisplayName("Zero speed should be accepted and use physical steering limits")
    void update_whenSpeedIsZero_shouldUsePhysicalSteeringLimits() {
        steeringController.update(
                PHYSICAL_LIMIT_DEGREES,
                0.0
        );

        assertEquals(
                PHYSICAL_LIMIT_DEGREES,
                steeringController.getTargetAngleDegrees()
        );
    }

    @ParameterizedTest(name = "requested angle={0}, expected target={1}")
    @CsvSource({
            "-1000.0, -450.0",
            "-451.0,  -450.0",
            "-450.0,  -450.0",
            "-200.0,  -200.0",
            "-60.0,    -60.0",
            "0.0,        0.0",
            "60.0,      60.0",
            "200.0,    200.0",
            "450.0,    450.0",
            "451.0,    450.0",
            "1000.0,   450.0"
    })
    @DisplayName("Requested angles should be clamped to physical limits at low speed")
    void update_atLowSpeed_shouldClampTargetToPhysicalLimits(
            double requestedAngleDegrees,
            double expectedTargetAngleDegrees
    ) {
        steeringController.update(
                requestedAngleDegrees,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                expectedTargetAngleDegrees,
                steeringController.getTargetAngleDegrees()
        );
    }

    @ParameterizedTest(name = "requested angle={0}, expected target={1}")
    @CsvSource({
            "-1000.0, -450.0",
            "-451.0,  -450.0",
            "-450.0,  -450.0",
            "450.0,    450.0",
            "451.0,    450.0",
            "1000.0,   450.0"
    })
    @DisplayName("The physical limit should apply at exactly the high-speed threshold")
    void update_atExactlyHighSpeedThreshold_shouldUsePhysicalLimit(
            double requestedAngleDegrees,
            double expectedTargetAngleDegrees
    ) {
        steeringController.update(
                requestedAngleDegrees,
                HIGH_SPEED_THRESHOLD_KMH
        );

        assertEquals(
                expectedTargetAngleDegrees,
                steeringController.getTargetAngleDegrees()
        );
    }

    // ---------------------------------------------------------
    // High-speed steering limits
    // ---------------------------------------------------------

    @ParameterizedTest(name = "requested angle={0}, expected target={1}")
    @CsvSource({
            "-1000.0, -90.0",
            "-451.0,   -90.0",
            "-450.0,   -90.0",
            "-100.0,   -90.0",
            "-90.0,    -90.0",
            "-60.0,    -60.0",
            "0.0,        0.0",
            "60.0,      60.0",
            "90.0,      90.0",
            "100.0,     90.0",
            "450.0,     90.0",
            "451.0,     90.0",
            "1000.0,    90.0"
    })
    @DisplayName("Requested angles should be clamped to high-speed limits")
    void update_aboveHighSpeedThreshold_shouldClampTargetToHighSpeedLimit(
            double requestedAngleDegrees,
            double expectedTargetAngleDegrees
    ) {
        steeringController.update(
                requestedAngleDegrees,
                REPRESENTATIVE_HIGH_SPEED_KMH
        );

        assertEquals(
                expectedTargetAngleDegrees,
                steeringController.getTargetAngleDegrees()
        );
    }

    @ParameterizedTest(name = "speed={0}, expected target={1}")
    @CsvSource({
            "120.0,    450.0",
            "120.0001,  90.0"
    })
    @DisplayName("Steering limit should change immediately above the speed threshold")
    void update_aroundHighSpeedThreshold_shouldSelectCorrectLimit(
            double speedKmh,
            double expectedTargetAngleDegrees
    ) {
        steeringController.update(
                PHYSICAL_LIMIT_DEGREES,
                speedKmh
        );

        assertEquals(
                expectedTargetAngleDegrees,
                steeringController.getTargetAngleDegrees()
        );
    }

    @Test
    @DisplayName("Should apply high-speed limit at the smallest representable speed above threshold")
    void update_atNextRepresentableSpeedAboveThreshold_shouldUseHighSpeedLimit() {
        steeringController.update(
                PHYSICAL_LIMIT_DEGREES,
                Math.nextUp(HIGH_SPEED_THRESHOLD_KMH)
        );

        assertEquals(
                HIGH_SPEED_LIMIT_DEGREES,
                steeringController.getTargetAngleDegrees()
        );
    }

    @Test
    @DisplayName("Should accept the largest finite speed")
    void update_whenSpeedIsLargestFiniteValue_shouldAcceptSpeed() {
        steeringController.update(
                30.0,
                Double.MAX_VALUE
        );

        assertEquals(
                30.0,
                steeringController.getTargetAngleDegrees()
        );
    }

    @Test
    @DisplayName("Should accept the largest finite requested angle and clamp it")
    void update_whenRequestedAngleIsLargestFiniteValue_shouldClampToPhysicalLimit() {
        steeringController.update(
                Double.MAX_VALUE,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                PHYSICAL_LIMIT_DEGREES,
                steeringController.getTargetAngleDegrees()
        );
    }

    @Test
    @DisplayName("Should accept the smallest finite negative requested angle and clamp it")
    void update_whenRequestedAngleIsNegativeLargestFiniteValue_shouldClampToNegativePhysicalLimit() {
        steeringController.update(
                -Double.MAX_VALUE,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                -PHYSICAL_LIMIT_DEGREES,
                steeringController.getTargetAngleDegrees()
        );
    }

    // ---------------------------------------------------------
    // Maximum change per tick
    // ---------------------------------------------------------

    @ParameterizedTest(name = "requested angle={0}, expected current angle={1}")
    @CsvSource({
            "60.0,    60.0",
            "60.001,  60.0",
            "90.0,    60.0",
            "150.0,   60.0",
            "450.0,   60.0",
            "-60.0,  -60.0",
            "-60.001, -60.0",
            "-90.0,  -60.0",
            "-150.0, -60.0",
            "-450.0, -60.0"
    })
    @DisplayName("Current angle should change by at most sixty degrees per update")
    void update_whenTargetIsWithinOrBeyondOneTick_shouldLimitCurrentAngleChange(
            double requestedAngleDegrees,
            double expectedCurrentAngleDegrees
    ) {
        steeringController.update(
                requestedAngleDegrees,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                expectedCurrentAngleDegrees,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @ParameterizedTest(name = "requested angle={0}, expected current angle={1}")
    @CsvSource({
            "10.0,  10.0",
            "30.0,  30.0",
            "59.0,  59.0",
            "-10.0, -10.0",
            "-30.0, -30.0",
            "-59.0, -59.0"
    })
    @DisplayName("Current angle should reach targets closer than one tick")
    void update_whenTargetIsCloserThanOneTick_shouldReachTargetImmediately(
            double requestedAngleDegrees,
            double expectedCurrentAngleDegrees
    ) {
        steeringController.update(
                requestedAngleDegrees,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                expectedCurrentAngleDegrees,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Current angle should reach a target exactly sixty degrees away")
    void update_whenTargetIsExactlyOneTickAway_shouldReachTarget() {
        steeringController.update(
                MAX_CHANGE_PER_TICK_DEGREES,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                MAX_CHANGE_PER_TICK_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Current angle should move toward a positive target")
    void update_whenTargetIsPositive_shouldMoveCurrentAnglePositively() {
        steeringController.update(
                150.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                MAX_CHANGE_PER_TICK_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Current angle should move toward a negative target")
    void update_whenTargetIsNegative_shouldMoveCurrentAngleNegatively() {
        steeringController.update(
                -150.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                -MAX_CHANGE_PER_TICK_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Current angle should move toward a reversed target")
    void update_whenTargetDirectionReverses_shouldMoveTowardNewTarget() {
        steeringController.update(
                120.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        steeringController.update(
                -120.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                INITIAL_ANGLE_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Target angle should update even when current angle cannot reach it immediately")
    void update_whenTargetIsBeyondOneTick_shouldStoreFullTargetAngle() {
        steeringController.update(
                150.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        assertEquals(
                150.0,
                steeringController.getTargetAngleDegrees()
        );
    }

    // ---------------------------------------------------------
    // Reaching the target over multiple updates
    // ---------------------------------------------------------

    @ParameterizedTest(name = "requested angle={0}")
    @MethodSource("targetTrackingScenarios")
    @DisplayName("Repeated updates should eventually reach the target angle")
    void update_whenCalledRepeatedly_shouldEventuallyReachTarget(
            double requestedAngleDegrees,
            List<Double> expectedCurrentAngles
    ) {
        for (double expectedCurrentAngle : expectedCurrentAngles) {
            assertEquals(
                    expectedCurrentAngle,
                    steeringController.update(
                            requestedAngleDegrees,
                            REPRESENTATIVE_LOW_SPEED_KMH
                    )
            );
        }

        assertEquals(
                requestedAngleDegrees,
                steeringController.getCurrentAngleDegrees()
        );
    }

    static Stream<Arguments> targetTrackingScenarios() {
        return Stream.of(
                Arguments.of(
                        150.0,
                        List.of(60.0, 120.0, 150.0)
                ),
                Arguments.of(
                        -150.0,
                        List.of(-60.0, -120.0, -150.0)
                ),
                Arguments.of(
                        120.0,
                        List.of(60.0, 120.0)
                ),
                Arguments.of(
                        -120.0,
                        List.of(-60.0, -120.0)
                ),
                Arguments.of(
                        30.0,
                        List.of(30.0)
                ),
                Arguments.of(
                        -30.0,
                        List.of(-30.0)
                )
        );
    }

    @Test
    @DisplayName("Increasing speed should apply the high-speed limit to the new target")
    void update_whenSpeedIncreasesAboveThreshold_shouldClampNewTarget() {
        steeringController.update(180.0, REPRESENTATIVE_LOW_SPEED_KMH);
        steeringController.update(180.0, REPRESENTATIVE_LOW_SPEED_KMH);
        steeringController.update(180.0, REPRESENTATIVE_LOW_SPEED_KMH);

        double currentAngle = steeringController.update(
                PHYSICAL_LIMIT_DEGREES,
                REPRESENTATIVE_HIGH_SPEED_KMH
        );

        assertEquals(HIGH_SPEED_LIMIT_DEGREES,
                steeringController.getTargetAngleDegrees());
        assertEquals(120.0, currentAngle);
        assertEquals(120.0, steeringController.getCurrentAngleDegrees());
    }

    // ---------------------------------------------------------
    // Self-centering
    // ---------------------------------------------------------

    @Test
    @DisplayName("Self-centering should set the target angle to zero")
    void selfCenter_whenCalled_shouldSetTargetAngleToZero() {
        steeringController.update(
                150.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        steeringController.selfCenter();

        assertEquals(
                INITIAL_ANGLE_DEGREES,
                steeringController.getTargetAngleDegrees()
        );
    }

    @Test
    @DisplayName("Self-centering should move current angle toward zero by one tick")
    void selfCenter_whenCurrentAngleIsPositive_shouldMoveTowardZero() {
        steeringController.update(
                120.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        steeringController.update(
                120.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        steeringController.selfCenter();

        assertEquals(
                MAX_CHANGE_PER_TICK_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Self-centering should move a negative current angle toward zero")
    void selfCenter_whenCurrentAngleIsNegative_shouldMoveTowardZero() {
        steeringController.update(
                -120.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        steeringController.update(
                -120.0,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        steeringController.selfCenter();

        assertEquals(
                -MAX_CHANGE_PER_TICK_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Self-centering should reach zero when the remaining angle is exactly one tick")
    void selfCenter_whenCurrentAngleIsExactlyOneTickAway_shouldReachZero() {
        steeringController.update(
                MAX_CHANGE_PER_TICK_DEGREES,
                REPRESENTATIVE_LOW_SPEED_KMH
        );

        steeringController.selfCenter();

        assertEquals(
                INITIAL_ANGLE_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }


    @Test
    @DisplayName("Repeated self-centering should eventually return current angle to zero")
    void selfCenter_whenCalledRepeatedly_shouldEventuallyReachZero() {
        steeringController.update(180.0, REPRESENTATIVE_LOW_SPEED_KMH);
        steeringController.update(180.0, REPRESENTATIVE_LOW_SPEED_KMH);
        steeringController.update(180.0, REPRESENTATIVE_LOW_SPEED_KMH);

        assertEquals(180.0, steeringController.getCurrentAngleDegrees());

        assertEquals(120.0, steeringController.selfCenter());
        assertEquals(60.0, steeringController.selfCenter());
        assertEquals(INITIAL_ANGLE_DEGREES, steeringController.selfCenter());

        assertEquals(INITIAL_ANGLE_DEGREES, steeringController.getTargetAngleDegrees());
    }


    @Test
    @DisplayName("Self-centering at zero should keep current angle at zero")
    void selfCenter_whenCurrentAngleIsZero_shouldKeepCurrentAngleAtZero() {
        steeringController.selfCenter();

        assertEquals(
                INITIAL_ANGLE_DEGREES,
                steeringController.getCurrentAngleDegrees()
        );
    }

    @Test
    @DisplayName("Self-centering should leave current angle unchanged when already centered")
    void selfCenter_whenAlreadyCentered_shouldKeepTargetAtZero() {
        steeringController.selfCenter();

        assertEquals(
                INITIAL_ANGLE_DEGREES,
                steeringController.getTargetAngleDegrees()
        );
    }

    @AfterEach
    void tearDown() {
        System.out.println(
                "---- Finished a SteeringController test ----"
        );
    }

    @AfterAll
    static void afterAll() {
        System.out.println(
                "========== Finished all SteeringController Tests =========="
        );
    }
}

