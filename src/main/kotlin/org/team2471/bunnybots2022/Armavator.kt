package org.team2471.bunnybots2022

    import edu.wpi.first.networktables.NetworkTableInstance
    import edu.wpi.first.wpilibj.DutyCycleEncoder
    import edu.wpi.first.wpilibj.Servo

    import org.team2471.frc.lib.actuators.FalconID
    import org.team2471.frc.lib.actuators.MotorController
    import org.team2471.frc.lib.actuators.SparkMaxID
    import org.team2471.frc.lib.actuators.TalonID
    import org.team2471.frc.lib.framework.Subsystem


object Armavator : Subsystem("Armavator") {
        //motors
        val suckMotor = MotorController(SparkMaxID(Sparks.INTAKE_SUCK))
        val spitMotor = MotorController(SparkMaxID(Sparks.INTAKE_SPIT))
        val intakePivotMotor = Servo(PWMServos.INTAKE_PIVOT)
        val armMotor = MotorController(FalconID(Falcons.ARM))
        val elevatorMotor = MotorController(FalconID(Falcons.ELEVATOR))

        //sensors
        val armAngleEncoder = DutyCycleEncoder(AnalogSensors.ARM_ANGLE)
        val elevatorEncoder = DutyCycleEncoder(DigitalSensors.INTAKE_ELEVATOR)

        //data table
        private val table = NetworkTableInstance.getDefault().getTable(Armavator.name)
        val currentEntry = table.getEntry("Current")
        val armAngleEntry = table.getEntry("Arm Angle")


        init {
            armMotor.config(20) {
                // this was from lil bois bench test of swerve
                feedbackCoefficient = 9.0 * 360.0 / 2048.0 / 2048.0  // ~111 ticks per degree // spark max-neo 360.0 / 42.0 / 19.6 // degrees per tick
                //setRawOffsetConfig(absoluteAngle)
                currentLimit(15,20,1)
//                inverted(false)
//                setSensorPhase(true)
//                pid {
//                    p(0.000002)
////                    d(0.0000025)
//                }
////                burnSettings()
            }
        }
    override suspend fun default(){
        println("Arm_Angle ${armAngleEncoder.absolutePosition}")
    }
}