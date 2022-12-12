package org.team2471.bunnybots2022

import edu.wpi.first.wpilibj.AnalogInput
import org.team2471.frc.lib.framework.Subsystem
import edu.wpi.first.wpilibj.Servo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.team2471.frc.lib.actuators.MotorController
import org.team2471.frc.lib.actuators.SparkMaxID
import org.team2471.frc.lib.actuators.TalonID
import org.team2471.frc.lib.coroutines.periodic
import org.team2471.frc.lib.units.Angle
import org.team2471.frc.lib.units.degrees


object Bunny : Subsystem("Bunny") {
    val leftPinchMotor = Servo(PWMServos.BUNNY_PINCH_LEFT)
    val rightPinchMotor = Servo(PWMServos.BUNNY_PINCH_RIGHT)
    val bunnyExtend = MotorController(SparkMaxID(Sparks.BUNNY_EXTEND))
    val bunnyExtendSensor = AnalogInput(AnalogSensors.BUNNY_EXTEND)

    const val EXTEND_ANGLE_MIN = 35.0
    const val EXTEND_ANGLE_MAX = 121.0 //recheck
    const val EXTEND_ANGLE_OUT = 50.0
    const val EXTEND_ANGLE_IN = 90.0

    val angleOffset = -11.0.degrees
    val analogAngle: Angle
        get() = (bunnyExtendSensor.voltage / 0.71 * 51).degrees + angleOffset //((bunnyExtendSensor.voltage.degrees - 1.43.degrees) / 0.72 * 47.2) - angleOffset
    var extendSetPoint = analogAngle //
        set(value) {
            field = value.asDegrees.coerceIn(EXTEND_ANGLE_MIN, EXTEND_ANGLE_MAX).degrees
            bunnyExtend.setPositionSetpoint(field.asDegrees)
        }

    init {
        bunnyExtend.config(20) {
            feedbackCoefficient =
                (1.0 / 20.0) * (24.0 / 68.0) * (360.0 / 42.0) // degrees per tick
            setRawOffsetConfig(analogAngle)
            inverted(false)
            currentLimit(15, 20, 1)
            pid {
                p(0.000006) //0.000012 but belts skip
                d(0.00000)  //0.000005
            }
        }
        GlobalScope.launch {

            periodic(0.1) {
                bunnyExtend.setRawOffset(analogAngle)
            }
        }
    }
    fun pinch() {
        leftPinchMotor.set(0.5)
        rightPinchMotor.set(0.0)
    }

    fun unpinch() {
        leftPinchMotor.set(1.0)
        rightPinchMotor.set(0.5)
    }

    override suspend fun default() {
        println("starting periodic")
        periodic {
            leftPinchMotor.set(if (OI.driverController.b) 0.5 else 1.0)
            rightPinchMotor.set(if (OI.driverController.b) 0.0 else 0.5)
//            bunnyExtend.setPercentOutput(OI.operatorRightX * 0.3)
            extendSetPoint -= (OI.driveRightTrigger * 5.0).degrees
            extendSetPoint += (OI.driveLeftTrigger * 5.0).degrees




            //debug//println(leftPinchMotor.get())
//            println("Bunny Angle: ${analogAngle}")

        }
    }
}