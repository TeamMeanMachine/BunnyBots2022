package org.team2471.bunnybots2022
import org.team2471.frc.lib.coroutines.parallel
import org.team2471.frc.lib.framework.use

suspend fun scoreCharge() = use(DepthCharge) {
    DepthCharge.score(false)
}
