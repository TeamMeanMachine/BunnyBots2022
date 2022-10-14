package org.team2471.bunnybots2022

    import edu.wpi.first.networktables.NetworkTableInstance
    import edu.wpi.first.wpilibj.DutyCycleEncoder

    import org.team2471.frc.lib.actuators.FalconID
    import org.team2471.frc.lib.actuators.MotorController
    import org.team2471.frc.lib.actuators.SparkMaxID
    import org.team2471.frc.lib.actuators.TalonID
    import org.team2471.frc.lib.framework.Subsystem


object Armavator : Subsystem("Armavator") {
        //motors
        val suckMotor = MotorController(SparkMaxID(Sparks.INTAKE_SUCK))
        val spitMotor = MotorController(SparkMaxID(Sparks.INTAKE_SPIT))
        val intakePivotMotor = MotorController(TalonID(Talons.INTAKE_PIVOT))
        val armMotor = MotorController(FalconID(Falcons.ARM))
        val elevatorMotor = MotorController(FalconID(Falcons.ELEVATOR))
        //sensors
        val armAngleEncoder = DutyCycleEncoder(DigitalSensors.INTAKE_ARM)
        val elevatorEncoder = DutyCycleEncoder(DigitalSensors.INTAKE_ELEVATOR)
        //data table
        private val table = NetworkTableInstance.getDefault().getTable(Armavator.name)
        val currentEntry = table.getEntry("Current")
        val armAngleEntry = table.getEntry("Arm Angle")


        init {

        }

}