@file:Suppress("unused")

package org.team2471.bunnybots2022

object Talons {
    const val INTAKE_SPIT = 10
    const val INTAKE_SUCK = 8

}


object Sparks {
    const val BUNNY_EXTEND = 10

}

object AnalogSensors {
    const val BUNNY_EXTEND = 0
//    const val ARM_ANGLE = 0
}

object DigitalSensors {
    const val INTAKE_ARM = 25
    const val INTAKE_ELEVATOR = 99
}

object PWMServos {
    const val BUNNY_PINCH_LEFT = 3
    const val BUNNY_PINCH_RIGHT = 4
    const val DEPTH_ONE = 1
    const val DEPTH_TWO = 2
    const val INTAKE_PIVOT = 0
}

object Falcons {

    const val DRIVE_FRONTLEFT = 12
    const val STEER_FRONTLEFT = 13
    const val DRIVE_FRONTRIGHT = 2
    const val STEER_FRONTRIGHT = 3
    const val DRIVE_REARRIGHT = 0
    const val STEER_REARRIGHT = 1
    const val DRIVE_REARLEFT = 14
    const val STEER_REARLEFT = 9

    const val ARM = 6

    const val ELEVATOR = 15

}

object CANCoders {
    const val CANCODER_FRONTLEFT = 20
    const val CANCODER_FRONTRIGHT = 21
    const val CANCODER_REARRIGHT = 22
    const val CANCODER_REARLEFT = 23
}
