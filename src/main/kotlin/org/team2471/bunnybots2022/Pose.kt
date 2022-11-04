package org.team2471.bunnybots2022

data class Pose(val elevatorHeight: Double, val armAngle: Double) {

    companion object {
        val current: Pose
            get() = Pose(Armavator.elevatorHeight, Armavator.armAngle.asDegrees)

        val START_POS = Pose(Armavator.ELEVATOR_START, Armavator.ARM_ANGLE_MIN)

    }
}