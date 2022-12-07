package org.team2471.bunnybots2022

import org.team2471.frc.lib.units.Angle
import org.team2471.frc.lib.units.Length
import org.team2471.frc.lib.units.degrees
import org.team2471.frc.lib.units.inches

data class Pose(val elevatorHeight: Length, val armAngle: Angle) {

    companion object {
        val current: Pose
            get() = Pose(Armavator.elevatorHeight, Armavator.armAngle)

        val N_Pos = Pose(Armavator.ELEVATOR_START, Armavator.ARM_ANGLE_MAX)
        val START_POSE = Pose(Armavator.ELEVATOR_START, Armavator.ARM_ANGLE_MIN)
        val DRIVE_POSE = Pose(Armavator.ELEVATOR_START + 6.inches, Armavator.ARM_ANGLE_MIN)
        val OVER_BIN_POSE1 = Pose(Armavator.ELEVATOR_MAX, Armavator.ARM_ANGLE_MIN) //animaton brings arm to min then elevator to max then arm to pose then elevator to pose
        val OVER_BIN_POSE2 = Pose(Armavator.ELEVATOR_MAX, 90.degrees)
        val OVER_BIN_POSE3 = Pose(41.inches, 90.degrees)

        val GROUND_POSE1 = Pose(Armavator.ELEVATOR_START, 44.degrees) //kind of tight--have drivers be able to adjust for error
        val GROUND_POSE2 = Pose(12.5.inches, 44.degrees)
        val UNDER_BIN_POSE1 = Pose(Armavator.elevatorSetPoint, 90.degrees)
        val UNDER_BIN_POSE2 = Pose(Armavator.ELEVATOR_MIN, 90.degrees)

    }
}