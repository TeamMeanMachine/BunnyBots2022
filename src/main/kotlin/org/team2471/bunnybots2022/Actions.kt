package org.team2471.bunnybots2022
import org.team2471.frc.lib.coroutines.parallel
import org.team2471.frc.lib.framework.use

//suspend fun goToPose(targetPose: Pose, fullCurve: Boolean = false, minTime: Double = 0.0) = use(Armavator) {
//    val time = if (fullCurve) {
//        maxOf(minTime, Armavator.angleChangeTime(targetPose.armAngle), Armavator.heightChangeTime(targetPose.elevatorHeight))
//    } else {
//        minTime
//    }
//    println("Pose Values: $time ${targetPose.elevatorHeight} ${targetPose.armAngle}")
//    parallel({
//        Armavator.changeHeight(targetPose.elevatorHeight, time)
//    }, {
//        Armavator.changeAngle(targetPose.armAngle, time)
//    })
//}

