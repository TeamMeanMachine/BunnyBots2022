package org.team2471.bunnybots2022

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.team2471.frc.lib.actuators.MotorController
import org.team2471.frc.lib.actuators.TalonID
import org.team2471.frc.lib.coroutines.periodic
import org.team2471.frc.lib.framework.Subsystem

// this is a test of the little 550 rev motor and its encoder powered by a talon srx
//object TestSystem: Subsystem("TestSystem") {
//    val motor = MotorController(TalonID(7))
//
//    init {
//        motor.config {
//            feedbackCoefficient = 360.0 / 5.0 / 4.0 / 3.0 / 25.67
//            pid {
//                p(0.0005)
//            }
//
//        }
//        GlobalScope.launch {
//            periodic {
//                if (OI.driverController.x) {
//                    motor.setPercentOutput(-0.4)
//                }
//                else if(OI.driverController.b) {
//                    motor.setPercentOutput(0.4)
//                }
//                else {
//                    motor.setPercentOutput(0.0)
//                    motor.setPositionSetpoint(OI.driveRightTrigger * 90.0)
//                }
//                println("test motor pos:  ${motor.position}")
//            }
//        }
//    }
//}