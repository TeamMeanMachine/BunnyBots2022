package org.team2471.bunnybots2022

import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.AnalogInput
import edu.wpi.first.wpilibj.DutyCycleEncoder
import edu.wpi.first.wpilibj.Servo
import edu.wpi.first.wpilibj.Timer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import org.team2471.frc.lib.actuators.FalconID
import org.team2471.frc.lib.actuators.MotorController
import org.team2471.frc.lib.actuators.SparkMaxID
import org.team2471.frc.lib.actuators.TalonID
import org.team2471.frc.lib.coroutines.periodic
import org.team2471.frc.lib.framework.Subsystem
import org.team2471.frc.lib.input.Controller
import org.team2471.frc.lib.motion_profiling.MotionCurve
import org.team2471.frc.lib.units.Angle
import org.team2471.frc.lib.units.Length
import org.team2471.frc.lib.units.degrees
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


object Armavator : Subsystem("Armavator") {
    //motors
    val suckMotor = MotorController(SparkMaxID(Sparks.INTAKE_SUCK))
    val spitMotor = MotorController(SparkMaxID(Sparks.INTAKE_SPIT))
    val intakePivotMotor = Servo(PWMServos.INTAKE_PIVOT)
    val armMotor = MotorController(FalconID(Falcons.ARM))
    val elevatorMotor = MotorController(FalconID(Falcons.ELEVATOR))

    //sensors
    val armAngleEncoder = AnalogInput(AnalogSensors.ARM_ANGLE)
    //val elevatorEncoder = DutyCycleEncoder(DigitalSensors.INTAKE_ELEVATOR)

    //data table
    private val table = NetworkTableInstance.getDefault().getTable(Armavator.name)
    val armCurrentEntry = table.getEntry("Current")
    val armAngleEntry = table.getEntry("Arm Angle")
    val armSetPointEntry = table.getEntry("Arm Set Point")
    val angleOffset = 230.0.degrees

    const val ARM_ANGLE_MIN = 7.0
    const val ARM_ANGLE_MAX = 95.0
    const val ELEVATOR_MIN = 0.0
    const val ELEVATOR_MAX = 0.0
    const val ELEVATOR_START = 0.0





    var upPressed = false
    var downPressed = false

    var armSetPoint = analogAngle
        set(value) {
            field = value.asDegrees.coerceIn(ARM_ANGLE_MIN, ARM_ANGLE_MAX).degrees
            armMotor.setPositionSetpoint(field.asDegrees)
        }

    val armAngle: Angle
        get() = armMotor.position.degrees
    val thiefAngle: Angle
        get() = intakePivotMotor.position.degrees
    val elevatorHeight: Double
        get() = elevatorMotor.position

    val analogAngle: Angle
        get() = -(((armAngleEncoder.voltage - 0.2) / 4.6 * 360.0).degrees) + angleOffset

    init {
        armMotor.config(20) {
            // this was from lil bois bench test of swerve
            feedbackCoefficient =
                (18.0 / 66.0) * (18.0 / 72.0) * (1.0 / 4.0) * (1.0 / 4.0) * (360.0 / 2048.0) // degrees per tick
            setRawOffsetConfig(analogAngle)
            currentLimit(15, 20, 1)
            pid {
                p(0.0000001)
                d(0.00002)
            }
////                burnSettings()
        }
        GlobalScope.launch {
            periodic {
                armAngleEntry.setDouble(armAngle.asDegrees)
                armSetPointEntry.setDouble(armSetPoint.asDegrees)
                armCurrentEntry.setDouble(armMotor.current)
            }
        }
    }
    fun angleChangeTime(target: Double) : Double {
        val distance = (armAngle.asDegrees - target).absoluteValue
        val rate = 45.4242 / 1.0 // degrees per sec
        return distance / rate
    }
    fun heightChangeTime(target: Double) : Double {
//        val distance = (height - target)
//        val rate = if (distance < 0.0) 40.0 else 20.0  // inches per sec
//        return distance.absoluteValue / rate
        return 1.0
    }
    suspend fun changeAngle(target: Double, minTime: Double = 0.0) {
        var time = angleChangeTime(target)
        if (minTime > time) {
            println("Time extended for changeAngle using minTime: $minTime")
            time = minTime
        }
        changePosition(armAngle.asDegrees, target, time, { value: Double ->
            armSetPoint = value.degrees
//            updatePositions()
        })
    }

    suspend fun changeHeight(target: Double, minTime: Double = 0.0) {
        var time = heightChangeTime(target)
        if (minTime > time) {
            println("Time extended for changeHeight using minTime: $minTime")
            time = minTime
        }
//        changePosition(height, target, time, { value: Double ->
//            heightSetpoint = value
//            updatePositions()
//        })
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

    override suspend fun default(){
        println("starting periodic")
        periodic {
            //println("Motor Angle: ${armMotor.position.roundToInt() - analogAngle.asDegrees.roundToInt()}  Set Point: ${armSetPoint} Power: ${(armMotor.output * 100).roundToInt()}")

            if (OI.driverController.dPad == Controller.Direction.UP) {
                upPressed = true
            } else if (OI.driverController.dPad == Controller.Direction.DOWN) {
                downPressed = true
            }

            if(OI.driverController.dPad != Controller.Direction.UP && upPressed) {
                upPressed = false
                armSetPoint += 45.0.degrees
                println("dpad up")
            }

            if(OI.driverController.dPad != Controller.Direction.DOWN && downPressed) {
                downPressed = false
                armSetPoint -= 45.0.degrees
                println("dpad down")
            }

  //          println("current: ${armMotor.current}")
        }
        println("ending periodic")
    }

}