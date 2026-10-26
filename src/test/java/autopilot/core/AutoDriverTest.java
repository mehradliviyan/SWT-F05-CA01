//package autopilot.core;
//
//import autopilot.exception.SensorFailureException;
//import autopilot.sensor.LaneSensor;
//import autopilot.sensor.ObstacleSensor;
//import autopilot.sensor.SpeedSensor;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class AutoDriverTest {
//
//    private SpeedSensor speedSensor;
//    private ObstacleSensor obstacleSensor;
//    private LaneSensor laneSensor;
//    private AutoDriver autoDriver;
//
//    @BeforeEach
//    void setUp() {
//        speedSensor = new SpeedSensor() {
//            @Override
//            public double getSpeedKmh() {
//                return 80.0;
//            }
//        };
//
//        obstacleSensor = new ObstacleSensor() {
//            @Override
//            public double getDistanceMeters() {
//                return 100.0;
//            }
//        };
//
//        laneSensor = new LaneSensor() {
//            @Override
//            public double getLaneOffsetMeters() {
//                return 0.0;
//            }
//        };
//
//        autoDriver = new AutoDriver(
//                speedSensor,
//                obstacleSensor,
//                laneSensor,
//                80.0
//        );
//    }
//
//    @Test
//    void test1() {
//        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);
//
//        assertNotNull(result);
//        assertFalse(result.safeMode());
//        assertNotNull(result.cruiseMode());
//        assertEquals(0.0, result.brakePressure());
//    }
//
//    @Test
//    void testLanePositive() {
//        laneSensor = new LaneSensor() {
//            @Override
//            public double getLaneOffsetMeters() {
//                return 1.0;
//            }
//        };
//
//        autoDriver = new AutoDriver(
//                speedSensor, obstacleSensor, laneSensor, 80.0
//        );
//
//        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);
//
//        assertTrue(result.steeringAngleDegrees() < 0.0);
//    }
//
//    @Test
//    void testLaneNegative() {
//        laneSensor = new LaneSensor() {
//            @Override
//            public double getLaneOffsetMeters() {
//                return -1.0;
//            }
//        };
//
//        autoDriver = new AutoDriver(
//                speedSensor, obstacleSensor, laneSensor, 80.0
//        );
//
//        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);
//
//        assertTrue(result.steeringAngleDegrees() > 0.0);
//    }
//
//    @Test
//    void testSpeedSensorFailure() {
//        speedSensor = new SpeedSensor() {
//            @Override
//            public double getSpeedKmh() {
//                throw new SensorFailureException("Speed sensor failed");
//            }
//        };
//
//        autoDriver = new AutoDriver(
//                speedSensor, obstacleSensor, laneSensor, 80.0
//        );
//
//        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);
//
//        assertTrue(result.safeMode());
//        assertEquals(30.0, result.brakePressure());
//        assertEquals(0.0, result.steeringAngleDegrees());
//        assertEquals(AdaptiveCruise.Mode.BRAKE, result.cruiseMode());
//        assertFalse(result.warningActive());
//    }
//
//    @Test
//    void testObstacleSensorFailure() {
//        obstacleSensor = new ObstacleSensor() {
//            @Override
//            public double getDistanceMeters() {
//                throw new SensorFailureException("Obstacle sensor failed");
//            }
//        };
//
//        autoDriver = new AutoDriver(
//                speedSensor, obstacleSensor, laneSensor, 80.0
//        );
//
//        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);
//
//        assertTrue(result.safeMode());
//        assertEquals(30.0, result.brakePressure());
//        assertEquals(0.0, result.steeringAngleDegrees());
//        assertEquals(AdaptiveCruise.Mode.BRAKE, result.cruiseMode());
//        assertFalse(result.warningActive());
//    }
//}



package autopilot.core;

import autopilot.exception.SensorFailureException;
import autopilot.sensor.LaneSensor;
import autopilot.sensor.ObstacleSensor;
import autopilot.sensor.SpeedSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoDriver Unit Tests")
class AutoDriverTest {

    private static final double DELTA = 0.000001;

    private static final double ZERO = 0.0;
    private static final double DEFAULT_SPEED_KMH = 80.0;
    private static final double DEFAULT_DISTANCE_METERS = 100.0;
    private static final double DEFAULT_TARGET_SPEED_KMH = 80.0;
    private static final double SAFE_MODE_BRAKE_PRESSURE = 30.0;

    private MutableSpeedSensor speedSensor;
    private MutableObstacleSensor obstacleSensor;
    private MutableLaneSensor laneSensor;
    private AutoDriver autoDriver;

    @BeforeEach
    void setUp() {
        // Create fresh, controllable test doubles for every test.
        speedSensor = new MutableSpeedSensor(DEFAULT_SPEED_KMH);
        obstacleSensor = new MutableObstacleSensor(DEFAULT_DISTANCE_METERS);
        laneSensor = new MutableLaneSensor(ZERO);

        autoDriver = new AutoDriver(
                speedSensor,
                obstacleSensor,
                laneSensor,
                DEFAULT_TARGET_SPEED_KMH
        );
    }

    // Replaces test1().
    // Assert the complete expected outcome of normal driving,
    // rather than checking only that the result is non-null.
    @Test
    @DisplayName("Normal driving should hold speed without braking or entering safe mode")
    void tick_whenDrivingNormally_shouldHoldSpeedWithoutBraking() {
        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertAll(
                () -> assertFalse(result.safeMode()),
                () -> assertEquals(AdaptiveCruise.Mode.HOLD, result.cruiseMode()),
                () -> assertEquals(ZERO, result.brakePressure(), DELTA),
                () -> assertEquals(ZERO, result.steeringAngleDegrees(), DELTA),
                () -> assertFalse(result.warningActive())
        );
    }

    // Replaces testLanePositive() and testLaneNegative().
    // Both scenarios exercise the same behavior with opposite input signs,
    // so a parameterized test avoids duplicated setup and assertions.
    @ParameterizedTest(name = "Lane offset {0} m should produce steering with sign {1}")
    @CsvSource({
            "1.0, -1.0",
            "-1.0, 1.0"
    })
    @DisplayName("Steering correction should oppose the lane deviation")
    void tick_whenLaneDeviationExceedsThreshold_shouldCorrectSteeringDirection(
            double laneOffsetMeters,
            double expectedAngleSign
    ) {
        laneSensor.setLaneOffsetMeters(laneOffsetMeters);

        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertTrue(
                result.steeringAngleDegrees() * expectedAngleSign > ZERO,
                "Steering angle should have the expected sign"
        );
        assertFalse(result.safeMode());
    }

    // Covers the ordinary proportional-braking path in AutoDriver.
    // Check the selected cruise mode and positive brake pressure without
    // assuming undocumented BrakeController pressure calculations.
    @Test
    @DisplayName("An obstacle inside safe distance should activate proportional braking")
    void tick_whenObstacleIsInsideSafeDistance_shouldApplyNormalBraking() {
        obstacleSensor.setDistanceMeters(30.0);

        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertAll(
                () -> assertFalse(result.safeMode()),
                () -> assertEquals(AdaptiveCruise.Mode.BRAKE, result.cruiseMode()),
                () -> assertTrue(
                        result.brakePressure() > ZERO,
                        "Normal braking should produce positive brake pressure"
                )
        );
    }

    // Covers the emergency-braking branch of applyCruiseDecision().
    @Test
    @DisplayName("An obstacle inside the emergency threshold should trigger emergency braking")
    void tick_whenObstacleIsAtEmergencyDistance_shouldApplyEmergencyBraking() {
        obstacleSensor.setDistanceMeters(10.0);

        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertAll(
                () -> assertFalse(result.safeMode()),
                () -> assertEquals(
                        AdaptiveCruise.Mode.EMERGENCY_BRAKE,
                        result.cruiseMode()
                ),
                () -> assertTrue(
                        result.brakePressure() > ZERO,
                        "Emergency braking should produce positive brake pressure"
                )
        );
    }

    // Replaces testSpeedSensorFailure() and testObstacleSensorFailure(),
    // and adds the previously missing LaneSensor failure scenario.
    // Each parameterized invocation receives fresh sensors through setUp().
    @ParameterizedTest(name = "Failure of {0} should activate safe mode")
    @EnumSource(SensorType.class)
    @DisplayName("Failure of any sensor should activate safe mode")
    void tick_whenAnySensorFails_shouldEnterSafeMode(SensorType sensorType) {
        switch (sensorType) {
            case SPEED -> speedSensor.setFailure(true);
            case OBSTACLE -> obstacleSensor.setFailure(true);
            case LANE -> laneSensor.setFailure(true);
        }

        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertAll(
                () -> assertTrue(result.safeMode()),
                () -> assertEquals(
                        SAFE_MODE_BRAKE_PRESSURE,
                        result.brakePressure(),
                        DELTA
                ),
                () -> assertEquals(
                        ZERO,
                        result.steeringAngleDegrees(),
                        DELTA
                ),
                () -> assertEquals(
                        AdaptiveCruise.Mode.BRAKE,
                        result.cruiseMode()
                ),
                () -> assertFalse(result.warningActive())
        );
    }

    private enum SensorType {
        SPEED,
        OBSTACLE,
        LANE
    }

    // Mutable test doubles let each test control sensor output and failure
    // behavior without replacing the sensor or reconstructing AutoDriver.

    private static final class MutableSpeedSensor implements SpeedSensor {
        private double speedKmh;
        private boolean failure;

        private MutableSpeedSensor(double speedKmh) {
            this.speedKmh = speedKmh;
        }

        @Override
        public double getSpeedKmh() {
            if (failure) {
                throw new SensorFailureException("Speed sensor failed");
            }
            return speedKmh;
        }

        private void setSpeedKmh(double speedKmh) {
            this.speedKmh = speedKmh;
        }

        private void setFailure(boolean failure) {
            this.failure = failure;
        }
    }

    private static final class MutableObstacleSensor implements ObstacleSensor {
        private double distanceMeters;
        private boolean failure;

        private MutableObstacleSensor(double distanceMeters) {
            this.distanceMeters = distanceMeters;
        }

        @Override
        public double getDistanceMeters() {
            if (failure) {
                throw new SensorFailureException("Obstacle sensor failed");
            }
            return distanceMeters;
        }

        private void setDistanceMeters(double distanceMeters) {
            this.distanceMeters = distanceMeters;
        }

        private void setFailure(boolean failure) {
            this.failure = failure;
        }
    }

    private static final class MutableLaneSensor implements LaneSensor {
        private double laneOffsetMeters;
        private boolean failure;

        private MutableLaneSensor(double laneOffsetMeters) {
            this.laneOffsetMeters = laneOffsetMeters;
        }

        @Override
        public double getLaneOffsetMeters() {
            if (failure) {
                throw new SensorFailureException("Lane sensor failed");
            }
            return laneOffsetMeters;
        }

        private void setLaneOffsetMeters(double laneOffsetMeters) {
            this.laneOffsetMeters = laneOffsetMeters;
        }

        private void setFailure(boolean failure) {
            this.failure = failure;
        }
    }
}
