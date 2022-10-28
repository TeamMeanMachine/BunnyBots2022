package org.team2471.bunnybots2022

import edu.wpi.first.wpilibj.Servo
import org.team2471.frc.lib.framework.Subsystem

object DepthCharge : Subsystem("DepthCharge") {
    //motors
    val firstMotor = Servo(PWMServos.DEPTH_ONE)
    val secondMotor = Servo(PWMServos.DEPTH_TWO)

    init {

    }
}