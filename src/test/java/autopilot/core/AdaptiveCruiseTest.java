
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

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdaptiveCruise Unit Tests")
class AdaptiveCruiseTest {

    private static final double DELTA = 0.000001;

    private static final double METERS_PER_KMH = 0.5;
    private static final double EMERGENCY_DISTANCE_RATIO = 0.5;
    private static final double RELEASE_DISTANCE_RATIO = 1.2;
    private static final double MAX_ACCELERATION_STEP_KMH = 5.0;
    private static final double HYSTERESIS_BRAKE_PRESSURE = 10.0;
    private static final double EMERGENCY_BRAKE_PRESSURE = 100.0;

    private static final double ZERO = 0.0;
    private static final double REPRESENTATIVE_TARGET_SPEED_KMH = 130.0;
    private static final double REPRESENTATIVE_SPEED_KMH = 100.0;

    private AdaptiveCruise adaptiveCruise;

    // ---------------------------------------------------------
    // Test lifecycle
    // ---------------------------------------------------------

    @BeforeAll
    static void beforeAll() {
        System.out.println(
                "========== Starting AdaptiveCruise Tests =========="
        );
    }

    @BeforeEach
    void setUp() {
        System.out.println(
                "---- Setting up a new AdaptiveCruise test ----"
        );

        adaptiveCruise = new AdaptiveCruise(
                REPRESENTATIVE_TARGET_SPEED_KMH
        );
    }


    // ---------------------------------------------------------
    // Constructor validation
    // ---------------------------------------------------------

    @ParameterizedTest(name = "Invalid target speed: {0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Constructor should reject invalid target speeds")
    void constructor_whenTargetSpeedIsInvalid_shouldThrowIllegalArgumentException(
            double targetSpeedKmh
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AdaptiveCruise(targetSpeedKmh)
        );

        assertEquals(
                "target speed must be a finite non-negative value",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Constructor should accept zero target speed")
    void constructor_whenTargetSpeedIsZero_shouldCreateController() {
        AdaptiveCruise controller = new AdaptiveCruise(ZERO);

        AdaptiveCruise.Decision decision = controller.update(
                ZERO,
                1.0
        );

        assertEquals(AdaptiveCruise.Mode.HOLD, decision.mode());
        assertEquals(ZERO, decision.amount(), DELTA);
    }

    @Test
    @DisplayName("Constructor should accept the largest finite target speed")
    void constructor_whenTargetSpeedIsLargestFiniteValue_shouldAcceptTarget() {
        AdaptiveCruise controller = new AdaptiveCruise(Double.MAX_VALUE);

        AdaptiveCruise.Decision decision = controller.update(
                ZERO,
                1.0
        );

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, decision.mode());
        assertEquals(
                MAX_ACCELERATION_STEP_KMH,
                decision.amount(),
                DELTA
        );
    }

    // ---------------------------------------------------------
    // Safe distance calculation
    // ---------------------------------------------------------

    @ParameterizedTest(name = "speed={0}, expected safe distance={1}")
    @CsvSource({
            "0.0,   0.0",
            "1.0,   0.5",
            "10.0,  5.0",
            "50.0, 25.0",
            "100.0, 50.0",
            "120.0, 60.0",
            "200.0, 100.0"
    })
    @DisplayName("Safe distance should equal half the current speed")
    void safeDistanceFor_whenSpeedIsValid_shouldReturnHalfSpeed(
            double speedKmh,
            double expectedDistanceMeters
    ) {
        assertEquals(
                expectedDistanceMeters,
                adaptiveCruise.safeDistanceFor(speedKmh),
                DELTA
        );
    }

    @Test
    @DisplayName("Safe distance should remain finite for the largest finite speed")
    void safeDistanceFor_whenSpeedIsLargestFiniteValue_shouldReturnFiniteDistance() {
        double safeDistance = adaptiveCruise.safeDistanceFor(
                Double.MAX_VALUE
        );

        assertTrue(Double.isFinite(safeDistance));
        assertEquals(Double.MAX_VALUE * METERS_PER_KMH, safeDistance, DELTA);
    }

    @ParameterizedTest(name = "Invalid speed: {0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Safe distance should reject invalid speeds")
    void safeDistanceFor_whenSpeedIsInvalid_shouldThrowIllegalArgumentException(
            double speedKmh
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adaptiveCruise.safeDistanceFor(speedKmh)
        );

        assertEquals(
                "speed must be a finite non-negative value",
                exception.getMessage()
        );
    }

    // ---------------------------------------------------------
    // Update input validation
    // ---------------------------------------------------------

    @ParameterizedTest(name = "Invalid speed: {0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Update should reject invalid current speeds")
    void update_whenSpeedIsInvalid_shouldThrowIllegalArgumentException(
            double speedKmh
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adaptiveCruise.update(speedKmh, 100.0)
        );

        assertEquals(
                "speed must be a finite non-negative value",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "Invalid distance: {0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Update should reject invalid distances")
    void update_whenDistanceIsInvalid_shouldThrowIllegalArgumentException(
            double distanceMeters
    ) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adaptiveCruise.update(
                        REPRESENTATIVE_SPEED_KMH,
                        distanceMeters
                )
        );

        assertEquals(
                "distance must be a finite non-negative value",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Update should validate speed before distance")
    void update_whenSpeedAndDistanceAreInvalid_shouldValidateSpeedFirst() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adaptiveCruise.update(Double.NaN, -1.0)
        );

        assertEquals(
                "speed must be a finite non-negative value",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Invalid speed should not change the existing braking state")
    void update_whenSpeedIsInvalid_shouldPreserveBrakingState() {
        AdaptiveCruise.Decision initialDecision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                20.0
        );

        assertEquals(
                AdaptiveCruise.Mode.EMERGENCY_BRAKE,
                initialDecision.mode()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> adaptiveCruise.update(Double.NaN, 55.0)
        );

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                55.0
        );

        assertEquals(AdaptiveCruise.Mode.BRAKE, decision.mode());
        assertEquals(
                HYSTERESIS_BRAKE_PRESSURE,
                decision.amount(),
                DELTA
        );
    }

    @Test
    @DisplayName("Invalid distance should not change the existing braking state")
    void update_whenDistanceIsInvalid_shouldPreserveBrakingState() {
        adaptiveCruise.update(REPRESENTATIVE_SPEED_KMH, 20.0);

        assertThrows(
                IllegalArgumentException.class,
                () -> adaptiveCruise.update(
                        REPRESENTATIVE_SPEED_KMH,
                        Double.NaN
                )
        );

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                55.0
        );

        assertEquals(AdaptiveCruise.Mode.BRAKE, decision.mode());
        assertEquals(
                HYSTERESIS_BRAKE_PRESSURE,
                decision.amount(),
                DELTA
        );
    }

    // ---------------------------------------------------------
    // Emergency braking
    // ---------------------------------------------------------

    @ParameterizedTest(name = "speed={0}, distance={1}")
    @CsvSource({
            "100.0,  0.0",
            "100.0, 10.0",
            "100.0, 25.0",
            "120.0, 30.0",
            "50.0,   6.25"
    })
    @DisplayName("Distance at or below the emergency threshold should trigger emergency braking")
    void update_whenDistanceIsAtOrBelowEmergencyThreshold_shouldEmergencyBrake(
            double speedKmh,
            double distanceMeters
    ) {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                speedKmh,
                distanceMeters
        );

        assertEquals(
                AdaptiveCruise.Mode.EMERGENCY_BRAKE,
                decision.mode()
        );

        assertEquals(
                EMERGENCY_BRAKE_PRESSURE,
                decision.amount(),
                DELTA
        );
    }

    @Test
    @DisplayName("Distance just above emergency threshold should not trigger emergency braking")
    void update_whenDistanceIsJustAboveEmergencyThreshold_shouldNotEmergencyBrake() {
        double speedKmh = 100.0;
        double emergencyDistance =
                speedKmh * METERS_PER_KMH * EMERGENCY_DISTANCE_RATIO;

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                speedKmh,
                Math.nextUp(emergencyDistance)
        );

        assertNotEquals(
                AdaptiveCruise.Mode.EMERGENCY_BRAKE,
                decision.mode()
        );
    }

    // ---------------------------------------------------------
    // Proportional braking
    // ---------------------------------------------------------

    @ParameterizedTest(name = "distance={0}, expected brake pressure={1}")
    @CsvSource({
            "26.0, 48.0",
            "30.0, 40.0",
            "35.0, 30.0",
            "40.0, 20.0",
            "49.0,  2.0"
    })
    @DisplayName("Braking pressure should be proportional to the distance deficit")
    void update_whenDistanceIsBelowSafeDistance_shouldApplyProportionalBraking(
            double distanceMeters,
            double expectedBrakePressure
    ) {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                distanceMeters
        );

        assertEquals(AdaptiveCruise.Mode.BRAKE, decision.mode());
        assertEquals(expectedBrakePressure, decision.amount(), DELTA);
    }


    @Test
    @DisplayName("Distance just above emergency threshold should apply proportional braking")
    void update_whenDistanceIsJustAboveEmergencyThreshold_shouldApplyProportionalBraking() {
        double speedKmh = 100.0;
        double safeDistance = speedKmh * METERS_PER_KMH;
        double emergencyDistance =
                safeDistance * EMERGENCY_DISTANCE_RATIO;

        double distanceMeters = Math.nextUp(emergencyDistance);

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                speedKmh,
                distanceMeters
        );

        assertEquals(AdaptiveCruise.Mode.BRAKE, decision.mode());
        assertEquals(
                (safeDistance - distanceMeters) / safeDistance * 100.0,
                decision.amount(),
                DELTA
        );
    }

    @Test
    @DisplayName("Emergency threshold should take precedence over proportional braking")
    void update_whenDistanceIsBelowEmergencyThreshold_shouldPreferEmergencyBrake() {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                20.0
        );

        assertEquals(
                AdaptiveCruise.Mode.EMERGENCY_BRAKE,
                decision.mode()
        );

        assertEquals(
                EMERGENCY_BRAKE_PRESSURE,
                decision.amount(),
                DELTA
        );
    }

    // ---------------------------------------------------------
    // Acceleration
    // ---------------------------------------------------------

    @ParameterizedTest(name = "speed={0}, target={1}, expected acceleration={2}")
    @CsvSource({
            "50.0, 100.0, 5.0",
            "95.0, 100.0, 5.0",
            "98.0, 100.0, 2.0",
            "99.0, 100.0, 1.0",
            "99.9, 100.0, 0.1"
    })
    @DisplayName("Acceleration should be capped by the maximum step and remaining speed difference")
    void update_whenDistanceIsSafeAndSpeedIsBelowTarget_shouldAccelerate(
            double speedKmh,
            double targetSpeedKmh,
            double expectedAcceleration
    ) {
        AdaptiveCruise controller = new AdaptiveCruise(targetSpeedKmh);

        double safeDistance = speedKmh * METERS_PER_KMH;

        AdaptiveCruise.Decision decision = controller.update(
                speedKmh,
                safeDistance + 1.0
        );

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, decision.mode());
        assertEquals(expectedAcceleration, decision.amount(), DELTA);
    }

    @Test
    @DisplayName("Acceleration should stop at the target speed")
    void update_whenSpeedEqualsTarget_shouldHoldSpeed() {
        AdaptiveCruise controller = new AdaptiveCruise(100.0);

        AdaptiveCruise.Decision decision = controller.update(
                100.0,
                51.0
        );

        assertEquals(AdaptiveCruise.Mode.HOLD, decision.mode());
        assertEquals(ZERO, decision.amount(), DELTA);
    }

    @Test
    @DisplayName("Acceleration should not occur when speed exceeds target")
    void update_whenSpeedExceedsTarget_shouldHoldSpeed() {
        AdaptiveCruise controller = new AdaptiveCruise(100.0);

        AdaptiveCruise.Decision decision = controller.update(
                110.0,
                56.0
        );

        assertEquals(AdaptiveCruise.Mode.HOLD, decision.mode());
        assertEquals(ZERO, decision.amount(), DELTA);
    }

    // ---------------------------------------------------------
    // Hold behavior and safe-distance boundary
    // ---------------------------------------------------------

    @Test
    @DisplayName("Distance exactly equal to safe distance should hold when not already braking")
    void update_whenDistanceEqualsSafeDistance_shouldHoldSpeed() {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                REPRESENTATIVE_SPEED_KMH * METERS_PER_KMH
        );

        assertEquals(AdaptiveCruise.Mode.HOLD, decision.mode());
        assertEquals(ZERO, decision.amount(), DELTA);
    }

    @Test
    @DisplayName("Distance above safe distance should accelerate when below target speed")
    void update_whenDistanceExceedsSafeDistanceAndSpeedIsBelowTarget_shouldAccelerate() {
        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                51.0
        );

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, decision.mode());
        assertEquals(
                MAX_ACCELERATION_STEP_KMH,
                decision.amount(),
                DELTA
        );
    }

    // ---------------------------------------------------------
    // Braking hysteresis and release behavior
    // ---------------------------------------------------------

    @ParameterizedTest(name = "distance={0}, expected mode={1}, expected amount={2}")
    @MethodSource("brakingHysteresisScenarios")
    @DisplayName("Active braking should follow hysteresis and release boundaries")
    void update_whenBrakingIsAlreadyActive_shouldApplyHysteresisRules(
            double distanceMeters,
            AdaptiveCruise.Mode expectedMode,
            double expectedAmount
    ) {
        AdaptiveCruise.Decision initialDecision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                20.0
        );

        assertEquals(
                AdaptiveCruise.Mode.EMERGENCY_BRAKE,
                initialDecision.mode()
        );

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                distanceMeters
        );

        assertEquals(expectedMode, decision.mode());
        assertEquals(expectedAmount, decision.amount(), DELTA);
    }

    static Stream<Arguments> brakingHysteresisScenarios() {
        return Stream.of(
                Arguments.of(40.0, AdaptiveCruise.Mode.BRAKE, 20.0),
                Arguments.of(49.0, AdaptiveCruise.Mode.BRAKE, 2.0),
                Arguments.of(50.0, AdaptiveCruise.Mode.BRAKE, 10.0),
                Arguments.of(55.0, AdaptiveCruise.Mode.BRAKE, 10.0),
                Arguments.of(59.9, AdaptiveCruise.Mode.BRAKE, 10.0),
                Arguments.of(60.0, AdaptiveCruise.Mode.ACCELERATE, 5.0),
                Arguments.of(70.0, AdaptiveCruise.Mode.ACCELERATE, 5.0)
        );
    }

    @Test
    @DisplayName("Braking should remain active after a non-emergency brake decision")
    void update_whenProportionalBrakingOccurs_shouldKeepBrakingActive() {
        adaptiveCruise.update(REPRESENTATIVE_SPEED_KMH, 20.0);

        AdaptiveCruise.Decision firstDecision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                40.0
        );

        assertEquals(AdaptiveCruise.Mode.BRAKE, firstDecision.mode());
        assertEquals(20.0, firstDecision.amount(), DELTA);

        AdaptiveCruise.Decision secondDecision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                55.0
        );

        assertEquals(AdaptiveCruise.Mode.BRAKE, secondDecision.mode());
        assertEquals(
                HYSTERESIS_BRAKE_PRESSURE,
                secondDecision.amount(),
                DELTA
        );
    }

    @Test
    @DisplayName("Braking should be released at the release-distance boundary")
    void update_whenDistanceReachesReleaseThreshold_shouldReleaseBraking() {
        adaptiveCruise.update(REPRESENTATIVE_SPEED_KMH, 20.0);

        double safeDistance =
                REPRESENTATIVE_SPEED_KMH * METERS_PER_KMH;
        double releaseDistance =
                safeDistance * RELEASE_DISTANCE_RATIO;

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                releaseDistance
        );

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, decision.mode());
        assertEquals(
                MAX_ACCELERATION_STEP_KMH,
                decision.amount(),
                DELTA
        );
    }

    @Test
    @DisplayName("After braking is released, the controller should no longer apply hysteresis pressure")
    void update_afterBrakingIsReleased_shouldUseNormalDecisionRules() {
        adaptiveCruise.update(REPRESENTATIVE_SPEED_KMH, 20.0);

        adaptiveCruise.update(REPRESENTATIVE_SPEED_KMH, 60.0);

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                55.0
        );

        assertEquals(AdaptiveCruise.Mode.ACCELERATE, decision.mode());
        assertEquals(
                MAX_ACCELERATION_STEP_KMH,
                decision.amount(),
                DELTA
        );
    }

    @Test
    @DisplayName("Emergency braking should take precedence while braking is already active")
    void update_whenBrakingIsActiveAndEmergencyThresholdIsReached_shouldEmergencyBrake() {
        adaptiveCruise.update(REPRESENTATIVE_SPEED_KMH, 20.0);

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                REPRESENTATIVE_SPEED_KMH,
                10.0
        );

        assertEquals(
                AdaptiveCruise.Mode.EMERGENCY_BRAKE,
                decision.mode()
        );

        assertEquals(
                EMERGENCY_BRAKE_PRESSURE,
                decision.amount(),
                DELTA
        );
    }


    @Test
    @DisplayName("Zero speed and zero distance should trigger emergency braking")
    void update_whenSpeedAndDistanceAreZero_shouldEmergencyBrake() {
        AdaptiveCruise controller = new AdaptiveCruise(
                REPRESENTATIVE_TARGET_SPEED_KMH
        );

        AdaptiveCruise.Decision decision = controller.update(
                ZERO,
                ZERO
        );

        assertEquals(
                AdaptiveCruise.Mode.EMERGENCY_BRAKE,
                decision.mode()
        );
        assertEquals(
                EMERGENCY_BRAKE_PRESSURE,
                decision.amount(),
                DELTA
        );
    }


    @Test
    @DisplayName("Zero speed and positive distance should allow acceleration when below target")
    void update_whenSpeedIsZeroAndDistanceIsPositive_shouldAccelerate() {
        AdaptiveCruise controller = new AdaptiveCruise(
                REPRESENTATIVE_TARGET_SPEED_KMH
        );

        AdaptiveCruise.Decision decision = controller.update(
                ZERO,
                1.0
        );

        assertEquals(
                AdaptiveCruise.Mode.ACCELERATE,
                decision.mode()
        );
        assertEquals(
                MAX_ACCELERATION_STEP_KMH,
                decision.amount(),
                DELTA
        );
    }


    @Test
    @DisplayName("Largest finite distance should be accepted")
    void update_whenDistanceIsLargestFiniteValue_shouldAcceptDistance() {
        AdaptiveCruise controller = new AdaptiveCruise(
                REPRESENTATIVE_TARGET_SPEED_KMH
        );

        AdaptiveCruise.Decision decision = controller.update(
                REPRESENTATIVE_SPEED_KMH,
                Double.MAX_VALUE
        );

        assertEquals(
                AdaptiveCruise.Mode.ACCELERATE,
                decision.mode()
        );
        assertEquals(
                MAX_ACCELERATION_STEP_KMH,
                decision.amount(),
                DELTA
        );
    }


    @Test
    @DisplayName("Positive speed with zero target speed should not accelerate")
    void update_whenTargetSpeedIsZeroAndCurrentSpeedIsPositive_shouldHoldSpeed() {
        AdaptiveCruise controller = new AdaptiveCruise(ZERO);

        AdaptiveCruise.Decision decision = controller.update(
                10.0,
                6.0
        );

        assertEquals(AdaptiveCruise.Mode.HOLD, decision.mode());
        assertEquals(ZERO, decision.amount(), DELTA);
    }


    @Test
    @DisplayName("Distance just below emergency threshold should trigger emergency braking")
    void update_whenDistanceIsJustBelowEmergencyThreshold_shouldEmergencyBrake() {
        double speedKmh = 100.0;
        double emergencyDistance =
                speedKmh * METERS_PER_KMH * EMERGENCY_DISTANCE_RATIO;

        AdaptiveCruise.Decision decision = adaptiveCruise.update(
                speedKmh,
                Math.nextDown(emergencyDistance)
        );

        assertEquals(AdaptiveCruise.Mode.EMERGENCY_BRAKE, decision.mode());
        assertEquals(EMERGENCY_BRAKE_PRESSURE, decision.amount(), DELTA);
    }

    @AfterEach
    void tearDown() {
        System.out.println(
                "---- Finished an AdaptiveCruise test ----"
        );
    }

    @AfterAll
    static void afterAll() {
        System.out.println(
                "========== Finished all AdaptiveCruise Tests =========="
        );
    }

}
