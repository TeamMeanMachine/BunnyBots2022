package org.team2471.bunnybots2022

import edu.wpi.first.networktables.NetworkTableInstance
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
import org.team2471.frc.lib.input.Controller
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.lib.units.Angle
import org.team2471.frc.lib.units.Length
import org.team2471.frc.lib.units.degrees
import org.team2471.frc.lib.units.inches
import kotlin.math.absoluteValue
import kotlin.math.max


object Armavator : Subsystem("Armavator") {
    //motors
    val suckMotor = MotorController(TalonID(Talons.INTAKE_SUCK))
    val spitMotor = MotorController(TalonID(Talons.INTAKE_SPIT))
    val intakePivotMotor = Servo(PWMServos.INTAKE_PIVOT)
    val armMotor = MotorController(FalconID(Falcons.ARM))
    val elevatorMotor = MotorController(FalconID(Falcons.ELEVATOR))

    //sensors
//    val armAngleEncoder = AnalogInput(AnalogSensors.ARM_ANGLE)
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
    val suckMotorCurrentEntry = table.getEntry("Suck Motor Current")
    val spitMotorCurrentEntry = table.getEntry("Spit Motor Current")

    val ARM_ANGLE_MIN = 7.0.degrees
    val ARM_ANGLE_MAX = 91.0.degrees
    val ELEVATOR_MIN = 1.0.inches
    val ELEVATOR_MAX = 55.0.inches
    val ELEVATOR_START = 21.0.inches
    val ELEVATOR_BREAKING_POINT = 20.0.inches
    val ELEVATOR_TUBE_TIME_MAX = 0.75
    val ARM_TUBE_TIME_MAX = 0.25
    val BIN_DEPTH = 17.0.inches
    val FLOOR_DEPTH = 11.0.degrees

    var tubeTime = 0.0
        set(value) { field = value.coerceIn(0.0, ELEVATOR_TUBE_TIME_MAX) }

    var upPressed = false
    var downPressed = false
    var leftPressed = false
    var rightPressed = false

    val underBinElevatorCurve = MotionCurve()
    val underBinArmCurve = MotionCurve()
    val underBinElevatorCurveB = MotionCurve()
    val underBinArmCurveB = MotionCurve()
    val elevatorTubeCurve = MotionCurve()
    val armTubeCurve = MotionCurve()

    var driverDPadWasPressed = false
    var operatorDPadUpWasPressed = false
    var operatorDPadDownWasPressed = false
    var tongueOffset = 0.0.inches
    val elevatorSwitch = DigitalInput(0)

    val elevatorHeight: Length
        get() = elevatorMotor.position.inches
    var elevatorSetPoint = elevatorHeight
        set(value) {
            field = value.asInches.coerceIn(ELEVATOR_MIN.asInches, ELEVATOR_MAX.asInches).inches
            if (!driverDPadWasPressed){
                elevatorMotor.setPositionSetpoint(field.asInches)
            }
        }

    var armSetPoint = ARM_ANGLE_MIN
        set(value) {
            field = value.asDegrees.coerceIn(ARM_ANGLE_MIN.asDegrees, ARM_ANGLE_MAX.asDegrees).degrees
            if (!driverDPadWasPressed) {
                armMotor.setPositionSetpoint(field.asDegrees)
            }
        }

    val armAngle: Angle
        get() = armMotor.position.degrees
    val thiefAngle: Angle
        get() = intakePivotMotor.position.degrees


    fun resetOffset() {
        armMotor.setRawOffset(ARM_ANGLE_MIN)
        elevatorMotor.setRawOffset(ELEVATOR_START.asInches.degrees)
    }
//    val analogAngle: Angle
//        get() = -(((armAngleEncoder.voltage - 0.2) / 4.6 * 360.0).degrees) + angleOffset

    init {
        println("Armavator says hi!")
        armMotor.config(20) {
            // this was from lil bois bench test of swerve
            feedbackCoefficient =
                (18.0 / 66.0) * (18.0 / 72.0) * (1.0 / 4.0) * (1.0 / 4.0) * (360.0 / 2048.0) // degrees per tick
            setRawOffsetConfig(ARM_ANGLE_MIN) //analogAngle
//            setRawOffsetConfig(ARM_ANGLE_MAX) //analogAngle
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
//            setRawOffsetConfig(ELEVATOR_MAX.asInches.degrees)
            pid {
                p(0.00000003)
                d(0.000002)
            }
        }
        suckMotor.config(20) {
            inverted(true)
        }

        underBinArmCurve.storeValue(0.0, Pose.UNDER_BIN_POSE1.armAngle.asDegrees)
        underBinElevatorCurve.storeValue(0.0, Pose.UNDER_BIN_POSE1.elevatorHeight.asInches)
        underBinArmCurve.storeValue(0.5, Pose.UNDER_BIN_POSE2.armAngle.asDegrees)
        underBinElevatorCurve.storeValue(0.5, Pose.UNDER_BIN_POSE2.elevatorHeight.asInches)
        underBinArmCurve.storeValue(1.0, Pose.UNDER_BIN_POSE3.armAngle.asDegrees)
        underBinElevatorCurve.storeValue(1.0, Pose.UNDER_BIN_POSE3.elevatorHeight.asInches)
        underBinArmCurve.storeValue(1.5, Pose.UNDER_BIN_POSE4.armAngle.asDegrees)
        underBinElevatorCurve.storeValue(2.0, Pose.UNDER_BIN_POSE4.elevatorHeight.asInches)
        underBinArmCurve.storeValue(2.25, Pose.UNDER_BIN_POSE5.armAngle.asDegrees)
        underBinElevatorCurve.storeValue(2.5, Pose.UNDER_BIN_POSE5.elevatorHeight.asInches)

        underBinArmCurveB.storeValue(0.0, Pose.UNDER_BIN_POSE1.armAngle.asDegrees)
        underBinElevatorCurveB.storeValue(0.0, Pose.UNDER_BIN_POSE1.elevatorHeight.asInches)
        underBinArmCurveB.storeValue(0.5, Pose.UNDER_BIN_POSE2.armAngle.asDegrees)
        underBinElevatorCurveB.storeValue(0.5, Pose.UNDER_BIN_POSE2.elevatorHeight.asInches)
        underBinArmCurveB.storeValue(1.0, Pose.UNDER_BIN_POSE3.armAngle.asDegrees)
        underBinElevatorCurveB.storeValue(1.0, Pose.UNDER_BIN_POSE3.elevatorHeight.asInches - 2.0)
        underBinArmCurveB.storeValue(1.5, Pose.UNDER_BIN_POSE4.armAngle.asDegrees)
        underBinElevatorCurveB.storeValue(1.5, Pose.UNDER_BIN_POSE4.elevatorHeight.asInches - 2.0)
        underBinArmCurveB.storeValue(2.25, Pose.UNDER_BIN_POSE5.armAngle.asDegrees)
        underBinElevatorCurveB.storeValue(2.5, Pose.UNDER_BIN_POSE5.elevatorHeight.asInches)

        elevatorTubeCurve.storeValue(0.0, Pose.OVER_BIN_POSE3.elevatorHeight.asInches)
        elevatorTubeCurve.storeValue(ELEVATOR_TUBE_TIME_MAX, Pose.OVER_BIN_POSE3.elevatorHeight.asInches - BIN_DEPTH.asInches)

        armTubeCurve.storeValue(0.0, Pose.UNDER_BIN_POSE5.armAngle.asDegrees)
        armTubeCurve.storeValue(ARM_TUBE_TIME_MAX, Pose.UNDER_BIN_POSE5.armAngle.asDegrees - FLOOR_DEPTH.asDegrees)

        GlobalScope.launch {
            periodic {
                armAngleEntry.setDouble(armAngle.asDegrees)
                armSetPointEntry.setDouble(armSetPoint.asDegrees)
                armCurrentEntry.setDouble(armMotor.current)
                elevatorHeightEntry.setDouble(elevatorHeight.asInches)
                elevatorSetpointEntry.setDouble(elevatorSetPoint.asInches)
                elevatorCurrentEntry.setDouble(elevatorMotor.current)
                elevatorSwitchEntry.setBoolean(elevatorSwitch.get())
                suckMotorCurrentEntry.setDouble(suckMotor.current)
                spitMotorCurrentEntry.setDouble(spitMotor.current)
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
        println("Drive pose!")
        if ((elevatorHeight - Pose.DRIVE_POSE.elevatorHeight).asInches.absoluteValue < 1.0 && (armAngle - Pose.DRIVE_POSE.armAngle).asDegrees.absoluteValue < 2.0 ) {
            println("Already at Drive Pose")
        } else if ((elevatorHeight - Pose.START_POSE.elevatorHeight).asInches.absoluteValue < 1.0 && (armAngle - Pose.START_POSE.armAngle).asDegrees.absoluteValue < 2.0 ) {
            println("At Start Pose")
        }
        else if (elevatorHeight > ELEVATOR_BREAKING_POINT) {
            goToPose(Pose.OVER_BIN_POSE2)
            goToPose(Pose.OVER_BIN_POSE1)
        }
        else if (elevatorHeight < Pose.DRIVE_POSE.elevatorHeight && armAngle > ARM_ANGLE_MAX - 7.0.degrees) {
            underBinToDrivePose()
        }
        goToPose(Pose.DRIVE_POSE)
    }

    suspend fun goToOverBinPose() = use(Armavator) {
        if (isPose(Pose.UNDER_BIN_POSE5, 2.0, 12.0)) {
            underBinToDrivePose()
        } else if (isPose(Pose.GROUND_POSE2)) {
            goToDrivePose()
        }

        if (isPose(Pose.OVER_BIN_POSE3, 17.0)) {
            println("Already Over Bin Pose")
        } else {
            goToPose(Pose.OVER_BIN_POSE1)
            goToPose(Pose.OVER_BIN_POSE2)
            goToPose(Pose(Pose.OVER_BIN_POSE3.elevatorHeight + tongueOffset, Pose.OVER_BIN_POSE3.armAngle))
        }
    }

    suspend fun goToGroundPose() = use(Armavator) {
        if (isPose(Pose.OVER_BIN_POSE3, 17.0) || isPose(Pose.START_POSE)) goToDrivePose()
        if (!isPose(Pose.GROUND_POSE2)) goToPose(Pose.GROUND_POSE1)
        goToPose(Pose(Pose.GROUND_POSE2.elevatorHeight + tongueOffset, Pose.GROUND_POSE2.armAngle))
    }

    suspend fun goToStartPose() = use(Armavator) {
        if ((elevatorHeight - Pose.START_POSE.elevatorHeight).asInches.absoluteValue < 1.0 && (armAngle - Pose.START_POSE.armAngle).asDegrees.absoluteValue < 2.0 ) {
            println("Already at Start Pose")
        } else { goToDrivePose() }
        goToPose(Pose.START_POSE)
    }

    suspend fun goToUnderBinPose() = use(Armavator) {
        if (isPose(Pose.OVER_BIN_POSE3, 16.0, 2.0) || isPose(Pose.GROUND_POSE2)) {
            goToDrivePose()
        }
        if (isPose(Pose.DRIVE_POSE) || isPose(Pose.START_POSE)) {
            underBinElevatorCurve.storeValue(0.0, elevatorHeight.asInches)
            underBinArmCurve.storeValue(0.0, armAngle.asDegrees)
            underBinElevatorCurve.storeValue(underBinElevatorCurve.length, Pose.UNDER_BIN_POSE5.elevatorHeight.asInches + tongueOffset.asInches)
            val timer = Timer()
            timer.start()
            periodic {
                val t = timer.get()
                elevatorSetPoint = underBinElevatorCurve.getValue(t).inches
                armSetPoint = underBinArmCurve.getValue(t).degrees

                if (t >= max(underBinArmCurve.length, underBinElevatorCurve.length)) {
                    stop()
                }
            }
        }
    }

    suspend fun underBinToDrivePose() = use(Armavator) {
        underBinElevatorCurveB.storeValue(0.0, Pose.DRIVE_POSE.elevatorHeight.asInches)
        underBinArmCurveB.storeValue(0.0, Pose.DRIVE_POSE.armAngle.asDegrees)
        val maxTime = max(underBinArmCurveB.length, underBinElevatorCurveB.length)
        val timer = Timer()
        timer.start()
        periodic {
            val t = maxTime - timer.get()
            elevatorSetPoint = underBinElevatorCurveB.getValue(t).inches
            armSetPoint = underBinArmCurveB.getValue(t).degrees

            if (t <= 0.0) {
                stop()
            }
        }
    }

    fun isPose(pose: Pose, eDiff: Double = 2.0, aDiff: Double = 2.0) : Boolean {
        return (elevatorHeight - pose.elevatorHeight).asInches.absoluteValue < eDiff && (armAngle - pose.armAngle).asDegrees.absoluteValue < aDiff
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
            if (elevatorHeight > Pose.OVER_BIN_POSE3.elevatorHeight - BIN_DEPTH - 1.0.inches && elevatorHeight < Pose.OVER_BIN_POSE3.elevatorHeight + 1.0.inches
                && armAngle > 80.0.degrees) {
                tubeTime += OI.operatorLeftY * 0.02
                elevatorSetPoint = elevatorTubeCurve.getValue(tubeTime).inches
            } else if (elevatorHeight < Pose.UNDER_BIN_POSE5.elevatorHeight + 1.0.inches && armAngle > Pose.UNDER_BIN_POSE5.armAngle - FLOOR_DEPTH - 5.0.degrees) {
                tubeTime += OI.operatorLeftY * 0.02
                armSetPoint = armTubeCurve.getValue(tubeTime).degrees
            }
            else {
                tubeTime = 0.0
            }

            intakePivotMotor.set(-OI.operatorRightX / 2.0 + 0.5)

            spitMotor.setPercentOutput(OI.operatorRightTrigger - OI.operatorLeftTrigger)
            suckMotor.setPercentOutput(if (OI.operatorController.rightBumper) 1.0 else if (OI.operatorController.leftBumper) -1.0 else 0.0)

            when (OI.driverController.dPad) {
                Controller.Direction.RIGHT -> {
                    armMotor.setPercentOutput(1.0)
                    driverDPadWasPressed = true
                }
                Controller.Direction.LEFT -> {
                    armMotor.setPercentOutput(-1.0)
                    driverDPadWasPressed = true
                }
                Controller.Direction.UP -> {
                    elevatorMotor.setPercentOutput(0.1)
                    driverDPadWasPressed = true
                }
                Controller.Direction.DOWN -> {
                    elevatorMotor.setPercentOutput(-0.1)
                    driverDPadWasPressed = true
                }
                Controller.Direction.IDLE -> {
                    if (driverDPadWasPressed) {
                        armMotor.setPercentOutput(0.0)
                        elevatorMotor.setPercentOutput(0.0)
                        driverDPadWasPressed = false
                    }
                }
            }
            when (OI.operatorController.dPad) {
                Controller.Direction.UP -> {
                    operatorDPadUpWasPressed = true
                }
                Controller.Direction.DOWN -> {
                    operatorDPadDownWasPressed = true
                }
                Controller.Direction.IDLE -> {
                    if (operatorDPadUpWasPressed) {
                        tongueOffset += 0.125.inches
                        elevatorSetPoint += 0.125.inches
                        operatorDPadUpWasPressed = false
                    } else if (operatorDPadDownWasPressed) {
                        tongueOffset -= 0.125.inches
                        elevatorSetPoint -= 0.125.inches
                        operatorDPadDownWasPressed = false
                    }
                }
            }
            //        ({operatorController.dPad == Controller.Direction.RIGHT}).whenTrue {
//            Armavator.goToDrivePose()
//        }
//        ({operatorController.dPad == Controller.Direction.UP}).whenTrue {
//            Armavator.goToOverBinPose()
//        }
//        ({operatorController.dPad == Controller.Direction.LEFT}).whenTrue {
//            Armavator.goToGroundPose()
//        }
//        ({operatorController.dPad == Controller.Direction.DOWN}).whenTrue {
//            Armavator.goToUnderBinPose()
//        }
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