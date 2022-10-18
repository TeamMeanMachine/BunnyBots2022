package org.team2471.bunnybots2022

import org.team2471.frc.lib.framework.Subsystem
import edu.wpi.first.wpilibj.Servo
import org.team2471.frc.lib.actuators.MotorController
import org.team2471.frc.lib.actuators.TalonID


object Bunny : Subsystem("Bunny") {
    //motors
    val pinchMotor = Servo(PWMServos.BUNNY_PINCH)
    val liftMotor = MotorController(TalonID(Talons.BUNNY_LIFT))

    //sensors
//    lift encoder

    init {

    }
}