
package autopilot.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BrakeController Unit Tests")
class BrakeControllerTest {

    private BrakeController brakeController;

    private static final double MAX_BRAKE_PRESSURE = 100.0;
    private static final double INITIAL_BRAKE_PRESSURE = 35.0;
    private static final double ABS_HIGH_PULSE = 90.0;
    private static final double ABS_LOW_PULSE = 60.0;
    @BeforeAll
    static void beforeAll() {
        System.out.println("========== Starting BrakeController Tests ==========");
    }

    @BeforeEach
    void setUp() {
        System.out.println("---- Setting up a new BrakeController test ----");

        brakeController = new BrakeController();
    }



    // ---------------------------------------------------------
    // Initial state
    // ---------------------------------------------------------

    @Test
    @DisplayName("Initial pressure should be zero")
    void initialState_shouldHaveZeroPressure() {
        assertEquals(0.0, brakeController.getPressure());
    }

    @Test
    @DisplayName("Initial warning should be inactive")
    void initialState_shouldHaveNoActiveWarning() {
        assertFalse(brakeController.isWarningActive());
    }


    // ---------------------------------------------------------
    // Pressure validation
    // ---------------------------------------------------------

    @ParameterizedTest(name = "invalid pressure={0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Should reject invalid requested pressure")
    void apply_whenPressureIsInvalid_shouldThrowIllegalArgumentException(
            double requestedPressure) {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> brakeController.apply(
                        requestedPressure, 0.0, GearBox.Gear.D)
        );

        assertEquals(
                "pressure must be a finite non-negative value",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "invalid pressure={0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Invalid pressure should not change stored pressure")
    void apply_whenPressureIsInvalid_shouldPreservePreviousPressure(
            double requestedPressure) {

        brakeController.apply(20.0, 0.0, GearBox.Gear.D);

        assertThrows(
                IllegalArgumentException.class,
                () -> brakeController.apply(
                        requestedPressure, 0.0, GearBox.Gear.D)
        );

        assertEquals(20.0, brakeController.getPressure());
    }

    // ---------------------------------------------------------
    // Speed validation
    // ---------------------------------------------------------

    @ParameterizedTest(name = "invalid speed={0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Should reject invalid speed")
    void apply_whenSpeedIsInvalid_shouldThrowIllegalArgumentException(
            double speedKmh) {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> brakeController.apply(20.0, speedKmh, GearBox.Gear.D)
        );

        assertEquals(
                "speed must be a finite non-negative value",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "invalid speed={0}")
    @ValueSource(doubles = {
            -1.0,
            -Double.MIN_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Invalid speed should not change stored pressure")
    void apply_whenSpeedIsInvalid_shouldPreservePreviousPressure(
            double speedKmh) {

        brakeController.apply(20.0, 0.0, GearBox.Gear.D);

        assertThrows(
                IllegalArgumentException.class,
                () -> brakeController.apply(40.0, speedKmh, GearBox.Gear.D)
        );

        assertEquals(20.0, brakeController.getPressure());
    }


    // ---------------------------------------------------------
    // Gear validation
    // ---------------------------------------------------------

    @ParameterizedTest
    @EnumSource(
            value = GearBox.Gear.class,
            names = "P",
            mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Should allow braking in every non-park gear")
    void apply_whenGearIsNotPark_shouldAllowBraking(GearBox.Gear gear) {
        assertEquals(
                25.0,
                brakeController.apply(25.0, 0.0, gear)
        );
    }

    @Test
    @DisplayName("Should reject braking in park")
    void apply_whenGearIsPark_shouldThrowIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> brakeController.apply(25.0, 0.0, GearBox.Gear.P)
        );

        assertEquals(
                "braking is unavailable while the gearbox is in park",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Should reject a null gear")
    void apply_whenGearIsNull_shouldThrowNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () -> brakeController.apply(25.0, 0.0, null)
        );
    }

    @Test
    @DisplayName("Park gear should not change previously stored pressure")
    void apply_whenGearIsPark_shouldPreservePreviousPressure() {
        brakeController.apply(20.0, 0.0, GearBox.Gear.D);

        assertThrows(
                IllegalStateException.class,
                () -> brakeController.apply(40.0, 0.0, GearBox.Gear.P)
        );

        assertEquals(20.0, brakeController.getPressure());
    }

    @Test
    @DisplayName("Null gear should not change previously stored pressure")
    void apply_whenGearIsNull_shouldPreservePreviousPressure() {
        brakeController.apply(20.0, 0.0, GearBox.Gear.D);

        assertThrows(
                NullPointerException.class,
                () -> brakeController.apply(40.0, 0.0, null)
        );

        assertEquals(20.0, brakeController.getPressure());
    }





    // ---------------------------------------------------------
    // Validation order
    // ---------------------------------------------------------

    @Test
    @DisplayName("Should validate pressure before speed and gear")
    void apply_whenPressureSpeedAndGearAreInvalid_shouldValidatePressureFirst() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> brakeController.apply(
                        -1.0, Double.NaN, GearBox.Gear.P)
        );

        assertEquals(
                "pressure must be a finite non-negative value",
                exception.getMessage()
        );
    }



    @Test
    @DisplayName("Should validate speed before gear")
    void apply_whenSpeedAndGearAreInvalid_shouldValidateSpeedFirst() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> brakeController.apply(
                        20.0, Double.NaN, GearBox.Gear.P)
        );

        assertEquals(
                "speed must be a finite non-negative value",
                exception.getMessage()
        );
    }


    // ---------------------------------------------------------
    // Valid inputs and normal pressure application
    // ---------------------------------------------------------

    @ParameterizedTest(name = "pressure={0}, speed={1}, expected={2}")
    @CsvSource({
            "0.0,   0.0,  0.0",
            "10.0,  0.0, 10.0",
            "25.0, 20.0, 25.0",
            "50.0, 30.0, 50.0",
            "80.0, 40.0, 80.0",
            "100.0, 30.0, 100.0"
    })
    @DisplayName("Should return requested pressure when ABS is inactive")
    void apply_whenAbsIsInactive_shouldReturnBoundedPressure(
            double requestedPressure,
            double speedKmh,
            double expectedPressure) {

        assertEquals(
                expectedPressure,
                brakeController.apply(
                        requestedPressure, speedKmh, GearBox.Gear.D)
        );
    }

    @ParameterizedTest(name = "requested pressure={0}")
    @ValueSource(doubles = {
            100.0001,
            110.0,
            150.0,
            200.0,
            1000.0,
            Double.MAX_VALUE
    })
    @DisplayName("Should cap pressure at maximum when ABS is inactive")
    void apply_whenPressureExceedsMaximumAndAbsIsInactive_shouldReturnMaximumPressure(
            double requestedPressure) {

        assertEquals(
                MAX_BRAKE_PRESSURE,
                brakeController.apply(requestedPressure, 0.0, GearBox.Gear.D)
        );
    }

    @Test
    @DisplayName("Should store the pressure returned by apply")
    void apply_whenCalled_shouldStoreAppliedPressure() {
        brakeController.apply(25.0, 0.0, GearBox.Gear.D);

        assertEquals(25.0, brakeController.getPressure());
    }

    @Test
    @DisplayName("Should not activate warning during normal application")
    void apply_whenCalledNormally_shouldKeepWarningInactive() {
        brakeController.apply(25.0, 0.0, GearBox.Gear.D);

        assertFalse(brakeController.isWarningActive());
    }



    // ---------------------------------------------------------
    // ABS activation boundaries
    // ---------------------------------------------------------

    @ParameterizedTest(name = "pressure={0}, speed={1}, expected={2}")
    @CsvSource({
            "80.0,  31.0,  80.0",
            "80.1,  30.0,  80.1",
            "80.1,  29.9,  80.1",
            "80.0,  30.0,  80.0",
            "150.0, 30.0, 100.0"
    })
    @DisplayName("Should not activate ABS unless both thresholds are exceeded")
    void apply_whenAbsThresholdsAreNotBothExceeded_shouldReturnBoundedPressure(
            double requestedPressure,
            double speedKmh,
            double expectedPressure) {

        assertEquals(
                expectedPressure,
                brakeController.apply(
                        requestedPressure, speedKmh, GearBox.Gear.D)
        );
    }

    @ParameterizedTest(name = "pressure={0}, speed={1}")
    @CsvSource({
            "80.1,  30.1",
            "90.0,  31.0",
            "100.0, 50.0",
            "150.0, 31.0"
    })
    @DisplayName("Should apply the high ABS pulse when both thresholds are exceeded")
    void apply_whenAbsConditionsAreMet_shouldReturnHighPulseFirst(
            double requestedPressure,
            double speedKmh) {


        assertEquals(
                ABS_HIGH_PULSE,
                brakeController.apply(
                        requestedPressure, speedKmh, GearBox.Gear.D)
        );
    }

    @Test
    @DisplayName("Should activate ABS at the smallest representable pressure above threshold")
    void apply_whenPressureIsImmediatelyAboveAbsThreshold_shouldActivateAbs() {


        assertEquals(
                ABS_HIGH_PULSE,
                brakeController.apply(
                        Math.nextUp(80.0),
                        Math.nextUp(30.0),
                        GearBox.Gear.D)
        );
    }

    @Test
    @DisplayName("Should accept the largest finite speed")
    void apply_whenSpeedIsLargestFiniteValue_shouldApplyAbs() {

        assertEquals(
                ABS_HIGH_PULSE,
                brakeController.apply(
                        90.0, Double.MAX_VALUE, GearBox.Gear.D)
        );
    }

    // ---------------------------------------------------------
    // ABS pulse sequence
    // ---------------------------------------------------------

    @Test
    @DisplayName("Should alternate ABS pulses between high and low")
    void apply_whenAbsRemainsActive_shouldAlternateHighAndLowPulses() {
        List<Double> actualPulses = Stream.generate(
                        () -> brakeController.apply(
                                90.0, 50.0, GearBox.Gear.D
                        ))
                .limit(4)
                .toList();

        assertEquals(List.of(ABS_HIGH_PULSE, ABS_LOW_PULSE, ABS_HIGH_PULSE, ABS_LOW_PULSE), actualPulses);
    }



    @ParameterizedTest(name = "inactive pressure={0}, speed={1}")
    @MethodSource("absInactiveInputs")
    @DisplayName("An inactive ABS call should reset the next pulse to high")
    void apply_whenAbsBecomesInactive_shouldResetPulseSequence(
            double inactivePressure,
            double inactiveSpeed) {

        brakeController.apply(90.0, 50.0, GearBox.Gear.D);

        brakeController.apply(
                inactivePressure, inactiveSpeed, GearBox.Gear.D);

        assertEquals(
                ABS_HIGH_PULSE,
                brakeController.apply(90.0, 50.0, GearBox.Gear.D)
        );
    }

    static Stream<Arguments> absInactiveInputs() {
        return Stream.of(
                Arguments.of(80.0, 50.0),
                Arguments.of(90.0, 30.0),
                Arguments.of(50.0, 50.0),
                Arguments.of(150.0, 30.0)
        );
    }

    @Test
    @DisplayName("Emergency braking should reset the next ABS pulse to high")
    void apply_afterEmergencyBrake_shouldStartWithHighAbsPulse() {
        brakeController.apply(90.0, 50.0, GearBox.Gear.D);

        brakeController.emergencyBrake(GearBox.Gear.D);

        assertEquals(
                ABS_HIGH_PULSE,
                brakeController.apply(90.0, 50.0, GearBox.Gear.D)
        );
    }

    // ---------------------------------------------------------
    // Invalid calls should not advance the ABS pulse sequence
    // ---------------------------------------------------------

    @ParameterizedTest(name = "invalid pressure={0}")
    @ValueSource(doubles = {
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Invalid pressure should not advance the ABS pulse sequence")
    void apply_whenPressureIsInvalidDuringAbs_shouldNotAdvancePulseSequence(
            double invalidPressure) {

        brakeController.apply(90.0, 50.0, GearBox.Gear.D);

        assertThrows(
                IllegalArgumentException.class,
                () -> brakeController.apply(
                        invalidPressure, 50.0, GearBox.Gear.D)
        );

        assertEquals(
                ABS_LOW_PULSE,
                brakeController.apply(90.0, 50.0, GearBox.Gear.D)
        );
    }

    @ParameterizedTest(name = "invalid speed={0}")
    @ValueSource(doubles = {
            -1.0,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    @DisplayName("Invalid speed should not advance the ABS pulse sequence")
    void apply_whenSpeedIsInvalidDuringAbs_shouldNotAdvancePulseSequence(
            double invalidSpeed) {

        brakeController.apply(90.0, 50.0, GearBox.Gear.D);

        assertThrows(
                IllegalArgumentException.class,
                () -> brakeController.apply(
                        90.0, invalidSpeed, GearBox.Gear.D)
        );

        assertEquals(
                ABS_LOW_PULSE,
                brakeController.apply(90.0, 50.0, GearBox.Gear.D)
        );
    }

    @Test
    @DisplayName("Park gear should not advance the ABS pulse sequence")
    void apply_whenGearIsParkDuringAbs_shouldNotAdvancePulseSequence() {
        brakeController.apply(90.0, 50.0, GearBox.Gear.D);

        assertThrows(
                IllegalStateException.class,
                () -> brakeController.apply(90.0, 50.0, GearBox.Gear.P)
        );

        assertEquals(
                ABS_LOW_PULSE,
                brakeController.apply(90.0, 50.0, GearBox.Gear.D)
        );
    }

    @Test
    @DisplayName("Null gear should not advance the ABS pulse sequence")
    void apply_whenGearIsNullDuringAbs_shouldNotAdvancePulseSequence() {
        brakeController.apply(90.0, 50.0, GearBox.Gear.D);

        assertThrows(
                NullPointerException.class,
                () -> brakeController.apply(90.0, 50.0, null)
        );

        assertEquals(
                ABS_LOW_PULSE,
                brakeController.apply(90.0, 50.0, GearBox.Gear.D)
        );
    }

    // ---------------------------------------------------------
    // Warning state
    // ---------------------------------------------------------

    @Test
    @DisplayName("Applying normal pressure should not clear an active warning")
    void apply_whenWarningIsAlreadyActive_shouldKeepWarningActive() {
        brakeController.emergencyBrake(GearBox.Gear.D);

        brakeController.apply(20.0, 0.0, GearBox.Gear.D);

        assertTrue(brakeController.isWarningActive());
    }


    // ==================== Emergency braking ====================



    @Test
    @DisplayName("Emergency braking should reject park gear")
    void emergencyBrake_whenGearIsPark_shouldThrowIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> brakeController.emergencyBrake(GearBox.Gear.P)
        );

        assertEquals(
                "braking is unavailable while the gearbox is in park",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Emergency braking should reject null gear")
    void emergencyBrake_whenGearIsNull_shouldThrowNullPointerException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> brakeController.emergencyBrake(null)
        );

        assertEquals("gear must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Failed emergency braking in park should preserve previous pressure")
    void emergencyBrake_whenGearIsPark_shouldPreservePreviousPressure() {
        brakeController.apply(INITIAL_BRAKE_PRESSURE, 0.0, GearBox.Gear.D);

        assertThrows(
                IllegalStateException.class,
                () -> brakeController.emergencyBrake(GearBox.Gear.P)
        );

        assertEquals(INITIAL_BRAKE_PRESSURE, brakeController.getPressure());
    }

    @Test
    @DisplayName("Failed emergency braking with null gear should preserve previous pressure")
    void emergencyBrake_whenGearIsNull_shouldPreservePreviousPressure() {
        brakeController.apply(INITIAL_BRAKE_PRESSURE, 0.0, GearBox.Gear.D);

        assertThrows(
                NullPointerException.class,
                () -> brakeController.emergencyBrake(null)
        );

        assertEquals(INITIAL_BRAKE_PRESSURE, brakeController.getPressure());
    }

    @Test
    @DisplayName("Failed emergency braking in park should not activate the warning")
    void emergencyBrake_whenGearIsPark_shouldKeepWarningInactive() {
        assertThrows(
                IllegalStateException.class,
                () -> brakeController.emergencyBrake(GearBox.Gear.P)
        );

        assertFalse(brakeController.isWarningActive());
    }

    @Test
    @DisplayName("Failed emergency braking should preserve an already active warning")
    void emergencyBrake_whenGearIsParkAndWarningIsActive_shouldKeepWarningActive() {
        brakeController.emergencyBrake(GearBox.Gear.D);

        assertThrows(
                IllegalStateException.class,
                () -> brakeController.emergencyBrake(GearBox.Gear.P)
        );

        assertTrue(brakeController.isWarningActive());
    }


    @Test
    @DisplayName("Failed emergency braking in null gear should not activate the warning")
    void emergencyBrake_whenGearIsNull_shouldKeepWarningInactive() {
        assertThrows(
                NullPointerException.class,
                () -> brakeController.emergencyBrake(null)
        );

        assertFalse(brakeController.isWarningActive());
    }

    @Test
    @DisplayName("Failed emergency braking in null gear should preserve an already active warning")
    void emergencyBrake_whenGearIsNullAndWarningIsActive_shouldKeepWarningActive() {
        brakeController.emergencyBrake(GearBox.Gear.D);

        assertThrows(
                NullPointerException.class,
                () -> brakeController.emergencyBrake(null)
        );

        assertTrue(brakeController.isWarningActive());
    }


    @ParameterizedTest
    @EnumSource(
            value = GearBox.Gear.class,
            names = "P",
            mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Emergency braking should set pressure to maximum in every non-park gear")
    void emergencyBrake_whenGearIsNotPark_shouldSetMaximumPressure(
            GearBox.Gear gear
    ) {

        brakeController.emergencyBrake(gear);


        assertEquals(MAX_BRAKE_PRESSURE, brakeController.getPressure());
    }

    @ParameterizedTest
    @EnumSource(
            value = GearBox.Gear.class,
            names = "P",
            mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Emergency braking should activate the warning")
    void emergencyBrake_whenGearIsNotPark_shouldActivateWarning(GearBox.Gear gear) {
        brakeController.emergencyBrake(gear);

        assertTrue(brakeController.isWarningActive());
    }

    @ParameterizedTest
    @EnumSource(
            value = GearBox.Gear.class,
            names = "P",
            mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Emergency braking should set pressure to maximum even when pressure is already non-zero")
    void emergencyBrake_whenPressureIsAlreadyApplied_shouldSetMaximumPressure(GearBox.Gear gear) {
        brakeController.apply(INITIAL_BRAKE_PRESSURE, 0.0, gear);

        brakeController.emergencyBrake(gear);

        assertEquals(MAX_BRAKE_PRESSURE, brakeController.getPressure());
    }



    @ParameterizedTest
    @EnumSource(
            value = GearBox.Gear.class,
            names = "P",
            mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Repeated emergency braking should keep pressure at maximum")
    void emergencyBrake_whenCalledRepeatedly_shouldKeepMaximumPressure(GearBox.Gear gear) {
        brakeController.emergencyBrake(gear);
        brakeController.emergencyBrake(gear);


        assertEquals(MAX_BRAKE_PRESSURE, brakeController.getPressure());
    }

    @ParameterizedTest
    @EnumSource(
            value = GearBox.Gear.class,
            names = "P",
            mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Repeated emergency braking should keep the warning active")
    void emergencyBrake_whenCalledRepeatedly_shouldKeepWarningActive(GearBox.Gear gear) {
        brakeController.emergencyBrake(gear);
        brakeController.emergencyBrake(gear);

        assertTrue(brakeController.isWarningActive());
    }


    @AfterEach
    void tearDown() {
        System.out.println("---- Finished a BrakeController test ----");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("========== Finished all BrakeController Tests ==========");
    }
}



