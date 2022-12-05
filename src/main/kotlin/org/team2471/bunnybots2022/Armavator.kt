package org.team2471.bunnybots2022

import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.DigitalInput
import edu.wpi.first.wpilibj.Servo
import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import org.team2471.frc.lib.actuators.FalconID
import org.team2471.frc.lib.actuators.MotorController
import org.team2471.frc.lib.actuators.TalonID
import org.team2471.frc.lib.coroutines.parallel
import org.team2471.frc.lib.coroutines.periodic
import org.team2471.frc.lib.framework.Subsystem
import org.team2471.frc.lib.framework.use
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.lib.units.Angle
import org.team2471.frc.lib.units.Length
import org.team2471.frc.lib.units.degrees
import org.team2471.frc.lib.units.inches
import kotlin.math.absoluteValue


object Armavator : Subsystem("Armavator") {
    //motors
    val suckMotor = MotorController(TalonID(Talons.INTAKE_SUCK))
    val spitMotor = MotorController(TalonID(Talons.INTAKE_SPIT))
    val intakePivotMotor = Servo(PWMServos.INTAKE_PIVOT)
    val armMotor = MotorController(FalconID(Falcons.ARM))
    val elevatorMotor = MotorController(FalconID(Falcons.ELEVATOR))

    //sensors
    val armAngleEncoder = AnalogInput(AnalogSensors.ARM_ANGLE)
//    val elevatorEncoder = DutyCycleEncoder(DigitalSensors.INTAKE_ELEVATOR)

    //data table
    private val table = NetworkTableInstance.getDefault().getTable(Armavator.name)
    val armCurrentEntry = table.getEntry("Current")
    val armAngleEntry = table.getEntry("Arm Angle")
    val armSetPointEntry = table.getEntry("Arm Set Point")
    val angleOffset = 96.0.degrees
    val elevatorHeightEntry = table.getEntry("Elevator Height")
    val elevatorSetpointEntry = table.getEntry("Elevator Set Point")
    val elevatorCurrentEntry = table.getEntry("Elevator Current")
    val elevatorSwitchEntry = table.getEntry("Elevator Switch")

    val ARM_ANGLE_MIN = 7.0.degrees
    val ARM_ANGLE_MAX = 91.0.degrees
    val ELEVATOR_MIN = 0.0.inches
    val ELEVATOR_MAX = 55.0.inches
    val ELEVATOR_START = 21.0.inches

    var upPressed = false
    var downPressed = false
    var leftPressed = false
    var rightPressed = false

    val elevatorSwitch = DigitalInput(0)

    val elevatorHeight: Length
        get() = elevatorMotor.position.inches
    var elevatorSetPoint = elevatorHeight
        set(value) {
            field = value.asInches.coerceIn(ELEVATOR_MIN.asInches, ELEVATOR_MAX.asInches).inches
            elevatorMotor.setPositionSetpoint(field.asInches)
        }

    var armSetPoint = analogAngle
        set(value) {
            field = value.asDegrees.coerceIn(ARM_ANGLE_MIN.asDegrees, ARM_ANGLE_MAX.asDegrees).degrees
            armMotor.setPositionSetpoint(field.asDegrees)
        }

    val armAngle: Angle
        get() = armMotor.position.degrees
    val thiefAngle: Angle
        get() = intakePivotMotor.position.degrees

    val analogAngle: Angle
        get() = -(((armAngleEncoder.voltage - 0.2) / 4.6 * 360.0).degrees) + angleOffset

    init {
        println("Armavator says hi!")
        armMotor.config(20) {
            // this was from lil bois bench test of swerve
            feedbackCoefficient =
                (18.0 / 66.0) * (18.0 / 72.0) * (1.0 / 4.0) * (1.0 / 4.0) * (360.0 / 2048.0) // degrees per tick
            setRawOffsetConfig(ARM_ANGLE_MIN) //analogAngle
            currentLimit(15, 20, 1)
            pid {
                p(0.0000001)
                d(0.00002)
            }
////                burnSettings()
        }
        elevatorMotor.config(20) {
            inverted(true)
            brakeMode()
            currentLimit(25, 30, 1)
            feedbackCoefficient = 12.0 / 28504 //57609.0  // inche per tick
            setRawOffsetConfig(ELEVATOR_START.asInches.degrees) // todo: really inches - this needs changed in meanlib to take a Double
            pid {
                p(0.00000003)
                d(0.000002)
            }
        }
        GlobalScope.launch {
            periodic {
                armAngleEntry.setDouble(armAngle.asDegrees)
                armSetPointEntry.setDouble(armSetPoint.asDegrees)
                armCurrentEntry.setDouble(armMotor.current)
                elevatorHeightEntry.setDouble(elevatorHeight.asInches)
                elevatorSetpointEntry.setDouble(elevatorSetPoint.asInches)
                elevatorCurrentEntry.setDouble(elevatorMotor.current)
                elevatorSwitchEntry.setBoolean(elevatorSwitch.get())
            }
        }
    }
    fun angleChangeTime(target: Double) : Double {
        val distance = (armAngle.asDegrees - target).absoluteValue
        val rate = 45.4242 / 1.0 // degrees per sec
        return distance / rate
    }
    fun heightChangeTime(target: Double) : Double {
        val distance = (elevatorHeight.asInches - target)
        val rate = 18.0  //inches per second
        return distance.absoluteValue / rate
    }
    suspend fun changeAngle(target: Double, minTime: Double = 0.0) {
        var time = angleChangeTime(target)
        if (minTime > time) {
            println("Time extended for changeAngle using minTime: $minTime")
            time = minTime
        }
        changePosition(armAngle.asDegrees, target, time) { value: Double ->
            armSetPoint = value.degrees
        }
    }

    suspend fun changeHeight(target: Double, minTime: Double = 0.0) {
        var time = heightChangeTime(target)
        if (minTime > time) {
            println("Time extended for changeHeight using minTime: $minTime")
            time = minTime
        }
        changePosition(elevatorHeight.asInches, target, time) { value: Double ->
            elevatorSetPoint = value.inches
        }
    }
    suspend fun changePosition(current: Double, target: Double, time : Double, function: (value : Double) -> (Unit)) {
        val curve = MotionCurve()
        curve.storeValue(0.0, current)
        curve.storeValue(time, target)
        val timer = Timer()
        timer.start()
        periodic {
            val t = timer.get()
            // println("ARM TIME: $t ${armAngle.asDegrees}   ${curve.getValue(t)}") //For printing arm stuff
            function(curve.getValue(t))
            if (t >= curve.length) {
                stop()
            }
        }
    }

    suspend fun goToPose(targetPose: Pose, fullCurve: Boolean = true, minTime: Double = 0.0) = use(Armavator) {
        val time = if (fullCurve) {
            maxOf(minTime, angleChangeTime(targetPose.armAngle.asDegrees), heightChangeTime(targetPose.elevatorHeight.asInches))
        } else {
            minTime
        }
        println("Pose Values: $time ${targetPose.elevatorHeight} ${targetPose.armAngle}")
        parallel({
            changeHeight(targetPose.elevatorHeight.asInches, time)
        }, {
            changeAngle(targetPose.armAngle.asDegrees, time)
        })
    }

    suspend fun goToDrivePose() = use(Armavator) {
        if (elevatorHeight > 39.0.inches) {
            goToPose(Pose.OVER_BIN_POSE2)
            goToPose(Pose.OVER_BIN_POSE1)
        }
        goToPose(Pose.DRIVE_POSE)
    }
    suspend fun goToOverBinPose() = use(Armavator) {
        goToPose(Pose.OVER_BIN_POSE1)
        goToPose(Pose.OVER_BIN_POSE2)
        goToPose(Pose.OVER_BIN_POSE3)
    }
    suspend fun goToGroundPose() = use(Armavator) {
        if (elevatorHeight > 39.0.inches) {
            goToPose(Pose.OVER_BIN_POSE2)
            goToPose(Pose.OVER_BIN_POSE1)
        }
        goToPose(Pose.GROUND_POSE1)
        goToPose(Pose.GROUND_POSE2)
    }
    suspend fun goToStartPose() = use(Armavator) {
        if (elevatorHeight > 39.0.inches) {
            goToPose(Pose.OVER_BIN_POSE2)
            goToPose(Pose.OVER_BIN_POSE1)
        }
        goToPose(Pose.START_POSE)
    }

    override fun preEnable() {
        println("Armavator preEnable")
        super.preEnable()
        elevatorSetPoint = elevatorHeight
        armSetPoint = armAngle
    }

    override suspend fun default(){
        println("starting periodic")
        periodic {
            if (elevatorHeight>Pose.OVER_BIN_POSE3.elevatorHeight-18.0.inches && elevatorHeight<Pose.OVER_BIN_POSE3.elevatorHeight+1.0.inches &&
                    OI.operatorLeftY >= 0.0) {
                elevatorSetPoint = Pose.OVER_BIN_POSE3.elevatorHeight - 15.0.inches * OI.operatorLeftY
            }

            intakePivotMotor.set(-OI.operatorRightX / 2.0 + 0.5)

            spitMotor.setPercentOutput(OI.operatorRightTrigger - OI.operatorLeftTrigger)
            suckMotor.setPercentOutput(if (OI.operatorController.rightBumper) 1.0 else if (OI.operatorController.leftBumper) -1.0 else 0.0)
        }
        println("ending periodic")
    }

    fun intakeTube() {
        suckMotor.setPercentOutput(0.5)
        println("intake tube")
    }

    fun spitTube(power: Double) {
        spitMotor.setPercentOutput(power)
        println("spit tube")
    }
}