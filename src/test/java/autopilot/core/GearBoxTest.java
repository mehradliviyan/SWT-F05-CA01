
package autopilot.core;

import autopilot.exception.IllegalGearChangeException;
import autopilot.sensor.SpeedSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GearBox Unit Tests")
class GearBoxTest {

    private MutableSpeedSensor speedSensor;
    private GearBox gearBox;

    @BeforeAll
    static void beforeAll() {
        System.out.println("Setup All GearBox Tests");
    }

    @BeforeEach
    void setUp() {
        System.out.println("Setup new GearBox Test");
        speedSensor = new MutableSpeedSensor(0.0);
        gearBox = new GearBox(speedSensor);
    }

    @Test
    @DisplayName("A newly created gearbox starts in Park")
    void initialState_newGearBox_startsInPark() {
        assertEquals(
                GearBox.Gear.P,
                gearBox.getCurrentGear(),
                "A new gearbox should start in Park"
        );
    }


    @Test
    @DisplayName("Changing to a null gear throws NullPointerException")
    void changeTo_nullGear_throwsNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () -> gearBox.changeTo(null)
        );
    }


    @Test
    @DisplayName("Requesting the current gear keeps the gear unchanged")
    void changeTo_currentGear_keepsCurrentGear() {
        gearBox.changeTo(GearBox.Gear.P);

        assertEquals(
                GearBox.Gear.P,
                gearBox.getCurrentGear(),
                "The current gear should remain Park"
        );
    }

    @Test
    @DisplayName("Requesting the current gear does not read the speed sensor")
    void changeTo_currentGear_doesNotReadSpeedSensor() {
        gearBox.changeTo(GearBox.Gear.P);

        assertEquals(
                0,
                speedSensor.getReadCount(),
                "The speed sensor should not be read when the requested gear is already selected"
        );
    }

    @ParameterizedTest(name = "Speed {0} throws IllegalArgumentException")
    @MethodSource("invalidSpeeds")
    @DisplayName("Invalid speeds are rejected")
    void changeTo_invalidSpeed_throwsIllegalArgumentException(double speedKmh) {
        speedSensor.setSpeed(speedKmh);

        assertThrows(
                IllegalArgumentException.class,
                () -> gearBox.changeTo(GearBox.Gear.R)
        );
        assertEquals(
                GearBox.Gear.P,
                gearBox.getCurrentGear(),
                "The gearbox should remain in Park when the speed is invalid"
        );
    }

    private static Stream<Double> invalidSpeeds() {
        return Stream.of(
                -1.0,
                -100.0,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        );
    }


    @ParameterizedTest(name = "Changing from P to R at {0} km/h succeeds")
    @ValueSource(doubles = {0.0, 0.5, 1.0, 1.6, 2.0, 2.2, 4.999})
    @DisplayName("Park to Reverse is allowed below the speed limit")
    void changeTo_reverseBelowSpeedLimit_changesGearToReverse(double speedKmh) {
        speedSensor.setSpeed(speedKmh);

        gearBox.changeTo(GearBox.Gear.R);

        assertEquals(
                GearBox.Gear.R,
                gearBox.getCurrentGear(),
                "The current gear should be Reverse"
        );
    }



    @ParameterizedTest(name = "Changing from P to R at {0} km/h throws an exception")
    @ValueSource(doubles = {5.0, 5.1, 10.0})
    @DisplayName("Park to Reverse is rejected at or above the speed limit")
    void changeTo_reverseAtOrAboveSpeedLimit_throwsIllegalGearChangeException(double speedKmh) {
        speedSensor.setSpeed(speedKmh);

        assertThrows(
                IllegalGearChangeException.class,
                () -> gearBox.changeTo(GearBox.Gear.R)
        );
    }



    @ParameterizedTest(name = "Gear remains P when changing to R at {0} km/h")
    @ValueSource(doubles = {5.0, 5.1, 10.0})
    @DisplayName("A rejected Park to Reverse change preserves the current gear")
    void changeTo_reverseAtOrAboveSpeedLimit_keepsCurrentGear(double speedKmh) {
        speedSensor.setSpeed(speedKmh);

        assertThrows(
                IllegalGearChangeException.class,
                () -> gearBox.changeTo(GearBox.Gear.R)
        );

        assertEquals(
                GearBox.Gear.P,
                gearBox.getCurrentGear(),
                "The gearbox should remain in Park after a rejected gear change"
        );
    }

    @Test
    @DisplayName("Reverse to Drive is allowed at zero speed")
    void changeTo_driveFromReverseAtZeroSpeed_changesGearToDrive() {
        gearBox.changeTo(GearBox.Gear.R);

        speedSensor.setSpeed(0);

        gearBox.changeTo(GearBox.Gear.D);

        assertEquals(
                GearBox.Gear.D,
                gearBox.getCurrentGear(),
                "The current gear should be Drive"
        );
    }


    @ParameterizedTest(name = "Changing from R to D at {0} km/h is rejected")
    @CsvSource({
            "0.1",
            "1.0",
            "5.0",
            "10.0"
    })
    @DisplayName("Reverse to Drive is rejected when the vehicle is moving")
    void changeTo_driveFromReverseAtPositiveSpeed_throwsIllegalGearChangeException(double speedKmh) {
        gearBox.changeTo(GearBox.Gear.R);

        speedSensor.setSpeed(speedKmh);

        assertThrows(
                IllegalGearChangeException.class,
                () -> gearBox.changeTo(GearBox.Gear.D)
        );

        assertEquals(
                GearBox.Gear.R,
                gearBox.getCurrentGear(),
                "The current gear should remain Reverse after a rejected gear change"
        );
    }




    @Test
    @DisplayName("Drive to Reverse is allowed at zero speed")
    void changeTo_reverseFromDriveAtZeroSpeed_changesGearToReverse() {
        gearBox.changeTo(GearBox.Gear.R);
        gearBox.changeTo(GearBox.Gear.D);

        speedSensor.setSpeed(0.0);

        gearBox.changeTo(GearBox.Gear.R);

        assertEquals(
                GearBox.Gear.R,
                gearBox.getCurrentGear(),
                "The current gear should be Reverse"
        );
    }



    @ParameterizedTest(name = "Changing from D to R at {0} km/h is rejected")
    @CsvSource({
            "0.1",
            "1.0",
            "5.0",
            "10.0"
    })
    @DisplayName("Drive to Reverse is rejected when the vehicle is moving")
    void changeTo_reverseFromDriveAtPositiveSpeed_throwsIllegalGearChangeException(double speedKmh) {
        gearBox.changeTo(GearBox.Gear.R);
        gearBox.changeTo(GearBox.Gear.D);

        speedSensor.setSpeed(speedKmh);

        assertThrows(
                IllegalGearChangeException.class,
                () -> gearBox.changeTo(GearBox.Gear.R)
        );

        assertEquals(
                GearBox.Gear.D,
                gearBox.getCurrentGear(),
                "The current gear should remain Drive after a rejected gear change"
        );
    }




    @ParameterizedTest(name = "Changing from R to N at {0} km/h succeeds")@CsvFileSource(
            resources = "/autopilot/core/reverse-to-neutral-speeds.csv"
    )
    @DisplayName("Reverse to Neutral is allowed at different valid speeds")
    void changeTo_neutralFromReverseAtValidSpeeds_changesGearToNeutral(double speedKmh) {
        gearBox.changeTo(GearBox.Gear.R);

        speedSensor.setSpeed(speedKmh);

        gearBox.changeTo(GearBox.Gear.N);

        assertEquals(
                GearBox.Gear.N,
                gearBox.getCurrentGear(),
                "The current gear should be Neutral"
        );
    }




    @ParameterizedTest(name = "Changing from N to D at {0} km/h succeeds")
    @CsvSource({
            "0.0",
            "0.1",
            "5.0",
            "20.0",
            "100.0"
    })
    @DisplayName("Neutral to Drive is allowed at different valid speeds")
    void changeTo_driveFromNeutralAtValidSpeeds_changesGearToDrive(double speedKmh) {
        gearBox.changeTo(GearBox.Gear.R);
        gearBox.changeTo(GearBox.Gear.N);

        speedSensor.setSpeed(speedKmh);

        gearBox.changeTo(GearBox.Gear.D);

        assertEquals(
                GearBox.Gear.D,
                gearBox.getCurrentGear(),
                "The current gear should be Drive"
        );
    }


    @ParameterizedTest(name = "Changing from P to {0} is rejected")
    @EnumSource(
            value = GearBox.Gear.class,
            names = {"N", "D"}
    )
    @DisplayName("Park cannot change directly to Neutral or Drive")
    void changeTo_invalidGearFromPark_throwsIllegalGearChangeException(
            GearBox.Gear targetGear
    ) {
        assertThrows(
                IllegalGearChangeException.class,
                () -> gearBox.changeTo(targetGear)
        );

        assertEquals(
                GearBox.Gear.P,
                gearBox.getCurrentGear(),
                "The current gear should remain Park"
        );
    }

    @ParameterizedTest(name = "Changing from R to {0} is rejected")
    @EnumSource(value = GearBox.Gear.class, names = {"P"})
    @DisplayName("Reverse cannot change directly to Park")
    void changeTo_parkFromReverse_throwsIllegalGearChangeException(
            GearBox.Gear targetGear
    ) {
        gearBox.changeTo(GearBox.Gear.R);

        assertThrows(
                IllegalGearChangeException.class,
                () -> gearBox.changeTo(targetGear)
        );

        assertEquals(
                GearBox.Gear.R,
                gearBox.getCurrentGear(),
                "The current gear should remain Reverse"
        );
    }

    @ParameterizedTest(name = "Changing from D to {0} is rejected")
    @EnumSource(
            value = GearBox.Gear.class,
            names = {"P", "N"}
    )
    @DisplayName("Drive cannot change directly to Park or Neutral")
    void changeTo_invalidGearFromDrive_throwsIllegalGearChangeException(
            GearBox.Gear targetGear
    ) {
        gearBox.changeTo(GearBox.Gear.R);
        gearBox.changeTo(GearBox.Gear.D);

        assertThrows(
                IllegalGearChangeException.class,
                () -> gearBox.changeTo(targetGear)
        );

        assertEquals(
                GearBox.Gear.D,
                gearBox.getCurrentGear(),
                "The current gear should remain Drive"
        );
    }

    @ParameterizedTest(name = "Changing from N to {0} is rejected")
    @EnumSource(
            value = GearBox.Gear.class,
            names = {"P", "R"}
    )
    @DisplayName("Neutral cannot change directly to Park or Reverse")
    void changeTo_invalidGearFromNeutral_throwsIllegalGearChangeException(
            GearBox.Gear targetGear
    ) {
        gearBox.changeTo(GearBox.Gear.R);
        gearBox.changeTo(GearBox.Gear.N);

        assertThrows(
                IllegalGearChangeException.class,
                () -> gearBox.changeTo(targetGear)
        );

        assertEquals(
                GearBox.Gear.N,
                gearBox.getCurrentGear(),
                "The current gear should remain Neutral"
        );
    }



    @ParameterizedTest(name = "Requesting the current gear {0} does not read the speed sensor")
    @EnumSource(GearBox.Gear.class)
    @DisplayName("Requesting any current gear does not read the speed sensor")
    void changeTo_anyCurrentGear_doesNotReadSpeedSensor(GearBox.Gear targetGear) {
        switch (targetGear) {
            case P -> {
            }
            case R -> gearBox.changeTo(GearBox.Gear.R);
            case N -> {
                gearBox.changeTo(GearBox.Gear.R);
                gearBox.changeTo(GearBox.Gear.N);
            }
            case D -> {
                gearBox.changeTo(GearBox.Gear.R);
                gearBox.changeTo(GearBox.Gear.D);
            }
        }

        speedSensor.setSpeed(Double.NaN);
        speedSensor.resetReadCount();

        gearBox.changeTo(targetGear);

        assertEquals(
                0,
                speedSensor.getReadCount(),
                "The speed sensor should not be read when the requested gear is already selected"
        );

        assertEquals(
                targetGear,
                gearBox.getCurrentGear(),
                "The current gear should remain unchanged"
        );
    }



    @AfterEach
    void tearDown() {
        System.out.println("TearDown a GearBox Test");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("TearDown All GearBox Tests");
    }

    // ---------------------------------------------------------
    // Test helpers
    // ---------------------------------------------------------


    private static final class MutableSpeedSensor implements SpeedSensor {

        private double speedKmh;
        private int readCount;

        private MutableSpeedSensor(double initialSpeedKmh) {
            this.speedKmh = initialSpeedKmh;
        }

        @Override
        public double getSpeedKmh() {
            readCount++;
            return speedKmh;
        }

        private void setSpeed(double speedKmh) {
            this.speedKmh = speedKmh;
        }

        private int getReadCount() {
            return readCount;
        }

        private void resetReadCount() {
            readCount = 0;
        }
    }
}


