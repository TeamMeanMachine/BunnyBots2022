class Intake {
    package org.team2471.frc2022

    import edu.wpi.first.networktables.NetworkTableInstance
    import edu.wpi.first.wpilibj.DutyCycleEncoder

    import kotlinx.coroutines.GlobalScope
    import kotlinx.coroutines.launch
    import org.team2471.frc.lib.actuators.FalconID
    import org.team2471.frc.lib.actuators.MotorController
    import org.team2471.frc.lib.actuators.TalonID
    import org.team2471.frc.lib.control.PDController
    import org.team2471.frc.lib.coroutines.MeanlibDispatcher
    import org.team2471.frc.lib.coroutines.parallel
    import org.team2471.frc.lib.coroutines.periodic
    import org.team2471.frc.lib.framework.Subsystem
    import org.team2471.frc.lib.framework.use
    import org.team2471.frc.lib.input.Controller
    import org.team2471.frc.lib.input.whenTrue
    import org.team2471.frc.lib.motion_profiling.MotionCurve
    import org.team2471.frc.lib.units.degrees
    import kotlin.math.absoluteValue
    import edu.wpi.first.wpilibj.Timer as Timer


    object Intake : Subsystem("Intake") {

        val intakeMotor = MotorController(TalonID(Talons.INTAKE))
        val intakePivotMotor = MotorController(FalconID(Falcons.INTAKE_PIVOT))

        private val table = NetworkTableInstance.getDefault().getTable(Intake.name)
        val currentEntry = table.getEntry("Current")
        val pivotEntry = table.getEntry("Pivot")
        val pivotSetpointEntry = table.getEntry("Pivot Setpoint")
        val pivotMotorEntry = table.getEntry("Pivot Motor")
        val pivotDriverOffsetEntry = table.getEntry("Pivot Controller")
        val intakeStateEntry = table.getEntry("Mode")
        val intakePresetEntry = table.getEntry("Intake Preset")
}