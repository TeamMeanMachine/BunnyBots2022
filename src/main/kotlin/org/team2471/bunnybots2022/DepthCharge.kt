package org.team2471.bunnybots2022

import edu.wpi.first.wpilibj.Servo
import edu.wpi.first.wpilibj.Timer
import org.team2471.frc.lib.coroutines.delay
import org.team2471.frc.lib.coroutines.periodic
import org.team2471.frc.lib.framework.Subsystem

object DepthCharge : Subsystem("DepthCharge") {
    //motors
    val firstMotor = Servo(PWMServos.DEPTH_ONE)
    val secondMotor = Servo(PWMServos.DEPTH_TWO)

    init {

    }

    override suspend fun default() {
        println("starting periodic")
        periodic {
            firstMotor.set(if (OI.driverController.leftBumper) 1.0 else 0.5)
            secondMotor.set(if (OI.driverController.rightBumper) 0.0 else 0.5)
        }
    }

    suspend fun score(rightMotor: Boolean) {
        val timer = Timer()
        timer.start()
        periodic {
            if(rightMotor) {
                secondMotor.set(0.0)
            } else {
                firstMotor.set(1.0)
            }
            println("timer=${timer.get()}")
            if (timer.get() > 2.0) {
                stop()
            }
        }
//        firstMotor.set(0.5)
//        secondMotor.set(0.5)
    }
}