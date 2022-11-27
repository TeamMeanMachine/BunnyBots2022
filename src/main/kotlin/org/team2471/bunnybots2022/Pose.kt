package org.team2471.bunnybots2022

import org.team2471.frc.lib.units.Angle
import org.team2471.frc.lib.units.Length
import org.team2471.frc.lib.units.inches

data class Pose(val elevatorHeight: Length, val armAngle: Angle) {

    companion object {
        val current: Pose
            get() = Pose(Armavator.elevatorHeight, Armavator.armAngle)

        val N_Pos = Pose(Armavator.ELEVATOR_START, Armavator.ARM_ANGLE_MAX)
        val START_POS = Pose(Armavator.ELEVATOR_START, Armavator.ARM_ANGLE_MIN)

    }
}